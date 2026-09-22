# DhanyaMart - Handicrafts E-commerce Web Application

Java + JSP + Servlets + **MySQL 8 (JDBC)** e-commerce capstone project.
The full flow is implemented: **Register -> Login -> Product catalogue -> Search/Filter ->
Product details + reviews -> Cart -> Checkout -> Orders, plus Seller and Admin dashboards**.

All data is stored in a **MySQL 8 database** named `dhanyamart`. The DAO layer uses
plain JDBC with **prepared statements** (no Hibernate/Spring). Passwords are never stored
in plain text - each one is salted and hashed with PBKDF2.

---

## 1. Project structure

```
Dhanya capstone/
├── pom.xml                                  (Maven build + MySQL Connector/J dependency)
├── src/main/resources/
│   ├── db.properties                        MySQL connection settings (edit user/password)
│   └── sql/dhanyamart_schema.sql            authoritative MySQL 8 schema (run by hand or auto)
├── src/main/java/com/dhanyamart/
│   ├── controller/
│   │   ├── LoginServlet.java                 /login
│   │   ├── RegisterServlet.java              /register
│   │   ├── LogoutServlet.java                /logout
│   │   ├── ProductsServlet.java              /products          (browse + search/filter)
│   │   ├── ProductDetailsServlet.java        /product?id=N      (details + reviews)
│   │   ├── ReviewServlet.java                /review            (POST, add/delete rating)
│   │   ├── CartServlet.java                  /cart              (session cart)
│   │   ├── CheckoutServlet.java              /checkout          (place order)
│   │   ├── OrderConfirmationServlet.java     /order-confirm?id=N
│   │   ├── OrdersServlet.java                /orders            (my orders)
│   │   ├── SellerProductsServlet.java        /seller/products   (seller dashboard)
│   │   ├── SellerProductServlet.java         /seller/product    (add/edit/delete)
│   │   ├── AdminServlet.java                 /admin             (overview/users/products/orders)
│   │   ├── SellerAccess.java                 shared seller/admin role checks
│   │   └── DataSeeder.java                   seeds demo accounts, products, reviews on first start
│   ├── dao/UserDAO.java                      users table logic  (JDBC)
│   ├── dao/ProductDAO.java                   products table logic (JDBC)
│   ├── dao/OrderDAO.java                     orders + order_items logic (JDBC)
│   ├── dao/ReviewDAO.java                    reviews table logic (JDBC)
│   ├── model/                                User, Product, CartItem, Order, OrderItem, Review
│   └── util/
│       ├── DBConnection.java                 reusable JDBC connection factory + auto-schema
│       ├── CellUtil.java                     parameter parsing helpers
│       └── PasswordUtil.java                 PBKDF2 hash + verify
└── src/main/webapp/
    ├── WEB-INF/web.xml
    ├── login.jsp / register.jsp / home.jsp
    ├── products.jsp / product.jsp / cart.jsp / checkout.jsp
    ├── orders.jsp / order-confirm.jsp / admin.jsp
    ├── seller/products.jsp / seller/product-form.jsp
    ├── _nav.jsp                              shared navigation bar
    ├── css/style.css
    └── images/products/*.svg                 placeholder product images
```

> Because this is a Maven project the source lives under `src/main/java`
> and the web content under `src/main/webapp`.

---

## 2. MySQL 8 database

**Database name:** `dhanyamart`

| Table        | Purpose                                                        |
|--------------|----------------------------------------------------------------|
| `users`      | login/registration, profile, and role (CUSTOMER / SELLER / ADMIN) |
| `products`   | product catalogue, owned by a seller (FK -> users)             |
| `orders`     | order snapshot (customer details, status, total) placed at checkout |
| `order_items`| line items of an order (FK -> orders, product_id snapshot)     |
| `reviews`    | one rating per product per user (unique key on product+user)   |

**About the cart:** the shopping cart is kept in the `HttpSession` (session cart).
When a customer checks out, the session cart becomes one `orders` row plus its
`order_items` rows in MySQL. So no separate cart table is needed.

The authoritative schema script lives at `src/main/resources/sql/dhanyamart_schema.sql`
(a copy lands at `db/dhanyamart_schema.sql` next to this readme). It contains all
tables with primary keys, foreign keys, indexes and `utf8mb4` collation.

### 2.1 One-time database setup

```sql
-- Option A: run the provided script (recommended)
mysql -u root -p < src/main/resources/sql/dhanyamart_schema.sql

-- Option B: by hand
CREATE DATABASE dhanyamart CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE dhanyamart;
-- ...then run the CREATE TABLE statements from dhanyamart_schema.sql ...
```

> **You can skip this step.** `DBConnection` opens the JDBC URL with
> `createDatabaseIfNotExist=true` and auto-runs `CREATE TABLE IF NOT EXISTS`
> on the first connection, so the database and tables are created
> automatically when the app starts (the MySQL user must have CREATE
> privileges - true for `root`).

### 2.2 Configuring the MySQL username / password

Edit **`src/main/resources/db.properties`** to match your MySQL 8 server
(or set the `DB_PASSWORD` environment variable instead):

```properties
db.url=jdbc:mysql://localhost:3306/dhanyamart?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
db.user=root
db.password=your_mysql_password_here
```

> **`db.properties` is git-ignored** so your real password is never published to
> GitHub - it only exists on your machine. On a fresh clone the app falls back to
> the defaults (`root` / empty password); if your MySQL root user has a password,
> set it locally via `DB_PASSWORD`, e.g. on Windows:
> `setx DB_PASSWORD your_mysql_password_here` (new terminals only).

Then rebuild: `mvn clean package`.

Configuration is resolved with the following precedence (highest first):

1. **JVM system properties** - `-Ddhanyamart.db.url=... -Ddhanyamart.db.user=... -Ddhanyamart.db.password=...`
2. **Environment variables** - `DB_URL`, `DB_USER`, `DB_PASSWORD`
   (also accepted: `DHANYAMART_DB_URL`, `DHANYAMART_DB_USER`, `DHANYAMART_DB_PASSWORD`)
3. **`db.properties`** on the classpath
4. Defaults: `localhost:3306`, user `root`, empty password

---

## 3. JDBC connectivity (reusable DBConnection)

`com.dhanyamart.util.DBConnection` is the single JDBC utility class:

- loads the MySQL Connector/J driver once,
- resolves the URL/user/password from the settings above,
- returns a fresh `Connection` per request via `getConnection()`,
- auto-creates the schema on the first connection by executing the
  statements inside `sql/dhanyamart_schema.sql`.

Every DAO is the single place that touches its SQL table. Controllers and JSPs
are unchanged from the Excel version apart from the new admin delete actions.

### CRUD operations (all synced between the UI and MySQL)

| Module  | Create | Read | Update | Delete |
|---------|--------|------|--------|--------|
| Users   | `/register` inserts a row | `/admin` lists users, login reads a user | `/admin` role dropdown (`updateRole`) | `/admin` user **Delete** button (blocked for sellers with products) |
| Products| `/seller/product` add form | `/products`, `/product`, seller dashboard | `/seller/product` edit form | `/admin` & `/seller/products` **Delete** button |
| Orders  | `/checkout` inserts order + items in one transaction | `/orders`, `/order-confirm`, `/admin` | `/admin` status dropdown (`updateStatus`) | `/admin` order **Delete** button |
| Reviews | `/review` adds a review | `/product` review list | `/review` re-rated by same user | `/product` **Delete my review** button |

Transactional integrity: placing an order inserts the `orders` header and all
`order_items` rows in a single JDBC transaction (commit on success, rollback on
error) and stock is reduced afterwards.

---

## 4. Demo accounts (auto-seeded on first start)

| Role     | Email                  | Password   |
|----------|------------------------|------------|
| Admin    | admin@dhanyamart.com   | Admin@123  |
| Seller   | seller@dhanyamart.com  | Seller@123 |
| Customer | demo@dhanyamart.com    | Demo@123   |

`DataSeeder` also loads 15 example products and a few reviews the first time the
tables are empty. Nothing is overwritten on later starts.

> Note: dropping the `dhanyamart` database resets the app, and the demo
> accounts/products are re-seeded on the next restart.

---

## 5. Setup steps

**Prerequisites:** JDK 17, Apache Maven 3.8+, MySQL 8 server, Tomcat 10.1+.

1. Start MySQL 8 and make sure you can log in (`mysql -u root -p`).
2. Configure the credentials in `src/main/resources/db.properties` (section 2.2).
3. Build the WAR.

**Option A - Eclipse:**
1. File > Import > *Existing Maven Projects* > browse to this folder > Finish.
2. Window > Preferences > Server > Runtime Environments > Add > Tomcat v10.1.
3. Right-click the project > *Run As* > *Run on Server* > choose Tomcat.

**Option B - Command line:**
```
mvn clean package
```
This produces `target/DhanyaMart.war`. Copy it into `TOMCAT_HOME\webapps\`,
then start Tomcat (`bin\startup.bat`).

On first request the `dhanyamart` database and its tables are created
automatically (see section 2.1), and the demo accounts / products are seeded.

> If you see a *Could not register / commit in MySQL* style error, check that
> MySQL is running and that the `db.properties` username/password are correct.

---

## 6. Testing steps

Open **http://localhost:8080/DhanyaMart/** in a browser. Role-based access:

- **Admin** - `admin@dhanyamart.com` / `Admin@123`
  - `/admin` dashboard: overview stats, manage user roles, delete users/orders,
    delete products, update order status.
  - Also has access to `/seller/products`.
- **Seller** - `seller@dhanyamart.com` / `Seller@123`
  - `/seller/products`: their product list (admin sees all), add/edit/delete products.
  - `/seller/product`: add/edit product form with server-side validation.
- **Customer** - `demo@dhanyamart.com` / `Demo@123` (or register a new account)
  - Browse/search/filter `/products`, view `/product?id=N` with reviews.
  - Add to cart, update quantities, `/checkout` (validates 10-digit phone), see
    `/order-confirm?id=N` and `/orders`.
  - Leave a rating on a product page (one review per product per user) and
    delete your own review.

Guards: all protected pages redirect unauthenticated users to `login.jsp`; the
seller pages require SELLER/ADMIN and `/admin` requires ADMIN.

Verify the data actually lives in MySQL while the app runs:

```sql
USE dhanyamart;
SELECT * FROM users;
SELECT * FROM products;
SELECT * FROM orders;
SELECT * FROM order_items;
SELECT * FROM reviews;
```

Register a user / add a product / place an order in the UI and re-run the
SELECTs - the new rows appear immediately.

---

## 7. Security checklist (already implemented)

- Passwords hashed with PBKDF2 + random salt, never stored plain text.
- Every SQL statement uses **prepared statements** (no SQL-injection via input).
- Protected JSPs/servlets redirect unauthenticated users before any output.
- Same error message for unknown email / wrong password (no account enumeration).
- Session fixation prevented via `request.changeSessionId()` after login.
- User input escaped before re-printing in JSPs (XSS protection).
- Server-side validation on product add/edit (name, category, description, price,
  stock, image path) and checkout (name/phone/address).
- Roles stored in session and re-checked on every admin/seller request.
- Orders written in one JDBC transaction (header + items either fully saved or rolled back).
- Foreign keys (RESTRICT/CASCADE) protect data integrity; deleting a seller who
  still owns products is refused by the database.