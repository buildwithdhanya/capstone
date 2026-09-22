package com.dhanyamart.dao;

import com.dhanyamart.model.Product;
import com.dhanyamart.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Data Access Object - every operation that touches the MySQL {@code products}
 * table goes through this class (add / edit / delete / search / stock management).
 *
 * All SQL uses prepared statements and the WHERE clauses for search/filter are
 * built dynamically with parameters - no string concatenation of user input.
 */
public class ProductDAO {

    public static final String DEFAULT_IMAGE = "images/products/generic.svg";

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String COLUMNS =
            "product_id, seller_id, name, category, description, price, stock, image, created_at";

    /** All products. */
    public List<Product> listAll() {
        return search(null, null);
    }

    /**
     * Products matching an optional text query (name/description, case-insensitive)
     * and an optional category (exact, case-insensitive). Null/blank means "all".
     *
     * @param query    free text search, or null for all
     * @param category category filter, or null for all
     * @param sort     one of "price_asc", "price_desc", "newest", "name", or null
     */
    public List<Product> search(String query, String category) {
        return search(query, category, null);
    }

    /** Same as search(query, category) but sorts the result set in SQL. */
    public List<Product> search(String query, String category, String sort) {
        List<Object> params = new ArrayList<>();
        List<String> conds = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM products");

        String cat = category == null ? "" : category.trim();
        String q = query == null ? "" : query.trim().toLowerCase();

        if (!cat.isEmpty()) {
            conds.add("LOWER(category) = LOWER(?)");
            params.add(cat);
        }
        if (!q.isEmpty()) {
            conds.add("(LOWER(name) LIKE ? OR LOWER(description) LIKE ?)");
            params.add("%" + q + "%");
            params.add("%" + q + "%");
        }
        if (!conds.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conds));
        }
        sql.append(' ').append(orderBy(sort));

        List<Product> result = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rowToProduct(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not search products in MySQL", e);
        }
        return result;
    }

    /** Maps a sort key to an SQL ORDER BY clause (no-op sorts by id). */
    private String orderBy(String sort) {
        if (sort == null || sort.isBlank()) {
            return "ORDER BY product_id";
        }
        switch (sort.trim()) {
            case "price_asc":
                return "ORDER BY price ASC, name ASC";
            case "price_desc":
                return "ORDER BY price DESC, name ASC";
            case "newest":
                return "ORDER BY created_at DESC, product_id DESC";
            case "name":
                return "ORDER BY name ASC";
            default:
                return "ORDER BY product_id";
        }
    }

    /** Applies the sort key to a product list (kept for API compatibility). */
    public void sortBy(List<Product> products, String sort) {
        if (products == null || products.isEmpty() || sort == null || sort.isBlank()) {
            return;
        }
        switch (sort.trim()) {
            case "price_asc":
                products.sort(Comparator.comparingDouble(Product::getPrice)
                        .thenComparing(Product::getName, String.CASE_INSENSITIVE_ORDER));
                break;
            case "price_desc":
                products.sort(Comparator.comparingDouble(Product::getPrice).reversed()
                        .thenComparing(Product::getName, String.CASE_INSENSITIVE_ORDER));
                break;
            case "newest":
                products.sort(Comparator.comparing(Product::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())));
                break;
            case "name":
                products.sort(Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER));
                break;
            default:
                break;
        }
    }

    /** Distinct categories (sorted), used for the filter dropdown. */
    public List<String> listCategories() {
        List<String> result = new ArrayList<>();
        String sql = "SELECT DISTINCT category FROM products "
                + "WHERE category IS NOT NULL AND TRIM(category) <> '' ORDER BY category";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(rs.getString(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not list product categories in MySQL", e);
        }
        return result;
    }

    public Product findById(int productId) {
        String sql = "SELECT " + COLUMNS + " FROM products WHERE product_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rowToProduct(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not read product by id from MySQL", e);
        }
        return null;
    }

    /** Adds a new product. The id is generated by MySQL auto-increment. */
    public boolean add(Product product) {
        String sql = "INSERT INTO products (seller_id, name, category, description, price, stock, image, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, product.getSellerId());
            ps.setString(2, product.getName());
            ps.setString(3, product.getCategory());
            ps.setString(4, product.getDescription());
            ps.setDouble(5, product.getPrice());
            ps.setInt(6, product.getStock());
            ps.setString(7, safeImage(product.getImage()));
            if (ps.executeUpdate() == 0) {
                return false;
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    product.setProductId(keys.getInt(1));
                }
            }
            return true;
        } catch (SQLException e) {
            throw new RuntimeException("Could not add product in MySQL", e);
        }
    }

    /** Updates an existing product row (matched by product_id). */
    public boolean update(Product product) {
        String sql = "UPDATE products SET name = ?, category = ?, description = ?, "
                + "price = ?, stock = ?, image = ? WHERE product_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, product.getName());
            ps.setString(2, product.getCategory());
            ps.setString(3, product.getDescription());
            ps.setDouble(4, product.getPrice());
            ps.setInt(5, product.getStock());
            ps.setString(6, safeImage(product.getImage()));
            ps.setInt(7, product.getProductId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not update product in MySQL", e);
        }
    }

    /**
     * Deletes a product row (admin/seller action). Reviews are removed by the
     * {@code fk_reviews_product} CASCADE, while past {@code order_items} keep
     * their snapshot (product_id set to NULL).
     */
    public boolean delete(int productId) {
        String sql = "DELETE FROM products WHERE product_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete product in MySQL", e);
        }
    }

    /** Lowers the stock level after an order is placed (never below zero). */
    public void decrementStock(int productId, int quantity) {
        String sql = "UPDATE products SET stock = GREATEST(0, stock - ?) WHERE product_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setInt(2, productId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not decrement stock in MySQL", e);
        }
    }

    private Product rowToProduct(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setProductId(rs.getInt("product_id"));
        p.setSellerId(rs.getInt("seller_id"));
        p.setName(rs.getString("name"));
        p.setCategory(rs.getString("category"));
        p.setDescription(rs.getString("description"));
        p.setPrice(rs.getDouble("price"));
        p.setStock(rs.getInt("stock"));
        p.setImage(safeImage(rs.getString("image")));
        p.setCreatedAt(format(rs.getTimestamp("created_at")));
        return p;
    }

    private String safeImage(String image) {
        if (image == null || image.isBlank()) {
            return DEFAULT_IMAGE;
        }
        return image;
    }

    private String format(Timestamp ts) {
        if (ts == null) {
            return "";
        }
        return ts.toLocalDateTime().format(TIMESTAMP);
    }
}