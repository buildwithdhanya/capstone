<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.dhanyamart.model.Order, com.dhanyamart.model.OrderItem, java.util.List" %>
<%!
    private String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    .replace("\"", "&quot;").replace("'", "&#39;");
    }
    private String statusClass(String status) {
        if ("DELIVERED".equals(status)) return "badge-success";
        if ("CANCELLED".equals(status)) return "badge-danger";
        if ("SHIPPED".equals(status)) return "badge-info";
        return "badge-warn";
    }
%>
<%
    if (session.getAttribute("user_id") == null) {
        response.sendRedirect("login.jsp");
        return;
    }
    if (request.getAttribute("orders") == null) {
        response.sendRedirect("orders");
        return;
    }
    List<Order> orders = (List<Order>) request.getAttribute("orders");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My Orders - DhanyaMart</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body class="app-body">
<div class="container page">
    <%@ include file="_nav.jsp" %>

    <h1>My Orders</h1>
    <p class="subtitle">Track every order you have placed and its current status.</p>

    <% if (orders == null || orders.isEmpty()) { %>
        <div class="card empty-state">
            <p>You have not placed any orders yet.</p>
            <p class="switch"><a href="products">Browse products &rarr;</a></p>
        </div>
    <% } else { %>
        <% for (Order order : orders) { %>
            <div class="card order-card">
                <div class="order-head">
                    <div>
                        <strong>Order #<%= order.getOrderId() %></strong>
                        <span class="muted"> - <%= esc(order.getOrderDate()) %></span>
                    </div>
                    <div>
                        <span class="badge <%= statusClass(order.getStatus()) %>"><%= esc(order.getStatus()) %></span>
                        <strong>&#8377;<%= order.getTotalText() %></strong>
                    </div>
                </div>
                <ul class="order-items">
                    <% for (OrderItem item : order.getItems()) { %>
                        <li><%= esc(item.getProductName()) %> <span class="muted">x<%= item.getQuantity() %></span></li>
                    <% } %>
                </ul>
                <p class="switch">
                    <a href="order-confirm?id=<%= order.getOrderId() %>">View order details &rarr;</a>
                </p>
            </div>
        <% } %>
    <% } %>

    <%@ include file="_footer.jsp" %>
</div>
</body>
</html>