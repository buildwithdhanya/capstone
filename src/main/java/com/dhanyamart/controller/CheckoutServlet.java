package com.dhanyamart.controller;

import com.dhanyamart.dao.OrderDAO;
import com.dhanyamart.dao.ProductDAO;
import com.dhanyamart.dao.UserDAO;
import com.dhanyamart.model.Order;
import com.dhanyamart.model.OrderItem;
import com.dhanyamart.model.Product;
import com.dhanyamart.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Checkout + Place Order.
 *   GET  /checkout -> shows the checkout form (cart must not be empty)
 *   POST /checkout -> validates delivery details, saves the order,
 *                     reduces stock, clears the cart and redirects to
 *                     the order confirmation page.
 */
@WebServlet("/checkout")
public class CheckoutServlet extends HttpServlet {

    private final ProductDAO productDAO = new ProductDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = requireSession(request, response);
        if (session == null) return;

        Map<Integer, Integer> cart = cart(request);
        if (cart == null || cart.isEmpty()) {
            response.sendRedirect("cart");
            return;
        }

        // Pre-fill the form with the user's saved phone/address.
        User user = userDAO.findById((Integer) session.getAttribute("user_id"));
        request.setAttribute("phone", user != null ? user.getPhone() : "");
        request.setAttribute("address", user != null ? user.getAddress() : "");
        request.setAttribute("name", session.getAttribute("name"));

        request.getRequestDispatcher("checkout.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = requireSession(request, response);
        if (session == null) return;

        Map<Integer, Integer> cart = cart(request);
        if (cart == null || cart.isEmpty()) {
            response.sendRedirect("cart");
            return;
        }

        String name = trim(request.getParameter("name"));
        String phone = trim(request.getParameter("phone"));
        String address = trim(request.getParameter("address"));

        // Server-side validation (mirrors checkout.jsp).
        if (name.length() < 3) {
            backToCheckout(request, response, name, phone, address, "Please enter your full name.");
            return;
        }
        if (!phone.matches("\\d{10}")) {
            backToCheckout(request, response, name, phone, address, "Please enter a valid 10-digit mobile number.");
            return;
        }
        if (address.length() < 5) {
            backToCheckout(request, response, name, phone, address, "Please enter your delivery address.");
            return;
        }

        int userId = (Integer) session.getAttribute("user_id");
        String email = (String) session.getAttribute("email");

        // Build order items from the cart, re-checking availability first.
        List<OrderItem> items = new ArrayList<>();
        double total = 0.0;
        for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
            Product product = productDAO.findById(entry.getKey());
            if (product == null || product.getStock() <= 0 || entry.getValue() <= 0) {
                response.sendRedirect("cart?msg=unavailable");
                return;
            }
            int qty = Math.min(entry.getValue(), product.getStock());
            String productName = product.getName().replace('|', ' ').replace(';', ' ');
            OrderItem item = new OrderItem(product.getProductId(), productName, product.getPrice(), qty);
            items.add(item);
            total += item.getSubtotal();
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setUserName(name);
        order.setUserEmail(email);
        order.setPhone(phone);
        order.setAddress(address);
        order.setTotal(total);
        order.setItems(items);

        int orderId = orderDAO.create(order);

        // Reduce stock for each ordered product.
        for (OrderItem item : items) {
            productDAO.decrementStock(item.getProductId(), item.getQuantity());
        }

        // Empty the cart -> go to the confirmation page.
        session.removeAttribute(CartServlet.CART_ATTR);
        response.sendRedirect("order-confirm?id=" + orderId);
    }

    private void backToCheckout(HttpServletRequest request, HttpServletResponse response,
                                String name, String phone, String address, String error)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        request.setAttribute("error", error);
        request.setAttribute("name", name);
        request.setAttribute("phone", phone);
        request.setAttribute("address", address);
        if (session != null) request.setAttribute("sessionName", session.getAttribute("name"));
        request.getRequestDispatcher("checkout.jsp").forward(request, response);
    }

    private HttpSession requireSession(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user_id") == null) {
            response.sendRedirect("login.jsp");
            return null;
        }
        return session;
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, Integer> cart(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (Map<Integer, Integer>) session.getAttribute(CartServlet.CART_ATTR);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}