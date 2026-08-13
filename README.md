# DhanyaMart - Authentication Module

Java + JSP + Servlets + Apache POI e-commerce capstone project.
This module covers ONLY: **Register -> Login -> Session -> Protected Home -> Logout**.

All user data is stored in an **Excel `.xlsx` file** (no MySQL). Passwords are
never stored in plain text - each one is salted and hashed with PBKDF2.

---

## 1. Project structure

```
Dhanya capstone/
├── pom.xml                                  (Maven build + Apache POI dependencies)
├── src/main/java/com/dhanyamart/
│   ├── controller/
│   │   ├── LoginServlet.java                 /login
│   │   ├── RegisterServlet.java              /register
│   │   └── LogoutServlet.java                /logout
│   ├── dao/UserDAO.java                      all users.xlsx read/write logic
│   ├── model/User.java                       user bean (POJO)
│   └── util/
│       ├── ExcelUtil.java                    open/create/save the Excel file
│       └── PasswordUtil.java                 PBKDF2 hash + verify
└── src/main/webapp/
    ├── WEB-INF/web.xml
    ├── login.jsp
    ├── register.jsp
    ├── home.jsp                              (protected page)
    └── css/style.css
```

> Note: because this is a Maven project the source lives under `src/main/java`
> and the web content under `src/main/webapp` (this maps to the `com.dhanyamart/`
> and `webapp/` folders from the requirement).

## 2. Apache POI dependencies (handled by Maven)

`pom.xml` declares:

```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

Maven automatically downloads `poi-ooxml` plus its transitive jars
(`poi`, `poi-ooxml-lite`, `xmlbeans`, `commons-collections4`, `commons-compress`,
`log4j-api`, `commons-math3`, `commons-io`, ...). No manual jar downloads needed.

## 3. How the Excel data file works

- Location: **`<user home>\dhanyamart-data\users.xlsx`**
  (Windows: `C:\Users\<you>\dhanyamart-data\users.xlsx`)
- Created **automatically on first run** (header row written by `ExcelUtil`).
- To use a different folder, set the system property `dhanyamart.data.dir`
  or the environment variable `DHANYAMART_DATA_DIR`.

Columns (row 0 = header):

| user_id | name | email | password | phone | address | created_at |
|---------|------|-------|----------|-------|---------|------------|
| 1001    | ...  | ...   | salt:hash | ...  | ...     | 2026-08-12 10:30:00 |

The `password` cell holds a PBKDF2 `salt:hash` value, e.g. `oKx7fH2n...:Qk4EwRtY...`
It can never be converted back to the real password.

---

## 4. Setup steps

**Prerequisites**
- JDK 17 (already installed on this machine)
- Apache Maven 3.8+ (install or use Eclipse m2e - see below)
- Apache Tomcat 10.1 or 11

**Option A - Run with Eclipse (recommended if no Maven on PATH)**
1. Eclipse > File > Import > *Existing Maven Projects* > browse to this folder > Finish.
2. Window > Preferences > Server > Runtime Environments > Add > Tomcat v10.1 (or v11),
   select your Tomcat folder. (Required only if you want Run-as on Server.)
3. Right-click the project > *Run As* > *Run on Server* > choose Tomcat.

**Option B - Build from the command line**
1. Install Maven and add it to `PATH`.
2. Open a terminal in this folder and run:
   ```
   mvn clean package
   ```
3. This produces `target/DhanyaMart.war`. Copy it into `TOMCAT_HOME\webapps\`,
   then start Tomcat (`bin\startup.bat`).

**No database setup is required.** The Excel file is created on first request.

---

## 5. Testing steps

Open **http://localhost:8080/DhanyaMart/** in a browser.

1. **Access control**
   - Opening the root URL redirects you to `login.jsp` because you are not logged in.
   - Try opening `http://localhost:8080/DhanyaMart/home.jsp` directly in a new tab -
     it redirects back to `login.jsp` (protected page check).

2. **Registration**
   - Click *Create an account*, fill every field (valid email, 10-digit phone,
     password + confirm password must match, address), submit.
   - Success message -> redirected to login page.
   - Try registering again with the **same email** -> duplicate-email error shown.
   - Try short password / bad email / mismatched confirm -> error shown, fields re-filled.

3. **Excel storage**
   - Open `C:\Users\<you>\dhanyamart-data\users.xlsx` in Excel.
   - A new row exists with your details. The `password` column contains a
     **hash** (`salt:hash`), NOT your real password.

4. **Login**
   - Enter the email + password you registered with -> redirected to `home.jsp`
     showing your name and email.
   - Wrong password -> "Invalid email or password." error on `login.jsp`.

5. **Session**
   - On `home.jsp`, check *Session ID* matches the cookie. Keep the tab open.
   - Open a second browser tab and go to `home.jsp` again - it works (session is shared).

6. **Logout**
   - Click *Logout* -> session destroyed -> back to `login.jsp`.
   - Now visit `home.jsp` again -> redirected to `login.jsp` (session is gone).

7. **Restart test**
   - Stop and restart Tomcat, then login again with the same credentials.
     Users persist because they live in the Excel file.

---

## 6. Security checklist (already implemented)

- Passwords hashed with PBKDF2 + random salt, never stored plain text.
- `home.jsp` redirects unauthenticated users (checked before any HTML output).
- Same error message for unknown email / wrong password (no account enumeration).
- Session fixation prevented via `request.changeSessionId()` after login.
- User input escaped before being re-printed in JSPs (XSS protection).
- Excel writes are guarded by a shared lock (thread-safe under concurrent requests).

## 7. Notes on "JDBC connectivity"

Storage is Excel via Apache POI, so **no JDBC/MySQL driver is used** in this module.
`UserDAO` is the single place that touches user data - if you later add a real
database, replace the DAO internals with a JDBC `DriverManager.getConnection(...)`
connection and keep the same method names; no controller changes are needed.

## 8. Switching to Tomcat 9 (if you ever need it)

Replace `jakarta.servlet` imports with `javax.servlet` in the 3 servlet files and
use `javax.servlet-api` 4.0.1 (provided) in `pom.xml`. Everything else stays the same.
