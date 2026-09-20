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
    if (session.getAttribute("user_id") == null) {
        response.sendRedirect("login.jsp");
        return;
    }
    if (request.getAttribute("products") == null) {
        response.sendRedirect("products");
        return;
    }
    List<Product> products = (List<Product>) request.getAttribute("products");
    List<String> categories = (List<String>) request.getAttribute("categories");
    String q = (String) request.getAttribute("q");
    String cat = (String) request.getAttribute("cat");
    if (q == null) q = "";
    if (cat == null) cat = "";
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Products - DhanyaMart</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body class="app-body">
<div class="container page">
    <%@ include file="_nav.jsp" %>

    <h1>Product Catalogue</h1>
    <p class="subtitle">Search by name or description, or browse by category.</p>

    <form action="products" method="get" class="search-bar">
        <input type="text" name="q" value="<%= esc(q) %>" placeholder="Search rice, dal, ghee..."
               class="search-input">
        <select name="cat" class="search-select">
            <option value="">All categories</option>
            <% for (String c : categories) { %>
                <option value="<%= esc(c) %>" <%= c.equals(cat) ? "selected" : "" %>><%= esc(c) %></option>
            <% } %>
        </select>
        <button type="submit" class="btn btn-small">Search</button>
    </form>

    <% if (products == null || products.isEmpty()) { %>
        <div class="alert alert-error">No products match your search.</div>
    <% } else { %>
        <div class="grid grid-3 product-grid">
            <% for (Product p : products) { %>
                <div class="card product-card">
                    <a href="product?id=<%= p.getProductId() %>">
                        <img src="<%= esc(p.getImage()) %>" alt="<%= esc(p.getName()) %>"
                             class="product-image" loading="lazy">
                    </a>
                    <p class="product-category"><%= esc(p.getCategory()) %></p>
                    <h3 class="product-name">
                        <a href="product?id=<%= p.getProductId() %>"><%= esc(p.getName()) %></a>
                    </h3>
                    <p class="product-desc"><%= esc(p.getDescription()) %></p>
                    <p class="product-price">&#8377;<%= p.getPriceText() %></p>
                    <% if (p.isInStock()) { %>
                        <p class="badge badge-stock">In stock: <%= p.getStock() %></p>
                    <% } else { %>
                        <p class="badge badge-out">Out of stock</p>
                    <% } %>
                    <a href="product?id=<%= p.getProductId() %>" class="btn btn-small">View Details</a>
                </div>
            <% } %>
        </div>
    <% } %>
</div>
</body>
</html>