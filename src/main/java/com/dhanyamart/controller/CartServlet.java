package com.dhanyamart.controller;

import com.dhanyamart.dao.ProductDAO;
import com.dhanyamart.model.CartItem;
import com.dhanyamart.model.Product;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shopping cart, kept in the HttpSession as a LinkedHashMap of
 * productId(Integer) -> quantity(Integer).
 *
 *   GET  /cart                          -> shows the cart (cart.jsp)
 *   POST /cart  action=add    id, qty   -> adds a product
 *   POST /cart  action=update id, qty   -> changes quantity
 *   POST /cart  action=remove id        -> removes a product
 */
@WebServlet("/cart")
public class CartServlet extends HttpServlet {

    public static final String CART_ATTR = "cart";
    private static final int MAX_QTY = 99;

    private final ProductDAO productDAO = new ProductDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user_id") == null) {
            response.sendRedirect("login.jsp");
            return;
        }
        Map<Integer, Integer> cart = cart(session);
        List<CartItem> items = new ArrayList<>();
        double total = 0.0;
        StringBuilder notice = new StringBuilder();
        if (cart != null) {
            // Re-sync the cart with the live catalogue: drop items that no
            // longer exist / are out of stock and clamp over-bought quantities.
            List<Integer> toRemove = new ArrayList<>();
            for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
                Product product = productDAO.findById(entry.getKey());
                if (product == null) {
                    toRemove.add(entry.getKey());
                    continue;
                }
                if (product.getStock() <= 0) {
                    toRemove.add(entry.getKey());
                    continue;
                }
                if (entry.getValue() > product.getStock()) {
                    int oldQty = entry.getValue();
                    cart.put(entry.getKey(), product.getStock());
                    notice.append("<span class=\"meta\">").append(esc(product.getName()))
                            .append(" - only ").append(product.getStock())
                            .append(" in stock. Quantity updated.</span><br>");
                    entry.setValue(product.getStock());
                }
            }
            for (Integer id : toRemove) {
                cart.remove(id);
            }
            for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
                Product product = productDAO.findById(entry.getKey());
                if (product == null) continue;
                CartItem item = new CartItem(product, entry.getValue());
                items.add(item);
                total += item.getSubtotal();
            }
        }
        if (notice.length() > 0) {
            request.setAttribute("cartNotice", notice.toString());
        }
        request.setAttribute("cartItems", items);
        request.setAttribute("cartTotal", total);
        request.getRequestDispatcher("cart.jsp").forward(request, response);
    }

    /** Minimal HTML escaping for notice text inserted into cart.jsp. */
    private String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    .replace("\"", "&quot;").replace("'", "&#39;");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user_id") == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        String action = request.getParameter("action");
        int productId = parseInt(request.getParameter("id"));
        int qty = parseInt(request.getParameter("qty"));

        Map<Integer, Integer> cart = cart(session);
        if (cart == null) {
            cart = new LinkedHashMap<>();
            session.setAttribute(CART_ATTR, cart);
        }

        switch (action == null ? "" : action) {
            case "add":
                handleAdd(request, response, cart, productId, qty);
                return;
            case "update":
                handleUpdate(response, cart, productId, qty);
                return;
            case "remove":
                cart.remove(productId);
                break;
            default:
                break;
        }
        response.sendRedirect("cart");
    }

    private void handleAdd(HttpServletRequest request, HttpServletResponse response,
                            Map<Integer, Integer> cart, int productId, int qty)
            throws IOException {
        if (productId <= 0 || qty <= 0) {
            response.sendRedirect("products.jsp");
            return;
        }
        Product product = productDAO.findById(productId);
        if (product == null) {
            response.sendRedirect("products.jsp?msg=not-found");
            return;
        }
        if (product.getStock() <= 0) {
            response.sendRedirect("product?id=" + productId + "&msg=out-of-stock");
            return;
        }
        int current = cart.getOrDefault(productId, 0);
        int newQty = Math.min(MAX_QTY, current + qty);
        if (newQty > product.getStock()) {
            newQty = product.getStock(); // cannot order more than available
        }
        cart.put(productId, newQty);
        response.sendRedirect("cart");
    }

    private void handleUpdate(HttpServletResponse response, Map<Integer, Integer> cart,
                              int productId, int qty) throws IOException {
        if (qty <= 0) {
            cart.remove(productId);
        } else {
            Product product = productDAO.findById(productId);
            int limit = product == null ? MAX_QTY : product.getStock();
            cart.put(productId, Math.min(MAX_QTY, Math.min(qty, limit)));
        }
        response.sendRedirect("cart");
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, Integer> cart(HttpSession session) {
        return (Map<Integer, Integer>) session.getAttribute(CART_ATTR);
    }
}