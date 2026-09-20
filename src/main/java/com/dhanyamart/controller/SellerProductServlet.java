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
import java.util.HashSet;
import java.util.Set;

/**
 * Add / edit / delete products (seller and admin).
 *
 *   GET  /seller/product?id=101  -> load the form for editing
 *   GET  /seller/product         -> blank "Add product" form
 *   POST /seller/product action=save   -> create or update
 *   POST /seller/product action=delete -> remove a product
 */
@WebServlet("/seller/product")
public class SellerProductServlet extends HttpServlet {

    private static final Set<String> VALID_CATEGORIES = new HashSet<>();
    static {
        VALID_CATEGORIES.add("Grains & Rice");
        VALID_CATEGORIES.add("Pulses & Dals");
        VALID_CATEGORIES.add("Spices");
        VALID_CATEGORIES.add("Oils & Ghee");
        VALID_CATEGORIES.add("Pickles & Chutneys");
        VALID_CATEGORIES.add("Snacks & Sweets");
    }

    private final ProductDAO productDAO = new ProductDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (!SellerAccess.isSellerOrAdmin(session)) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        int productId = CellUtil.parseInt(request.getParameter("id"));
        if (productId > 0) {
            Product product = productDAO.findById(productId);
            if (product == null || !SellerAccess.canManage(product, session)) {
                response.sendRedirect(request.getContextPath() + "/seller/products");
                return;
            }
            request.setAttribute("product", product);
        }

        request.setAttribute("categories", VALID_CATEGORIES);
        request.getRequestDispatcher("/seller/product-form.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (!SellerAccess.isSellerOrAdmin(session)) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        String action = request.getParameter("action");
        if ("delete".equals(action)) {
            handleDelete(request, response, session);
            return;
        }
        handleSave(request, response, session);
    }

    private void handleDelete(HttpServletRequest request, HttpServletResponse response,
                              HttpSession session) throws IOException {
        int productId = CellUtil.parseInt(request.getParameter("id"));
        Product product = productDAO.findById(productId);
        if (product != null && SellerAccess.canManage(product, session)) {
            productDAO.delete(productId);
            response.sendRedirect(request.getContextPath() + "/seller/products?msg=deleted");
        } else {
            response.sendRedirect(request.getContextPath() + "/seller/products");
        }
    }

    private void handleSave(HttpServletRequest request, HttpServletResponse response,
                            HttpSession session) throws IOException {
        int productId = CellUtil.parseInt(request.getParameter("id"));
        String name = trim(request.getParameter("name"));
        String category = trim(request.getParameter("category"));
        String description = trim(request.getParameter("description"));
        String image = trim(request.getParameter("image"));
        double price = CellUtil.parseDouble(request.getParameter("price"));
        int stock = CellUtil.parseInt(request.getParameter("stock"));

        // Server-side validation (mirrors product-form.jsp).
        if (name.length() < 3 || name.length() > 100) {
            redirectMsg(request, response, productId, "invalid-name");
            return;
        }
        if (!VALID_CATEGORIES.contains(category)) {
            redirectMsg(request, response, productId, "invalid-category");
            return;
        }
        if (description.length() < 5 || description.length() > 1000) {
            redirectMsg(request, response, productId, "invalid-description");
            return;
        }
        if (price <= 0 || price > 1000000) {
            redirectMsg(request, response, productId, "invalid-price");
            return;
        }
        if (stock < 0 || stock > 10000) {
            redirectMsg(request, response, productId, "invalid-stock");
            return;
        }
        if (!image.isBlank() && !image.matches("^images/products/[a-zA-Z0-9._-]+\\.(svg|jpe?g|png|gif|webp)$")
                && !image.startsWith("https://") && !image.startsWith("http://")) {
            redirectMsg(request, response, productId, "invalid-image");
            return;
        }

        // Editing an existing product?
        Product existing = productId > 0 ? productDAO.findById(productId) : null;
        if (existing != null && !SellerAccess.canManage(existing, session)) {
            response.sendRedirect(request.getContextPath() + "/seller/products");
            return;
        }

        Product product = existing != null ? existing : new Product();
        product.setName(name);
        product.setCategory(category);
        product.setDescription(description);
        product.setPrice(price);
        product.setStock(stock);
        product.setImage(image.isBlank() ? ProductDAO.DEFAULT_IMAGE : image);

        if (existing != null) {
            productDAO.update(product);
            response.sendRedirect(request.getContextPath() + "/seller/products?msg=updated");
        } else {
            product.setSellerId((Integer) session.getAttribute("user_id"));
            productDAO.add(product);
            response.sendRedirect(request.getContextPath() + "/seller/products?msg=added");
        }
    }

    private void redirectMsg(HttpServletRequest request, HttpServletResponse response,
                             int productId, String msg) throws IOException {
        if (productId > 0) {
            response.sendRedirect(request.getContextPath() + "/seller/product?id=" + productId + "&msg=" + msg);
        } else {
            response.sendRedirect(request.getContextPath() + "/seller/product?msg=" + msg);
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
