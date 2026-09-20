<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.dhanyamart.dao.ProductDAO, com.dhanyamart.model.Product, com.dhanyamart.model.OrderItem, java.util.ArrayList, java.util.List, java.util.Map" %>
<%!
    private String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    .replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    if (session.getAttribute("user_id") == null) {
        response.sendRedirect("login.jsp");
        return;
    }
    Object cartObj = session.getAttribute("cart");
    java.util.Map<Integer, Integer> cart =
            cartObj instanceof Map ? (Map<Integer, Integer>) cartObj : null;
    if (cart == null || cart.isEmpty()) {
        response.sendRedirect("cart");
        return;
    }

    // Order summary resolved live (works on GET and on validation errors).
    ProductDAO productDAO = new ProductDAO();
    List<OrderItem> reviewItems = new ArrayList<>();
    double summaryTotal = 0.0;
    for (Map.Entry<Integer, Integer> e : cart.entrySet()) {
        Product p = productDAO.findById(e.getKey());
        if (p == null) continue;
        OrderItem oi = new OrderItem(p.getProductId(), p.getName(), p.getPrice(), e.getValue());
        reviewItems.add(oi);
        summaryTotal += oi.getSubtotal();
    }

    String error = (String) request.getAttribute("error");
    String name = request.getAttribute("name") != null ? (String) request.getAttribute("name") : "";
    String phone = request.getAttribute("phone") != null ? (String) request.getAttribute("phone") : "";
    String address = request.getAttribute("address") != null ? (String) request.getAttribute("address") : "";
    if (name.isEmpty()) name = (String) session.getAttribute("name");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Checkout - DhanyaMart</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body class="app-body">
<div class="container page">
    <%@ include file="_nav.jsp" %>

    <h1>Checkout</h1>
    <p class="subtitle">Confirm delivery details and place your order.</p>

    <% if (error != null) { %>
        <div class="alert alert-error"><%= esc(error) %></div>
    <% } %>

    <div class="detail-row">
        <form action="checkout" method="post" class="checkout-form"
              onsubmit="return validateCheckoutForm()">
            <div class="form-group">
                <label for="name">Full Name</label>
                <input type="text" id="name" name="name" value="<%= esc(name) %>">
            </div>
            <div class="form-group">
                <label for="phone">Mobile Number</label>
                <input type="tel" id="phone" name="phone" value="<%= esc(phone) %>"
                       maxlength="10" placeholder="10-digit mobile number">
            </div>
            <div class="form-group">
                <label for="address">Delivery Address</label>
                <textarea id="address" name="address" rows="4"><%= esc(address) %></textarea>
            </div>
            <button type="submit" class="btn">Place Order</button>
        </form>

        <div class="card order-summary">
            <h3>Order Summary</h3>
            <% for (OrderItem item : reviewItems) { %>
                <p class="summary-line">
                    <%= esc(item.getProductName()) %>
                    <span class="muted">x<%= item.getQuantity() %></span>
                    <span class="right-muted">&#8377;<%= String.format("%,.2f", item.getSubtotal()) %></span>
                </p>
            <% } %>
            <hr>
            <p class="summary-total">
                <strong>Total</strong>
                <strong>&#8377;<%= String.format("%,.2f", summaryTotal) %></strong>
            </p>
            <p class="muted">Cash on delivery only.</p>
        </div>
    </div>
    <p class="switch"><a href="cart">&larr; Back to cart</a></p>

    <%@ include file="_footer.jsp" %>
</div>

<script>
    function validateCheckoutForm() {
        var name = document.getElementById("name").value.trim();
        var phone = document.getElementById("phone").value.trim();
        var address = document.getElementById("address").value.trim();
        if (name.length < 3) { alert("Please enter your full name."); return false; }
        if (!/^\d{10}$/.test(phone)) { alert("Please enter a valid 10-digit mobile number."); return false; }
        if (address.length < 5) { alert("Please enter your delivery address."); return false; }
        return true;
    }
</script>
</body>
</html>