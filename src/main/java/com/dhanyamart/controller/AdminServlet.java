package com.dhanyamart.controller;

import com.dhanyamart.dao.OrderDAO;
import com.dhanyamart.dao.ProductDAO;
import com.dhanyamart.dao.UserDAO;
import com.dhanyamart.model.Product;
import com.dhanyamart.util.CellUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

/**
 * Admin dashboard.
 *   GET /admin?tab=overview|users|products|orders
 *   POST /admin action=updateRole    (userId, role)
 *   POST /admin action=updateStatus  (orderId, status)
 *   POST /admin action=deleteProduct (id)
 *   POST /admin action=deleteUser    (userId)
 *   POST /admin action=deleteOrder   (orderId)
 *
 * Only the ADMIN role may access this servlet.
 */
@WebServlet("/admin")
public class AdminServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final OrderDAO orderDAO = new OrderDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (!SellerAccess.isAdmin(session)) {
            response.sendRedirect("login.jsp");
            return;
        }

        String tab = request.getParameter("tab");
        loadData(request, tab == null ? "overview" : tab);
        request.getRequestDispatcher("admin.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (!SellerAccess.isAdmin(session)) {
            response.sendRedirect("login.jsp");
            return;
        }

        String action = request.getParameter("action");
        if ("updateRole".equals(action)) {
            int userId = CellUtil.parseInt(request.getParameter("userId"));
            int me = (Integer) session.getAttribute("user_id");
            // Never let an admin demote the currently logged-in account.
            if (userId != me) {
                userDAO.updateRole(userId, request.getParameter("role"));
            }
            response.sendRedirect("admin?tab=users");
            return;
        }
        if ("updateStatus".equals(action)) {
            int orderId = CellUtil.parseInt(request.getParameter("orderId"));
            orderDAO.updateStatus(orderId, request.getParameter("status"));
            response.sendRedirect("admin?tab=orders");
            return;
        }
        if ("deleteProduct".equals(action)) {
            int productId = CellUtil.parseInt(request.getParameter("id"));
            productDAO.delete(productId);
            response.sendRedirect("admin?tab=products");
            return;
        }
        if ("deleteUser".equals(action)) {
            int userId = CellUtil.parseInt(request.getParameter("userId"));
            int me = (Integer) session.getAttribute("user_id");
            // Never let an admin delete the currently logged-in account.
            if (userId != me && userDAO.delete(userId)) {
                response.sendRedirect("admin?tab=users&msg=user-deleted");
            } else {
                response.sendRedirect("admin?tab=users&msg=delete-failed");
            }
            return;
        }
        if ("deleteOrder".equals(action)) {
            int orderId = CellUtil.parseInt(request.getParameter("orderId"));
            if (orderDAO.delete(orderId)) {
                response.sendRedirect("admin?tab=orders&msg=order-deleted");
            } else {
                response.sendRedirect("admin?tab=orders&msg=delete-failed");
            }
            return;
        }
        response.sendRedirect("admin");
    }

    private void loadData(HttpServletRequest request, String tab) {
        request.setAttribute("tab", tab);

        if ("users".equals(tab)) {
            request.setAttribute("users", userDAO.listAll());
        } else if ("products".equals(tab)) {
            request.setAttribute("products", productDAO.listAll());
        } else if ("orders".equals(tab)) {
            request.setAttribute("orders", orderDAO.listAll());
        } else {
            List<Product> all = productDAO.listAll();
            int sellerCount = 0;
            int revenue = (int) Math.round(orderDAO.totalRevenue());
            for (com.dhanyamart.model.User u : userDAO.listAll()) {
                if ("SELLER".equals(u.getRole())) sellerCount++;
            }
            request.setAttribute("userCount", userDAO.listAll().size());
            request.setAttribute("sellerCount", sellerCount);
            request.setAttribute("productCount", all.size());
            request.setAttribute("orderCount", orderDAO.listAll().size());
            request.setAttribute("revenue", revenue);
        }
    }
}