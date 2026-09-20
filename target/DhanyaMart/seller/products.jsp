<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.dhanyamart.model.Product, java.util.List" %>
<%!
    private String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    .replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    if (!"SELLER".equals(session.getAttribute("role"))
            && !"ADMIN".equals(session.getAttribute("role"))) {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
        return;
    }
    if (request.getAttribute("products") == null) {
        response.sendRedirect(request.getContextPath() + "/seller/products");
        return;
    }
    List<Product> products = (List<Product>) request.getAttribute("products");
    String msg = request.getParameter("msg");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Seller Dashboard - DhanyaMart</title>
    <base href="<%= request.getContextPath() %>/">
    <link rel="stylesheet" href="css/style.css">
</head>
<body class="app-body">
<div class="container page">
    <%@ include file="/_nav.jsp" %>

    <h1>Seller Dashboard</h1>
    <p class="subtitle">Add, edit and manage the products you sell.</p>

    <% if ("added".equals(msg)) { %>
        <div class="alert alert-success">Product added successfully.</div>
    <% } else if ("updated".equals(msg)) { %>
        <div class="alert alert-success">Product updated successfully.</div>
    <% } else if ("deleted".equals(msg)) { %>
        <div class="alert alert-success">Product deleted.</div>
    <% } %>

    <div class="action-row">
        <a href="seller/product" class="btn btn-small">+ Add New Product</a>
        <a href="products" class="btn btn-small btn-ghost">View storefront</a>
    </div>

    <% if (products == null || products.isEmpty()) { %>
        <div class="card empty-state">
            <p>You have not added any products yet.</p>
        </div>
    <% } else { %>
        <table class="table">
            <thead>
                <tr>
                    <th>Image</th>
                    <th>Product</th>
                    <th>Category</th>
                    <th>Price</th>
                    <th>Stock</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
            <% for (Product p : products) { %>
                <tr>
                    <td><img src="<%= esc(p.getImage()) %>" alt="<%= esc(p.getName()) %>" class="thumb"></td>
                    <td>
                        <a class="product-name" href="product?id=<%= p.getProductId() %>"><%= esc(p.getName()) %></a>
                        <br><span class="muted">ID <%= p.getProductId() %></span>
                    </td>
                    <td><%= esc(p.getCategory()) %></td>
                    <td>&#8377;<%= p.getPriceText() %></td>
                    <td><%= p.getStock() %></td>
                    <td class="cell-actions">
                        <a href="seller/product?id=<%= p.getProductId() %>" class="btn btn-small">Edit</a>
                        <form action="seller/product" method="post" class="inline-form"
                              onsubmit="return confirm('Delete this product?');">
                            <input type="hidden" name="action" value="delete">
                            <input type="hidden" name="id" value="<%= p.getProductId() %>">
                            <button type="submit" class="btn btn-small btn-danger">Delete</button>
                        </form>
                    </td>
                </tr>
            <% } %>
            </tbody>
        </table>
    <% } %>
</div>
</body>
</html>