-- =====================================================================
--  DhanyaMart - MySQL 8 schema
-- =====================================================================
--  This script creates the `dhanyamart` database and every table the
--  application uses. The DAO layer also auto-runs the same CREATE TABLE
--  IF NOT EXISTS statements on first connect, so running this file by
--  hand is optional - but it is the authoritative schema for the review.
--
--  Run it inside the MySQL client:
--      mysql -u root -p < dhanyamart_schema.sql
--
--  Tables:
--    users         authentication + profile info (role: CUSTOMER/SELLER/ADMIN)
--    products      product catalogue (owned by a SELLER/ADMIN user)
--    orders        an order snapshot placed at checkout
--    order_items   line items of an order (FK -> orders, product_id)
--    reviews       one rating per product per user
--
--  Note on the shopping cart: it is kept in the HttpSession (session cart),
--  so there is no cart table. When a customer checks out, the session cart
--  is converted into an `orders` + `order_items` row pair in MySQL.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS dhanyamart
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE dhanyamart;

-- ----------------------------------------------------------------
-- users
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    user_id    INT           NOT NULL AUTO_INCREMENT,
    name       VARCHAR(100)  NOT NULL,
    email      VARCHAR(150)  NOT NULL,
    password   VARCHAR(255)  NOT NULL COMMENT 'PBKDF2 salt:hash, never plain text',
    phone      VARCHAR(20)   NULL,
    address    VARCHAR(500)  NULL,
    role       VARCHAR(20)   NOT NULL DEFAULT 'CUSTOMER',
    created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE KEY uq_users_email (email)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ----------------------------------------------------------------
-- products
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS products (
    product_id  INT          NOT NULL AUTO_INCREMENT,
    seller_id   INT          NOT NULL,
    name        VARCHAR(100) NOT NULL,
    category    VARCHAR(100) NULL,
    description TEXT         NULL,
    price       DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    stock       INT          NOT NULL DEFAULT 0,
    image       VARCHAR(255) NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (product_id),
    KEY idx_products_seller (seller_id),
    KEY idx_products_category (category),
    CONSTRAINT fk_products_seller FOREIGN KEY (seller_id)
        REFERENCES users (user_id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ----------------------------------------------------------------
-- orders
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS orders (
    order_id   INT           NOT NULL AUTO_INCREMENT,
    user_id    INT           NOT NULL,
    user_name  VARCHAR(100)  NULL,
    user_email VARCHAR(150)  NULL,
    phone      VARCHAR(20)   NULL,
    address    VARCHAR(500)  NULL,
    order_date DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status     VARCHAR(20)   NOT NULL DEFAULT 'PLACED',
    total      DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    PRIMARY KEY (order_id),
    KEY idx_orders_user (user_id),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ----------------------------------------------------------------
-- order_items  (snapshot of each cart line at purchase time)
-- product_id is SET NULL when a product is later deleted so that past
-- orders keep their name/price snapshot intact.
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_items (
    order_item_id INT           NOT NULL AUTO_INCREMENT,
    order_id      INT           NOT NULL,
    product_id    INT           NULL,
    product_name  VARCHAR(100)  NOT NULL,
    price         DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    quantity      INT           NOT NULL DEFAULT 1,
    PRIMARY KEY (order_item_id),
    KEY idx_order_items_order (order_id),
    KEY idx_order_items_product (product_id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id)
        REFERENCES orders (order_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id)
        REFERENCES products (product_id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ----------------------------------------------------------------
-- reviews  (one review per product per user, enforced by a unique key)
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reviews (
    review_id  INT          NOT NULL AUTO_INCREMENT,
    product_id INT          NOT NULL,
    user_id    INT          NOT NULL,
    user_name  VARCHAR(100) NULL,
    rating     TINYINT      NOT NULL,
    comment    VARCHAR(500) NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (review_id),
    KEY idx_reviews_product (product_id),
    KEY idx_reviews_user (user_id),
    CONSTRAINT uq_reviews_product_user UNIQUE (product_id, user_id),
    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id)
        REFERENCES products (product_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;