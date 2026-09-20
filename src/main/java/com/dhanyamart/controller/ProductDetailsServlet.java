package com.dhanyamart.controller;

import com.dhanyamart.dao.ProductDAO;
import com.dhanyamart.dao.ReviewDAO;
import com.dhanyamart.model.Product;
import com.dhanyamart.model.Review;
import com.dhanyamart.util.CellUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Product details page.
 *   GET /product?id=101 -> shows the product plus its reviews & ratings
 */
@WebServlet("/product")
public class ProductDetailsServlet extends HttpServlet {

    private final ProductDAO productDAO = new ProductDAO();
    private final ReviewDAO reviewDAO = new ReviewDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (request.getSession(false) == null
                || request.getSession(false).getAttribute("user_id") == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        int productId = CellUtil.parseInt(request.getParameter("id"));
        Product product = productDAO.findById(productId);
        if (product == null) {
            request.setAttribute("error", "Product not found.");
            request.getRequestDispatcher("products.jsp").forward(request, response);
            return;
        }

        List<Review> reviews = reviewDAO.listByProduct(productId);
        request.setAttribute("product", product);
        request.setAttribute("reviews", reviews);
        request.setAttribute("reviewCount", reviews.size());
        request.setAttribute("reviewAverage", average(reviews));

        request.getRequestDispatcher("product.jsp").forward(request, response);
    }

    private double average(List<Review> reviews) {
        if (reviews.isEmpty()) return 0.0;
        double sum = 0.0;
        for (Review r : reviews) sum += r.getRating();
        return sum / reviews.size();
    }
}