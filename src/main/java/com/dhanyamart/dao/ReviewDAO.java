package com.dhanyamart.dao;

import com.dhanyamart.model.Review;
import com.dhanyamart.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object - every operation that touches the MySQL {@code reviews}
 * table goes through this class. A customer can rate a product once; a second
 * rating updates the first (same product + user combination, enforced by the
 * unique key uq_reviews_product_user).
 */
public class ReviewDAO {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String COLUMNS =
            "review_id, product_id, user_id, user_name, rating, comment, created_at";

    /**
     * Validates and adds (or replaces) a review. Returns true on success.
     * The existing review for the same product+user is updated in place.
     */
    public boolean addOrUpdate(Review review) {
        if (review == null || review.getProductId() <= 0 || review.getUserId() <= 0
                || review.getRating() < 1 || review.getRating() > 5) {
            return false;
        }
        try (Connection conn = DBConnection.getConnection()) {
            Integer existingId = null;
            String find = "SELECT review_id FROM reviews WHERE product_id = ? AND user_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(find)) {
                ps.setInt(1, review.getProductId());
                ps.setInt(2, review.getUserId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        existingId = rs.getInt(1);
                    }
                }
            }

            if (existingId == null) {
                String sql = "INSERT INTO reviews (product_id, user_id, user_name, rating, comment, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, NOW())";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, review.getProductId());
                    ps.setInt(2, review.getUserId());
                    ps.setString(3, review.getUserName());
                    ps.setInt(4, review.getRating());
                    ps.setString(5, review.getComment());
                    return ps.executeUpdate() > 0;
                }
            } else {
                String sql = "UPDATE reviews SET rating = ?, comment = ?, user_name = ?, created_at = NOW() "
                        + "WHERE review_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, review.getRating());
                    ps.setString(2, review.getComment());
                    ps.setString(3, review.getUserName());
                    ps.setInt(4, existingId);
                    return ps.executeUpdate() > 0;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not save review in MySQL", e);
        }
    }

    /** All reviews for one product, newest first. */
    public List<Review> listByProduct(int productId) {
        List<Review> result = new ArrayList<>();
        String sql = "SELECT " + COLUMNS + " FROM reviews WHERE product_id = ? "
                + "ORDER BY created_at DESC, review_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rowToReview(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not read reviews from MySQL", e);
        }
        return result;
    }

    public Review findById(int reviewId) {
        String sql = "SELECT " + COLUMNS + " FROM reviews WHERE review_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reviewId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rowToReview(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not read review by id from MySQL", e);
        }
        return null;
    }

    /** Deletes a review row (a customer may delete their own review). */
    public boolean delete(int reviewId) {
        String sql = "DELETE FROM reviews WHERE review_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reviewId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete review in MySQL", e);
        }
    }

    private Review rowToReview(ResultSet rs) throws SQLException {
        Review r = new Review();
        r.setReviewId(rs.getInt("review_id"));
        r.setProductId(rs.getInt("product_id"));
        r.setUserId(rs.getInt("user_id"));
        r.setUserName(rs.getString("user_name"));
        r.setRating(rs.getInt("rating"));
        r.setComment(rs.getString("comment"));
        r.setCreatedAt(format(rs.getTimestamp("created_at")));
        return r;
    }

    private String format(Timestamp ts) {
        if (ts == null) {
            return "";
        }
        return ts.toLocalDateTime().format(TIMESTAMP);
    }
}