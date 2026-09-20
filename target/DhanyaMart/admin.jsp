<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.dhanyamart.model.Order, com.dhanyamart.model.Product, com.dhanyamart.model.User, java.util.List" %>
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
    if (!"ADMIN".equals(session.getAttribute("role"))) {
        response.sendRedirect("login.jsp");
        return;
    }
    if (request.getAttribute("tab") == null) {
        response.sendRedirect("admin");
        return;
    }
    String tab = (String) request.getAttribute("tab");
    if (tab == null) tab = "overview";
    Integer currentUserId = (Integer) session.getAttribute("user_id");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin Dashboard - DhanyaMart</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body class="app-body">
<div class="container page">
    <%@ include file="_nav.jsp" %>

    <h1>Admin Dashboard</h1>
    <p class="subtitle">Manage users, products and orders across the store.</p>

    <div class="tabs">
        <a href="admin?tab=overview" class="tab <%= "overview".equals(tab) ? "active" : "" %>">Overview</a>
        <a href="admin?tab=users" class="tab <%= "users".equals(tab) ? "active" : "" %>">Users</a>
        <a href="admin?tab=products" class="tab <%= "products".equals(tab) ? "active" : "" %>">Products</a>
        <a href="admin?tab=orders" class="tab <%= "orders".equals(tab) ? "active" : "" %>">Orders</a>
    </div>

<% if ("users".equals(tab)) {
    List<User> users = (List<User>) request.getAttribute("users"); %>
    <h2>Users</h2>
    <table class="table">
        <thead>
            <tr>
                <th>ID</th><th>Name</th><th>Email</th><th>Phone</th><th>Address</th><th>Role</th>
            </tr>
        </thead>
        <tbody>
        <% for (User u : users) { %>
            <tr>
                <td><%= u.getUserId() %></td>
                <td><%= esc(u.getName()) %></td>
                <td><%= esc(u.getEmail()) %></td>
                <td><%= esc(u.getPhone()) %></td>
                <td><%= esc(u.getAddress()) %></td>
                <td>
                    <% if (u.getUserId() == currentUserId) { %>
                        <span class="badge badge-info"><%= esc(u.getRole()) %></span>
                    <% } else { %>
                        <form action="admin" method="post" class="inline-form">
                            <input type="hidden" name="action" value="updateRole">
                            <input type="hidden" name="userId" value="<%= u.getUserId() %>">
                            <select name="role" class="search-select" onchange="this.form.submit()">
                                <option value="CUSTOMER" <%= "CUSTOMER".equals(u.getRole()) ? "selected" : "" %>>CUSTOMER</option>
                                <option value="SELLER" <%= "SELLER".equals(u.getRole()) ? "selected" : "" %>>SELLER</option>
                                <option value="ADMIN" <%= "ADMIN".equals(u.getRole()) ? "selected" : "" %>>ADMIN</option>
                            </select>
                        </form>
                    <% } %>
                </td>
            </tr>
        <% } %>
        </tbody>
    </table>

<% } else if ("products".equals(tab)) {
    List<Product> products = (List<Product>) request.getAttribute("products"); %>
    <h2>Products</h2>
    <table class="table">
        <thead>
            <tr><th>Image</th><th>Product</th><th>Category</th><th>Price</th><th>Stock</th><th>Actions</th></tr>
        </thead>
        <tbody>
        <% for (Product p : products) { %>
            <tr>
                <td><img src="<%= esc(p.getImage()) %>" alt="<%= esc(p.getName()) %>" class="thumb"></td>
                <td><a class="product-name" href="product?id=<%= p.getProductId() %>"><%= esc(p.getName()) %></a>
                    <br><span class="muted">Seller ID <%= p.getSellerId() %></span></td>
                <td><%= esc(p.getCategory()) %></td>
                <td>&#8377;<%= p.getPriceText() %></td>
                <td><%= p.getStock() %></td>
                <td class="cell-actions">
                    <a href="seller/product?id=<%= p.getProductId() %>" class="btn btn-small">Edit</a>
                    <form action="admin" method="post" class="inline-form"
                          onsubmit="return confirm('Delete this product?');">
                        <input type="hidden" name="action" value="deleteProduct">
                        <input type="hidden" name="id" value="<%= p.getProductId() %>">
                        <button type="submit" class="btn btn-small btn-danger">Delete</button>
                    </form>
                </td>
            </tr>
        <% } %>
        </tbody>
    </table>

<% } else if ("orders".equals(tab)) {
    List<Order> orders = (List<Order>) request.getAttribute("orders"); %>
    <h2>Orders</h2>
    <table class="table">
        <thead>
            <tr><th>Order</th><th>Customer</th><th>Date</th><th>Items</th><th>Total</th><th>Status</th></tr>
        </thead>
        <tbody>
        <% for (Order o : orders) { %>
            <tr>
                <td><a href="order-confirm?id=<%= o.getOrderId() %>" class="product-name">#<%= o.getOrderId() %></a></td>
                <td><%= esc(o.getUserName()) %><br><span class="muted"><%= esc(o.getUserEmail()) %></span></td>
                <td><%= esc(o.getOrderDate()) %></td>
                <td><%= o.getItems().size() %></td>
                <td>&#8377;<%= o.getTotalText() %></td>
                <td>
                    <form action="admin" method="post" class="inline-form">
                        <input type="hidden" name="action" value="updateStatus">
                        <input type="hidden" name="orderId" value="<%= o.getOrderId() %>">
                        <select name="status" class="search-select" onchange="this.form.submit()">
                            <option value="PLACED" <%= "PLACED".equals(o.getStatus()) ? "selected" : "" %>>PLACED</option>
                            <option value="PACKED" <%= "PACKED".equals(o.getStatus()) ? "selected" : "" %>>PACKED</option>
                            <option value="SHIPPED" <%= "SHIPPED".equals(o.getStatus()) ? "selected" : "" %>>SHIPPED</option>
                            <option value="DELIVERED" <%= "DELIVERED".equals(o.getStatus()) ? "selected" : "" %>>DELIVERED</option>
                            <option value="CANCELLED" <%= "CANCELLED".equals(o.getStatus()) ? "selected" : "" %>>CANCELLED</option>
                        </select>
                    </form>
                </td>
            </tr>
        <% } %>
        </tbody>
    </table>

<% } else { %>

    <h2>Overview</h2>
    <div class="grid grid-3 stat-grid">
        <div class="card stat"><span class="stat-number"><%= request.getAttribute("userCount") %></span> Total Users</div>
        <div class="card stat"><span class="stat-number"><%= request.getAttribute("sellerCount") %></span> Sellers</div>
        <div class="card stat"><span class="stat-number"><%= request.getAttribute("productCount") %></span> Products</div>
        <div class="card stat"><span class="stat-number"><%= request.getAttribute("orderCount") %></span> Orders</div>
        <div class="card stat"><span class="stat-number">&#8377;<%= request.getAttribute("revenue") %></span> Revenue (excl. cancelled)</div>
    </div>

    <p class="switch">
        <a href="admin?tab=users">Manage users &rarr;</a> &middot;
        <a href="admin?tab=products">Manage products &rarr;</a> &middot;
        <a href="admin?tab=orders">Manage orders &rarr;</a>
    </p>

<% } %>

    <%@ include file="_footer.jsp" %>
</div>
</body>
</html>