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
    // No valid session -> redirect to login. This must happen BEFORE any
    // HTML is printed, so an unauthenticated user can never see this page.
    Integer userId = (Integer) session.getAttribute("user_id");
    if (userId == null) {
        response.sendRedirect("login.jsp");
        return;
    }
    String name = (String) session.getAttribute("name");
    String email = (String) session.getAttribute("email");
    // ------------------------------------------
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Home - DhanyaMart</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
<div class="container container-wide">
    <nav class="topbar">
        <span class="brand">DhanyaMart</span>
        <a href="logout" class="btn btn-small">Logout</a>
    </nav>

    <h1>Welcome, <%= esc(name) %>!</h1>
    <p class="subtitle">You have successfully logged in. Products, cart and orders
        will be added here in the next modules.</p>

    <div class="card">
        <p><strong>User ID:</strong> <%= userId %></p>
        <p><strong>Name:</strong> <%= esc(name) %></p>
        <p><strong>Email:</strong> <%= esc(email) %></p>
        <p><strong>Session ID:</strong> <%= session.getId() %></p>
    </div>

    <p class="switch">Done? <a href="logout">Logout</a></p>
</div>
</body>
</html>
