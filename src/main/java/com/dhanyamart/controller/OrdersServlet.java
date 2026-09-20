package com.dhanyamart.controller;

import com.dhanyamart.dao.OrderDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * My Orders.
 *   GET /orders -> every order of the logged-in customer, newest first,
 *                  including the current order status.
 */
@WebServlet("/orders")
public class OrdersServlet extends HttpServlet {

    private final OrderDAO orderDAO = new OrderDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user_id") == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        int userId = (Integer) session.getAttribute("user_id");
        request.setAttribute("orders", orderDAO.listByUser(userId));
        request.getRequestDispatcher("orders.jsp").forward(request, response);
    }
}