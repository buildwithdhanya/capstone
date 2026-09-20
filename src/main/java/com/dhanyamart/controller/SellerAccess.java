package com.dhanyamart.controller;

import com.dhanyamart.dao.ProductDAO;
import com.dhanyamart.model.Product;
import com.dhanyamart.util.CellUtil;
import jakarta.servlet.http.HttpSession;

/**
 * Shared role checks for the seller dashboard. Kept in one place so the
 * seller servlets and JSPs never repeat the same checks.
 */
public final class SellerAccess {

    private SellerAccess() {
    }

    /** True when the session belongs to a seller or an admin. */
    public static boolean isSellerOrAdmin(HttpSession session) {
        if (session == null || session.getAttribute("user_id") == null) {
            return false;
        }
        String role = (String) session.getAttribute("role");
        return "SELLER".equals(role) || "ADMIN".equals(role);
    }

    /** True when the session belongs to an admin. */
    public static boolean isAdmin(HttpSession session) {
        if (session == null || session.getAttribute("user_id") == null) {
            return false;
        }
        return "ADMIN".equals(session.getAttribute("role"));
    }

    /**
     * True when the product may be managed by this session's user:
     * the product is owned by the user, or the user is the admin.
     */
    public static boolean canManage(Product product, HttpSession session) {
        if (session == null || product == null) {
            return false;
        }
        Integer userId = (Integer) session.getAttribute("user_id");
        String role = (String) session.getAttribute("role");
        return ("SELLER".equals(role) && product.getSellerId() == userId)
                || "ADMIN".equals(role);
    }
}