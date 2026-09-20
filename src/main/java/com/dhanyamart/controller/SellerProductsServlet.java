package com.dhanyamart.controller;

import com.dhanyamart.dao.ProductDAO;
import com.dhanyamart.model.Product;
import com.dhanyamart.util.CellUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Seller dashboard.
 *   GET /seller/products -> lists the products owned by the logged-in seller
 *                           (an ADMIN sees every product here as well).
 */
@WebServlet("/seller/products")
public class SellerProductsServlet extends HttpServlet {

    private final ProductDAO productDAO = new ProductDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (!SellerAccess.isSellerOrAdmin(session)) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        int userId = (Integer) session.getAttribute("user_id");
        String role = (String) session.getAttribute("role");

        List<Product> products = new ArrayList<>();
        for (Product p : productDAO.listAll()) {
            if ("ADMIN".equals(role) || p.getSellerId() == userId) {
                products.add(p);
            }
        }

        request.setAttribute("products", products);
        request.setAttribute("role", role);
        request.getRequestDispatcher("/seller/products.jsp").forward(request, response);
    }
}