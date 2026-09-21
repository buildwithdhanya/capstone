package com.dhanyamart.controller;

import com.dhanyamart.dao.ProductDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Product catalogue.
 *   GET /products                  -> every product
 *   GET /products?q=rice           -> text search on name/description
 *   GET /products?cat=Spices       -> filter by category (may be combined with q)
 *   GET /products?sort=price_asc   -> sort the result set (see ProductDAO.sortBy)
 */
@WebServlet("/products")
public class ProductsServlet extends HttpServlet {

    private final ProductDAO productDAO = new ProductDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Protected page: redirect to login when not authenticated.
        if (request.getSession(false) == null
                || request.getSession(false).getAttribute("user_id") == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        String query = request.getParameter("q");
        String category = request.getParameter("cat");
        String sort = request.getParameter("sort");

        // Keep the search box + filter dropdown filled with the current values.
        request.setAttribute("q", query == null ? "" : query);
        request.setAttribute("cat", category == null ? "" : category);
        request.setAttribute("sort", sort == null ? "" : sort);
        request.setAttribute("products", productDAO.search(query, category, sort));
        request.setAttribute("categories", productDAO.listCategories());

        request.getRequestDispatcher("products.jsp").forward(request, response);
    }
}