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
    <title>Register - DhanyaMart</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body class="auth-body">
<div class="auth-shell">

    <aside class="auth-showcase">
        <div class="auth-brand">
            <span class="auth-brand-badge">D</span>
            DhanyaMart
        </div>
        <h1>Join the artisan marketplace.</h1>
        <p>Browse handlooms, folk art, pottery and more &mdash; every purchase supports a local craftsperson.</p>
        <div class="auth-art">
            <img src="images/products/art.jpg" alt="Folk art" loading="lazy">
            <img src="images/products/pottery.jpg" alt="Pottery" loading="lazy">
            <img src="images/products/jewellery.jpg" alt="Temple jewellery" loading="lazy">
        </div>
        <ul class="auth-points">
            <li>Create one account for shopping &amp; orders</li>
            <li>Rate products and help fellow buyers</li>
            <li>Cash on delivery across all regions</li>
        </ul>
    </aside>

    <main class="auth-panel">
        <div class="auth-card container-wide">
            <div class="brand-mark">D</div>
            <h1>Create your account</h1>
            <p class="subtitle">A few details and you are ready to shop.</p>

            <%
                String error = (String) request.getAttribute("error");
                String name = request.getAttribute("name") != null
                        ? (String) request.getAttribute("name") : "";
                String email = request.getAttribute("email") != null
                        ? (String) request.getAttribute("email") : "";
                String phone = request.getAttribute("phone") != null
                        ? (String) request.getAttribute("phone") : "";
                String address = request.getAttribute("address") != null
                        ? (String) request.getAttribute("address") : "";
            %>

            <% if (error != null) { %>
                <div class="alert alert-error"><%= esc(error) %></div>
            <% } %>

            <form action="register" method="post" onsubmit="return validateRegisterForm()">
                <div class="form-group">
                    <label for="name">Full Name</label>
                    <input type="text" id="name" name="name" value="<%= esc(name) %>"
                           placeholder="Your full name" autofocus>
                </div>
                <div class="form-group">
                    <label for="email">Email</label>
                    <input type="email" id="email" name="email" value="<%= esc(email) %>"
                           placeholder="you@example.com">
                </div>
                <div class="form-group">
                    <label for="phone">Phone</label>
                    <input type="tel" id="phone" name="phone" value="<%= esc(phone) %>"
                           placeholder="10-digit mobile number" maxlength="10">
                </div>
                <div class="form-row-2">
                    <div class="form-group">
                        <label for="password">Password</label>
                        <input type="password" id="password" name="password"
                               placeholder="At least 6 chars">
                    </div>
                    <div class="form-group">
                        <label for="confirmPassword">Confirm</label>
                        <input type="password" id="confirmPassword" name="confirmPassword"
                               placeholder="Re-enter password">
                    </div>
                </div>
                <div class="form-group">
                    <label for="address">Address</label>
                    <textarea id="address" name="address" rows="3"
                              placeholder="Delivery address"><%= esc(address) %></textarea>
                </div>
                <button type="submit" class="btn">Create Account</button>
            </form>

            <p class="switch">Already have an account? <a href="login.jsp">Login</a></p>
        </div>
    </main>

</div>

<script>
    function validateRegisterForm() {
        var name = document.getElementById("name").value.trim();
        var email = document.getElementById("email").value.trim();
        var phone = document.getElementById("phone").value.trim();
        var password = document.getElementById("password").value;
        var confirmPassword = document.getElementById("confirmPassword").value;
        var address = document.getElementById("address").value.trim();

        var emailPattern = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;
        var phonePattern = /^\d{10}$/;

        if (name.length < 3)                 { alert("Please enter your full name."); return false; }
        if (!emailPattern.test(email))       { alert("Please enter a valid email address."); return false; }
        if (!phonePattern.test(phone))       { alert("Please enter a valid 10-digit phone number."); return false; }
        if (password.length < 6)             { alert("Password must be at least 6 characters."); return false; }
        if (password !== confirmPassword)    { alert("Passwords do not match."); return false; }
        if (address.length < 5)              { alert("Please enter your address."); return false; }
        return true;
    }
</script>
</body>
</html>