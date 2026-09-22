package com.dhanyamart.controller;

import com.dhanyamart.dao.ReviewDAO;
import com.dhanyamart.model.Review;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Handles a product review.
 *   POST /review  parms: productId, rating, comment          -> add/replace rating
 *   POST /review  action=delete, reviewId, productId         -> delete own review
 * Saves (or replaces) the current user's rating then returns to the product.
 */
@WebServlet("/review")
public class ReviewServlet extends HttpServlet {

    private final ReviewDAO reviewDAO = new ReviewDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Integer userId = (Integer) (request.getSession(false) != null
                ? request.getSession(false).getAttribute("user_id") : null);
        if (userId == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        if ("delete".equals(request.getParameter("action"))) {
            handleDelete(request, response, userId);
            return;
        }

        int productId = parseId(request.getParameter("productId"));
        int rating = parseId(request.getParameter("rating"));
        String comment = request.getParameter("comment");
        if (comment == null) comment = "";
        comment = comment.trim();

        // Server-side validation (same rules as the JSP).
        if (productId <= 0) {
            response.sendRedirect("products.jsp");
            return;
        }
        if (rating < 1 || rating > 5) {
            response.sendRedirect("product?id=" + productId + "&msg=invalid-rating");
            return;
        }
        if (comment.isEmpty() || comment.length() > 500) {
            response.sendRedirect("product?id=" + productId + "&msg=invalid-comment");
            return;
        }

        Review review = new Review();
        review.setProductId(productId);
        review.setUserId(userId);
        review.setUserName((String) request.getSession(false).getAttribute("name"));
        review.setRating(rating);
        review.setComment(comment);

        reviewDAO.addOrUpdate(review);
        response.sendRedirect("product?id=" + productId + "&msg=reviewed");
    }

    /**
     * A customer may delete only their own review. Ownership is re-checked
     * against the database before the row is removed.
     */
    private void handleDelete(HttpServletRequest request, HttpServletResponse response,
                              int userId) throws IOException {
        int reviewId = parseId(request.getParameter("reviewId"));
        int productId = parseId(request.getParameter("productId"));
        Review review = reviewDAO.findById(reviewId);
        if (review != null && review.getUserId() == userId
                && review.getProductId() == productId && reviewDAO.delete(reviewId)) {
            response.sendRedirect("product?id=" + productId + "&msg=deleted-review");
        } else {
            response.sendRedirect("product?id=" + productId);
        }
    }

    private int parseId(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }
}