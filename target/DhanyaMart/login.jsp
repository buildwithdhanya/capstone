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
<body class="auth-body">
<div class="auth-shell">

    <aside class="auth-showcase">
        <div class="auth-brand">
            <span class="auth-brand-badge">D</span>
            DhanyaMart
        </div>
        <h1>Crafts with a story &amp; a soul.</h1>
        <p>Handloom textiles, folk paintings, pottery, wood and brass &mdash; made by Indian artisans, delivered to your doorstep.</p>
        <div class="auth-art">
            <img src="images/products/kalamkari.jpg" alt="Block-printed fabric" loading="lazy">
            <img src="images/products/diya.jpg" alt="Clay diyas" loading="lazy">
            <img src="images/products/brass.jpg" alt="Brass lamp" loading="lazy">
        </div>
        <ul class="auth-points">
            <li>Authentic handloom &amp; handicraft catalogue</li>
            <li>Fair prices, straight from the artisans</li>
            <li>Cash on delivery across all regions</li>
        </ul>
    </aside>

    <main class="auth-panel">
        <div class="auth-card">
            <div class="brand-mark">D</div>
            <h1>Welcome back</h1>
            <p class="subtitle">Sign in to continue shopping.</p>

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

            <div class="demo-box">
                <p>Try a demo account</p>
                <div class="demo-row">
                    <button type="button" class="demo-chip"
                            onclick="fillDemo('demo@dhanyamart.com','Demo@123')">Customer</button>
                    <button type="button" class="demo-chip"
                            onclick="fillDemo('seller@dhanyamart.com','Seller@123')">Seller</button>
                    <button type="button" class="demo-chip"
                            onclick="fillDemo('admin@dhanyamart.com','Admin@123')">Admin</button>
                </div>
            </div>

            <p class="switch">New here? <a href="register.jsp">Create an account</a></p>
        </div>
    </main>

</div>

<script>
    function fillDemo(userEmail, userPassword) {
        document.getElementById("email").value = userEmail;
        document.getElementById("password").value = userPassword;
    }
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