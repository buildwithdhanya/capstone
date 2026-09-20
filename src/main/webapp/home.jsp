<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%!
    // Escapes HTML so any user-typed text is shown safely (prevents XSS)
    private String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    .replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    // ---------- PROTECTED PAGE CHECK ----------
    Integer userId = (Integer) session.getAttribute("user_id");
    if (userId == null) {
        response.sendRedirect("login.jsp");
        return;
    }
    String name = (String) session.getAttribute("name");
    String role = (String) session.getAttribute("role");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Home - DhanyaMart</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body class="app-body">
<div class="container page">
    <%@ include file="_nav.jsp" %>

    <h1>Welcome, <%= esc(name) %>!</h1>
    <p class="subtitle">Handcrafted crafts, handloom textiles and ethnic decor, straight from local artisans.</p>

    <div class="card">
        <p><strong>Account:</strong> <%= esc(name) %> (<%= esc(name != null ? role : "") %>)</p>
        <p><strong>Email:</strong> <%= session.getAttribute("email") %></p>
        <p><strong>Member since:</strong> logged in with ID <%= userId %></p>
    </div>

    <div class="grid grid-3">
        <a href="products" class="card link-card">
            <h3>Browse Products</h3>
            <p>Explore the full catalogue of crafts, handlooms, pottery and more.</p>
        </a>
        <a href="cart" class="card link-card">
            <h3>Your Cart</h3>
            <p>Review items, change quantities or remove products before checkout.</p>
        </a>
        <a href="orders" class="card link-card">
            <h3>My Orders</h3>
            <p>Track placed orders and their current status.</p>
        </a>
        <% if ("SELLER".equals(role) || "ADMIN".equals(role)) { %>
        <a href="seller/products" class="card link-card">
            <h3>Seller Dashboard</h3>
            <p>Add, edit and manage the products you sell.</p>
        </a>
        <% } %>
        <% if ("ADMIN".equals(role)) { %>
        <a href="admin" class="card link-card">
            <h3>Admin Dashboard</h3>
            <p>Manage users, products and orders across the store.</p>
        </a>
        <% } %>
    </div>

    <p class="switch"><a href="products">Start shopping &rarr;</a></p>
</div>
</body>
</html>