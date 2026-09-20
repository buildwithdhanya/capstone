<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.dhanyamart.model.Order, com.dhanyamart.model.OrderItem" %>
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
    Order order = (Order) request.getAttribute("order");
    if (order == null) {
        response.sendRedirect("orders");
        return;
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Order Confirmed - DhanyaMart</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body class="app-body">
<div class="container page">
    <%@ include file="_nav.jsp" %>

    <div class="alert alert-success">
        <strong>Thank you! Your order has been placed successfully.</strong>
    </div>

    <h1>Order #<%= order.getOrderId() %> Confirmed</h1>
    <p class="subtitle">Placed on <%= esc(order.getOrderDate()) %>
        &middot; Status: <span class="badge badge-stock"><%= esc(order.getStatus()) %></span></p>

    <div class="detail-row">
        <div class="card">
            <h3>Delivery Details</h3>
            <p><strong>Name:</strong> <%= esc(order.getUserName()) %></p>
            <p><strong>Phone:</strong> <%= esc(order.getPhone()) %></p>
            <p><strong>Address:</strong> <%= esc(order.getAddress()) %></p>
        </div>

        <div class="card order-summary">
            <h3>Order Summary</h3>
            <% for (OrderItem item : order.getItems()) { %>
                <p class="summary-line">
                    <%= esc(item.getProductName()) %>
                    <span class="muted">x<%= item.getQuantity() %></span>
                    <span class="right-muted">&#8377;<%= String.format("%,.2f", item.getSubtotal()) %></span>
                </p>
            <% } %>
            <hr>
            <p class="summary-total">
                <strong>Total</strong>
                <strong>&#8377;<%= order.getTotalText() %></strong>
            </p>
            <p class="muted">Cash on delivery. Track the status under My Orders.</p>
        </div>
    </div>

    <div class="action-row">
        <a href="orders" class="btn btn-small">My Orders</a>
        <a href="products" class="btn btn-small btn-ghost">Continue Shopping</a>
    </div>
</div>
</body>
</html>