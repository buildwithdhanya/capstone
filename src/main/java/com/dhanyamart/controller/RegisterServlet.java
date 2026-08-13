package com.dhanyamart.controller;

import com.dhanyamart.dao.UserDAO;
import com.dhanyamart.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Handles New User Registration.
 *   GET  /register  -> shows register.jsp
 *   POST /register  -> validates input, checks duplicate email,
 *                      hashes the password and saves the user to users.xlsx
 */
@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String name = trim(request.getParameter("name"));
        String email = trim(request.getParameter("email"));
        String phone = trim(request.getParameter("phone"));
        String address = trim(request.getParameter("address"));
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");

        // 1) Field-level validation
        String error = validate(name, email, phone, address, password, confirmPassword);

        // 2) Duplicate email check
        if (error == null && userDAO.emailExists(email)) {
            error = "An account with this email already exists. Please login instead.";
        }

        // 3) On error: send the user back to the form with a message
        //    (form fields are re-filled so nothing is lost)
        if (error != null) {
            request.setAttribute("error", error);
            request.setAttribute("name", name);
            request.setAttribute("email", email);
            request.setAttribute("phone", phone);
            request.setAttribute("address", address);
            request.getRequestDispatcher("register.jsp").forward(request, response);
            return;
        }

        // 4) Build the User and save (UserDAO hashes the password)
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setAddress(address);
        user.setPassword(password);

        userDAO.registerUser(user);

        // 5) Success -> go to login page with a happy message
        response.sendRedirect("login.jsp?registered=1");
    }

    /** Server-side validation. Returns an error message or null if all good. */
    private String validate(String name, String email, String phone, String address,
                            String password, String confirmPassword) {
        if (name == null || name.length() < 3) {
            return "Please enter your full name (at least 3 characters).";
        }
        if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            return "Please enter a valid email address.";
        }
        if (phone == null || !phone.matches("\\d{10}")) {
            return "Please enter a valid 10-digit phone number.";
        }
        if (address == null || address.length() < 5) {
            return "Please enter your address (at least 5 characters).";
        }
        if (password == null || password.length() < 6) {
            return "Password must be at least 6 characters long.";
        }
        if (confirmPassword == null || !confirmPassword.equals(password)) {
            return "Password and Confirm Password do not match.";
        }
        return null;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
