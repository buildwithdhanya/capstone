package com.dhanyamart.dao;

import com.dhanyamart.model.User;
import com.dhanyamart.util.DBConnection;
import com.dhanyamart.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object - every operation that touches the MySQL {@code users}
 * table goes through this class. This is the ONLY class the controllers talk
 * to for user data, so data access stayed isolated when the storage was moved
 * from Excel to the database.
 *
 * All SQL uses prepared statements (avoids SQL injection) and MySQL's
 * {@code users.user_id} auto-increment column generates the ids.
 *
 * Role column: CUSTOMER, SELLER or ADMIN. Accounts created through the
 * register form are always CUSTOMER.
 */
public class UserDAO {

    public static final String ROLE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_SELLER = "SELLER";
    public static final String ROLE_ADMIN = "ADMIN";

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String COLUMNS =
            "user_id, name, email, password, phone, address, role, created_at";

    /** Returns true if a row with the given email already exists. */
    public boolean emailExists(String email) {
        return findByEmail(email) != null;
    }

    /** Finds a user by email (email match is case-insensitive). */
    public User findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String sql = "SELECT " + COLUMNS + " FROM users WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rowToUser(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not read user by email from MySQL", e);
        }
        return null;
    }

    /** Finds a user by numeric id. */
    public User findById(int userId) {
        String sql = "SELECT " + COLUMNS + " FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = rowToUser(rs);
                    user.setPassword(null); // never pass the hash around
                    return user;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not read user by id from MySQL", e);
        }
        return null;
    }

    /** Returns every user in the users table (safest for the admin dashboard). */
    public List<User> listAll() {
        List<User> result = new ArrayList<>();
        String sql = "SELECT " + COLUMNS + " FROM users ORDER BY user_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User user = rowToUser(rs);
                user.setPassword(null);
                result.add(user);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not list users from MySQL", e);
        }
        return result;
    }

    /** Returns the role ("CUSTOMER" when a row has none, e.g. legacy data). */
    public String getRole(int userId) {
        String sql = "SELECT role FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return roleOrDefault(rs.getString("role"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not read user role from MySQL", e);
        }
        return ROLE_CUSTOMER;
    }

    /** Changes a user's role (admin action). */
    public boolean updateRole(int userId, String role) {
        String normalized = normalizeRole(role);
        if (normalized == null) {
            return false;
        }
        String sql = "UPDATE users SET role = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, normalized);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not update user role in MySQL", e);
        }
    }

    /**
     * Verifies email + password against the users table.
     * Returns the matching User (without the password hash) or null on failure.
     */
    public User login(String email, String rawPassword) {
        if (email == null || email.isBlank() || rawPassword == null) {
            return null;
        }
        String sql = "SELECT " + COLUMNS + " FROM users WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null; // email not found
                }
                String storedHash = rs.getString("password");
                if (!PasswordUtil.verifyPassword(rawPassword, storedHash)) {
                    return null; // email found but wrong password
                }
                User user = rowToUser(rs);
                user.setPassword(null); // never pass the hash around
                return user;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not log in against MySQL", e);
        }
    }

    /**
     * Adds a new user row to the users table. The password is hashed before
     * inserting. Returns false if the email is already taken (the unique key
     * on email stops duplicates). New registrations get the CUSTOMER role.
     */
    public boolean registerUser(User user) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()
                || user.getPassword() == null || user.getPassword().isBlank()) {
            return false;
        }
        String sql = "INSERT INTO users (name, email, password, phone, address, role, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, NOW())";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail().trim());
            // ONLY the salted hash is stored in MySQL, never the plain text
            ps.setString(3, PasswordUtil.hashPassword(user.getPassword()));
            ps.setString(4, user.getPhone());
            ps.setString(5, user.getAddress());
            ps.setString(6, roleOrDefault(user.getRole()));
            if (ps.executeUpdate() == 0) {
                return false;
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setUserId(keys.getInt(1));
                }
            }
            return true;
        } catch (SQLIntegrityConstraintViolationException e) {
            return false; // duplicate email
        } catch (SQLException e) {
            throw new RuntimeException("Could not register user in MySQL", e);
        }
    }

    /** Internal: registers a user with an explicit role (used by the DataSeeder). */
    public boolean registerUser(User user, String role) {
        user.setRole(role);
        return registerUser(user);
    }

    /**
     * Deletes a user. Fails (returns false) when the user still owns products
     * - the {@code fk_products_seller} foreign key uses RESTRICT, so a seller
     * with products can never be removed.
     */
    public boolean delete(int userId) {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // FK RESTRICT (or any constraint) -> record is still referenced
            return false;
        }
    }

    /** Maps one result-set row into a User object. */
    private User rowToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setPhone(rs.getString("phone"));
        user.setAddress(rs.getString("address"));
        user.setRole(roleOrDefault(rs.getString("role")));
        user.setCreatedAt(format(rs.getTimestamp("created_at")));
        return user;
    }

    private String roleOrDefault(String role) {
        if (role == null || role.isBlank()) {
            return ROLE_CUSTOMER;
        }
        return role.trim().toUpperCase();
    }

    /** Returns the normalised role or null when the value is not a valid role. */
    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        String normalized = role.trim().toUpperCase();
        if (!normalized.equals(ROLE_CUSTOMER) && !normalized.equals(ROLE_SELLER)
                && !normalized.equals(ROLE_ADMIN)) {
            return null;
        }
        return normalized;
    }

    private String format(Timestamp ts) {
        if (ts == null) {
            return "";
        }
        return ts.toLocalDateTime().format(TIMESTAMP);
    }
}