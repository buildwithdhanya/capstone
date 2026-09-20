package com.dhanyamart.controller;

import com.dhanyamart.dao.OrderDAO;
import com.dhanyamart.model.Order;
import com.dhanyamart.util.CellUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Order confirmation page shown right after checkout.
 *   GET /order-confirm?id=501 -> displays the placed order
 * Only the owner of the order can view it.
 */
@WebServlet("/order-confirm")
public class OrderConfirmationServlet extends HttpServlet {

    private final OrderDAO orderDAO = new OrderDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user_id") == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        int orderId = CellUtil.parseInt(request.getParameter("id"));
        Order order = orderDAO.findById(orderId);
        boolean owner = order != null && order.getUserId() == (Integer) session.getAttribute("user_id");
        boolean admin = "ADMIN".equals(session.getAttribute("role"));
        if (order == null || (!owner && !admin)) {
            response.sendRedirect("orders");
            return;
        }

        request.setAttribute("order", order);
        request.getRequestDispatcher("order-confirm.jsp").forward(request, response);
    }
}