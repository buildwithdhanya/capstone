package com.dhanyamart.controller;

import com.dhanyamart.dao.ProductDAO;
import com.dhanyamart.model.Product;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Product catalogue (paginated).
 *   GET /products                  -> every product, first page
 *   GET /products?q=rice&page=2    -> text search on name/description
 *   GET /products?cat=Spices       -> filter by category (may be combined with q)
 *   GET /products?sort=price_asc   -> sort the result set (see ProductDAO.sortBy)
 */
@WebServlet("/products")
public class ProductsServlet extends HttpServlet {

    private static final int PAGE_SIZE = 9;

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
        int page = parsePage(request.getParameter("page"));

        // Keep the search box + filter dropdown filled with the current values.
        request.setAttribute("q", query == null ? "" : query);
        request.setAttribute("cat", category == null ? "" : category);
        request.setAttribute("sort", sort == null ? "" : sort);

        List<Product> all = productDAO.search(query, category, sort);
        int totalPages = Math.max(1, (int) Math.ceil(all.size() / (double) PAGE_SIZE));
        page = Math.min(page, totalPages);

        int fromIndex = (page - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, all.size());
        List<Product> pageProducts = all.subList(fromIndex, toIndex);

        request.setAttribute("products", pageProducts);
        request.setAttribute("total", all.size());
        request.setAttribute("page", page);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("categories", productDAO.listCategories());

        request.getRequestDispatcher("products.jsp").forward(request, response);
    }

    /** Parses the "page" param, falling back to page 1 for anything invalid. */
    private int parsePage(String value) {
        try {
            int p = Integer.parseInt(value);
            return p < 1 ? 1 : p;
        } catch (Exception e) {
            return 1;
        }
    }
}