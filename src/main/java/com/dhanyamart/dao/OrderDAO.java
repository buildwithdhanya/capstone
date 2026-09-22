package com.dhanyamart.dao;

import com.dhanyamart.model.Order;
import com.dhanyamart.model.OrderItem;
import com.dhanyamart.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object - every operation that touches the MySQL {@code orders}
 * and {@code order_items} tables goes through this class.
 *
 * An order is a snapshot: the customer name/address and each line item's
 * product name/price are copied at purchase time, so later product edits
 * never change past orders. Placing an order is transactional - the order
 * header and all its line items are inserted and committed together.
 */
public class OrderDAO {

    public static final String STATUS_PLACED = "PLACED";
    public static final String STATUS_PACKED = "PACKED";
    public static final String STATUS_SHIPPED = "SHIPPED";
    public static final String STATUS_DELIVERED = "DELIVERED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    public static final String[] STATUSES = {
            STATUS_PLACED, STATUS_PACKED, STATUS_SHIPPED, STATUS_DELIVERED, STATUS_CANCELLED
    };

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Reads an order plus its line items via a left join (newest order first). */
    private static final String SELECT_SQL =
            "SELECT o.order_id, o.user_id, o.user_name, o.user_email, o.phone, o.address, "
                    + "o.order_date, o.status, o.total, "
                    + "oi.order_item_id, oi.product_id, oi.product_name, oi.price, oi.quantity "
                    + "FROM orders o LEFT JOIN order_items oi ON oi.order_id = o.order_id "
                    + "%s ORDER BY o.order_id DESC, oi.order_item_id";

    /** Valid flow statuses a customer or admin may set. */
    public static boolean isValidStatus(String status) {
        if (status == null) return false;
        for (String s : STATUSES) {
            if (s.equals(status)) return true;
        }
        return false;
    }

    /**
     * Saves a new order with its line items in one transaction and returns
     * the generated order_id (0 when nothing was written).
     */
    public int create(Order order) {
        if (order == null || order.getItems().isEmpty()) {
            return 0;
        }
        int generatedId = 0;
        String insertOrder = "INSERT INTO orders "
                + "(user_id, user_name, user_email, phone, address, order_date, status, total) "
                + "VALUES (?, ?, ?, ?, ?, NOW(), ?, ?)";
        String insertItem = "INSERT INTO order_items (order_id, product_id, product_name, price, quantity) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection()) {
            boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(insertOrder,
                    PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, order.getUserId());
                ps.setString(2, order.getUserName());
                ps.setString(3, order.getUserEmail());
                ps.setString(4, order.getPhone());
                ps.setString(5, order.getAddress());
                ps.setString(6, order.getStatus() != null && isValidStatus(order.getStatus())
                        ? order.getStatus() : STATUS_PLACED);
                ps.setDouble(7, order.getTotal());
                if (ps.executeUpdate() == 0) {
                    conn.rollback();
                    return 0;
                }
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        generatedId = keys.getInt(1);
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(insertItem)) {
                for (OrderItem item : order.getItems()) {
                    ps.setInt(1, generatedId);
                    ps.setInt(2, item.getProductId());
                    ps.setString(3, item.getProductName());
                    ps.setDouble(4, item.getPrice());
                    ps.setInt(5, item.getQuantity());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            conn.commit();
            conn.setAutoCommit(oldAutoCommit);
        } catch (SQLException e) {
            throw new RuntimeException("Could not create order in MySQL", e);
        }
        return generatedId;
    }

    /** Orders placed by one customer, newest first. */
    public List<Order> listByUser(int userId) {
        return queryOrders("WHERE o.user_id = ?", userId);
    }

    /** Every order in the system (admin dashboard), newest first. */
    public List<Order> listAll() {
        return queryOrders("", (Object[]) new Object[0]);
    }

    public Order findById(int orderId) {
        List<Order> orders = queryOrders("WHERE o.order_id = ?", orderId);
        return orders.isEmpty() ? null : orders.get(0);
    }

    public boolean updateStatus(int orderId, String status) {
        if (!isValidStatus(status)) {
            return false;
        }
        String sql = "UPDATE orders SET status = ? WHERE order_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, orderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not update order status in MySQL", e);
        }
    }

    /** Deletes an order; its line items are removed by the CASCADE foreign key. */
    public boolean delete(int orderId) {
        String sql = "DELETE FROM orders WHERE order_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete order in MySQL", e);
        }
    }

    /** Total value of all non-cancelled orders (admin revenue figure). */
    public double totalRevenue() {
        String sql = "SELECT COALESCE(SUM(total), 0) FROM orders WHERE status <> ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, STATUS_CANCELLED);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not sum order revenue in MySQL", e);
        }
        return 0.0;
    }

    /** Runs the joined order query and groups the rows into Order objects. */
    private List<Order> queryOrders(String whereClause, Object... params) {
        String sql = String.format(SELECT_SQL, whereClause);
        List<Order> result = new ArrayList<>();
        Map<Integer, Order> byId = new LinkedHashMap<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int orderId = rs.getInt("order_id");
                    Order order = byId.get(orderId);
                    if (order == null) {
                        order = rowToOrder(rs);
                        byId.put(orderId, order);
                        result.add(order);
                    }
                    Integer itemId = (Integer) rs.getObject("order_item_id");
                    if (itemId != null) {
                        order.getItems().add(rowToOrderItem(rs));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not read orders from MySQL", e);
        }
        return result;
    }

    private Order rowToOrder(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setOrderId(rs.getInt("order_id"));
        order.setUserId(rs.getInt("user_id"));
        order.setUserName(rs.getString("user_name"));
        order.setUserEmail(rs.getString("user_email"));
        order.setPhone(rs.getString("phone"));
        order.setAddress(rs.getString("address"));
        order.setOrderDate(format(rs.getTimestamp("order_date")));
        order.setStatus(rs.getString("status"));
        order.setTotal(rs.getDouble("total"));
        return order;
    }

    private OrderItem rowToOrderItem(ResultSet rs) throws SQLException {
        OrderItem item = new OrderItem();
        item.setProductId(rs.getInt("product_id"));
        item.setProductName(rs.getString("product_name"));
        item.setPrice(rs.getDouble("price"));
        item.setQuantity(rs.getInt("quantity"));
        return item;
    }

    private String format(Timestamp ts) {
        if (ts == null) {
            return "";
        }
        return ts.toLocalDateTime().format(TIMESTAMP);
    }
}