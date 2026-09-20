# DhanyaMart - Handicrafts E-commerce Web Application

Java + JSP + Servlets + Apache POI e-commerce capstone project.
The full flow is implemented: **Register -> Login -> Product catalogue -> Search/Filter ->
Product details + reviews -> Cart -> Checkout -> Orders, plus Seller and Admin dashboards**.

All data is stored in **Excel `.xlsx` files** (no MySQL). Passwords are never stored
in plain text - each one is salted and hashed with PBKDF2.

---

## 1. Project structure

```
Dhanya capstone/
├── pom.xml                                  (Maven build + Apache POI dependencies)
├── src/main/java/com/dhanyamart/
│   ├── controller/
│   │   ├── LoginServlet.java                 /login
│   │   ├── RegisterServlet.java              /register
│   │   ├── LogoutServlet.java                /logout
│   │   ├── ProductsServlet.java              /products          (browse + search/filter)
│   │   ├── ProductDetailsServlet.java        /product?id=N      (details + reviews)
│   │   ├── ReviewServlet.java                /review            (POST, add rating)
│   │   ├── CartServlet.java                  /cart              (session cart)
│   │   ├── CheckoutServlet.java              /checkout          (place order)
│   │   ├── OrderConfirmationServlet.java     /order-confirm?id=N
│   │   ├── OrdersServlet.java                /orders            (my orders)
│   │   ├── SellerProductsServlet.java        /seller/products   (seller dashboard)
│   │   ├── SellerProductServlet.java         /seller/product    (add/edit/delete)
│   │   ├── AdminServlet.java                 /admin             (overview/users/products/orders)
│   │   ├── SellerAccess.java                 shared seller/admin role checks
│   │   └── DataSeeder.java                   seeds demo accounts, products, reviews
│   ├── dao/UserDAO.java                      users.xlsx logic
│   ├── dao/ProductDAO.java                   products.xlsx logic
│   ├── dao/OrderDAO.java                     orders.xlsx logic
│   ├── dao/ReviewDAO.java                    reviews.xlsx logic
│   ├── model/                                User, Product, CartItem, Order, OrderItem, Review
│   └── util/
│       ├── ExcelUtil.java                    open/create/save the Excel files
│       ├── CellUtil.java                     cell read/write helpers
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

## 2. Apache POI dependencies (handled by Maven)

`pom.xml` declares `poi-ooxml` 5.2.5; Maven pulls in `poi`, `poi-ooxml-lite`,
`xmlbeans`, `commons-collections4`, `commons-compress`, `log4j-api`, etc.
No manual jar downloads needed.

## 3. How the Excel data files work

Location: **`<user home>\dhanyamart-data\`** (Windows: `C:\Users\<you>\dhanyamart-data\`).
Created **automatically on first run** (header rows written by `ExcelUtil`).
To use a different folder, set the system property `dhanyamart.data.dir` or the
environment variable `DHANYAMART_DATA_DIR`.

| File         | Contents                                              |
|--------------|-------------------------------------------------------|
| users.xlsx   | user_id, name, email, password (salt:hash), phone, address, created_at, role |
| products.xlsx| product_id, seller_id, name, category, description, price, stock, image, created_at |
| orders.xlsx  | order_id, user info, phone, address, order_date, status, total, items |
| reviews.xlsx | review_id, product_id, user_id, user_name, rating, comment, created_at |

Order line items are stored in the `items` cell as `productId|name|price|qty;` (repeated).
The `password` cell holds a PBKDF2 `salt:hash` value that can never be converted back.

## 4. Demo accounts (auto-seeded on first start)

| Role     | Email                  | Password   |
|----------|------------------------|------------|
| Admin    | admin@dhanyamart.com   | Admin@123  |
| Seller   | seller@dhanyamart.com  | Seller@123 |
| Customer | demo@dhanyamart.com    | Demo@123   |

`DataSeeder` also loads 15 example products and a few reviews the first time
products.xlsx is created. Nothing is overwritten on later starts.

> Note: deleting the files under `dhanyamart-data` resets the app, and the
> demo accounts/products are re-seeded on the next restart.

## 5. Setup steps

**Prerequisites:** JDK 17, Apache Maven 3.8+, Apache Tomcat 10.1+.

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

**No database setup is required** - the Excel files are created on first request.

## 6. Testing steps

Open **http://localhost:8080/DhanyaMart/** in a browser. Role-based access:

- **Admin** - `admin@dhanyamart.com` / `Admin@123`
  - `/admin` dashboard: overview stats, manage user roles, delete products, update order status.
  - Also has access to `/seller/products`.
- **Seller** - `seller@dhanyamart.com` / `Seller@123`
  - `/seller/products`: their product list (admin sees all), add/edit/delete products.
  - `/seller/product`: add/edit product form with server-side validation.
- **Customer** - `demo@dhanyamart.com` / `Demo@123` (or register a new account)
  - Browse/search/filter `/products`, view `/product?id=N` with reviews.
  - Add to cart, update quantities, `/checkout` (validates 10-digit phone), see
    `/order-confirm?id=N` and `/orders`.
  - Leave a rating on a product page (one review per product per user).

Guards: all protected pages redirect unauthenticated users to `login.jsp`; the
seller pages require SELLER/ADMIN and `/admin` requires ADMIN.

## 7. Security checklist (already implemented)

- Passwords hashed with PBKDF2 + random salt, never stored plain text.
- Protected JSPs/servlets redirect unauthenticated users before any output.
- Same error message for unknown email / wrong password (no account enumeration).
- Session fixation prevented via `request.changeSessionId()` after login.
- User input escaped before re-printing in JSPs (XSS protection).
- Server-side validation on product add/edit (name, category, description, price,
  stock, image path) and checkout (name/phone/address).
- Roles stored in session and re-checked on every admin/seller request.
- Excel writes guarded by a shared lock (thread-safe under concurrent requests).

## 8. "JDBC connectivity" note

Storage is Excel via Apache POI, so no JDBC/MySQL driver is used. Each DAO is the
single place that touches its data file - to switch to MySQL later, replace the DAO
internals with JDBC and keep the same method names; controller/JSP changes are not needed.