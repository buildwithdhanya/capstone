package com.dhanyamart.controller;

import com.dhanyamart.dao.UserDAO;
import com.dhanyamart.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Handles Login.
 *   GET  /login  -> shows login.jsp (or redirects to home if already logged in)
 *   POST /login  -> verifies email + password against users.xlsx,
 *                   creates an HttpSession and redirects to home.jsp
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Already logged in? Go straight to the protected page.
        HttpSession existing = request.getSession(false);
        if (existing != null && existing.getAttribute("user_id") != null) {
            response.sendRedirect("home.jsp");
            return;
        }
        request.getRequestDispatcher("login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String email = trim(request.getParameter("email"));
        String password = request.getParameter("password");

        if (email.isEmpty() || password == null || password.isEmpty()) {
            request.setAttribute("error", "Please enter your email and password.");
            request.setAttribute("email", email);
            request.getRequestDispatcher("login.jsp").forward(request, response);
            return;
        }

        User user = userDAO.login(email, password);

        if (user == null) {
            // Same message for "email not found" and "wrong password"
            // so attackers cannot tell which accounts exist.
            request.setAttribute("error", "Invalid email or password.");
            request.setAttribute("email", email);
            request.getRequestDispatcher("login.jsp").forward(request, response);
            return;
        }

        // Login OK -> create a fresh session and store the logged-in user
        HttpSession session = request.getSession(true);
        request.changeSessionId();          // prevent session-fixation attacks
        session.setAttribute("user_id", user.getUserId());
        session.setAttribute("name", user.getName());
        session.setAttribute("email", user.getEmail());

        response.sendRedirect("home.jsp");
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
