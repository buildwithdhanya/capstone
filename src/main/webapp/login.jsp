<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%!
    // Escapes HTML so any user-typed text is shown safely (prevents XSS)
    private String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    .replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Login - DhanyaMart</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
<div class="container">
    <h1>DhanyaMart</h1>
    <p class="subtitle">Login to your account</p>

    <%
        String error = (String) request.getAttribute("error");
        String registered = request.getParameter("registered");
        String email = request.getAttribute("email") != null
                ? (String) request.getAttribute("email") : "";
    %>

    <% if (error != null) { %>
        <div class="alert alert-error"><%= esc(error) %></div>
    <% } %>

    <% if ("1".equals(registered)) { %>
        <div class="alert alert-success">Registration successful! Please login to continue.</div>
    <% } %>

    <form action="login" method="post" onsubmit="return validateLoginForm()">
        <div class="form-group">
            <label for="email">Email</label>
            <input type="email" id="email" name="email"
                   value="<%= esc(email) %>" placeholder="you@example.com" autofocus>
        </div>
        <div class="form-group">
            <label for="password">Password</label>
            <input type="password" id="password" name="password" placeholder="Your password">
        </div>
        <button type="submit" class="btn">Login</button>
    </form>

    <p class="switch">New here? <a href="register.jsp">Create an account</a></p>
</div>

<script>
    function validateLoginForm() {
        var email = document.getElementById("email").value.trim();
        var password = document.getElementById("password").value;
        if (email === "") { alert("Please enter your email."); return false; }
        if (password === "") { alert("Please enter your password."); return false; }
        return true;
    }
</script>
</body>
</html>
