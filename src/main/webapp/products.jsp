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
    String sort = (String) request.getAttribute("sort");
    Integer total = (Integer) request.getAttribute("total");
    Integer pageNo = (Integer) request.getAttribute("page");
    Integer totalPages = (Integer) request.getAttribute("totalPages");
    if (q == null) q = "";
    if (cat == null) cat = "";
    if (sort == null) sort = "";
    if (total == null) total = 0;
    if (pageNo == null) pageNo = 1;
    if (totalPages == null) totalPages = 1;
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

    <h1 class="page-title">Product Catalogue</h1>
    <p class="subtitle" style="text-align:left">Search crafts, handlooms and decor &mdash; or browse by category.</p>

    <form action="products" method="get" class="search-bar">
        <input type="text" name="q" value="<%= esc(q) %>" placeholder="Search for a craft, saree, pottery, art..."
               class="search-input">
        <select name="cat" class="search-select">
            <option value="">All categories</option>
            <% for (String c : categories) { %>
                <option value="<%= esc(c) %>" <%= c.equals(cat) ? "selected" : "" %>><%= esc(c) %></option>
            <% } %>
        </select>
        <select name="sort" class="search-select">
            <option value="">Sort: Default</option>
            <option value="newest" <%= "newest".equals(sort) ? "selected" : "" %>>Newest first</option>
            <option value="price_asc" <%= "price_asc".equals(sort) ? "selected" : "" %>>Price: Low to High</option>
            <option value="price_desc" <%= "price_desc".equals(sort) ? "selected" : "" %>>Price: High to Low</option>
            <option value="name" <%= "name".equals(sort) ? "selected" : "" %>>Name A-Z</option>
        </select>
        <button type="submit" class="btn btn-small">Search</button>
    </form>

    <% if (products == null || products.isEmpty()) { %>
        <div class="alert alert-error">No products match your search.</div>
    <% } else { %>
        <p class="subtitle" style="text-align:left"><%= total %> product<%= total == 1 ? "" : "s" %> found</p>
        <div class="grid grid-3 product-grid">
            <% for (Product p : products) { %>
                <article class="card product-card">
                    <div class="image-wrap">
                        <a href="product?id=<%= p.getProductId() %>">
                            <img src="<%= esc(p.getImage()) %>" alt="<%= esc(p.getName()) %>"
                                 class="product-image" loading="lazy">
                        </a>
                        <a href="product?id=<%= p.getProductId() %>" class="btn btn-small">View Details</a>
                    </div>
                    <div class="product-body">
                        <p class="product-category"><%= esc(p.getCategory()) %></p>
                        <h3 class="product-name">
                            <a href="product?id=<%= p.getProductId() %>"><%= esc(p.getName()) %></a>
                        </h3>
                        <p class="product-desc"><%= esc(p.getDescription()) %></p>
                        <p class="product-price">&#8377;<%= p.getPriceText() %></p>
                    </div>
                    <div class="product-meta">
                        <% if (p.isInStock()) { %>
                            <span class="badge badge-stock">In stock: <%= p.getStock() %></span>
                        <% } else { %>
                            <span class="badge badge-out">Out of stock</span>
                        <% } %>
                    </div>
                </article>
            <% } %>
        </div>

        <% if (totalPages > 1) { %>
            <div class="pager">
                <% if (pageNo > 1) { %>
                    <a class="btn btn-small" href="products?q=<%= esc(q) %>&cat=<%= esc(cat) %>&sort=<%= esc(sort) %>&page=<%= pageNo - 1 %>">&larr; Previous</a>
                <% } %>
                <span class="pager-info">Page <%= pageNo %> of <%= totalPages %></span>
                <% if (pageNo < totalPages) { %>
                    <a class="btn btn-small" href="products?q=<%= esc(q) %>&cat=<%= esc(cat) %>&sort=<%= esc(sort) %>&page=<%= pageNo + 1 %>">Next &rarr;</a>
                <% } %>
            </div>
        <% } %>
    <% } %>

    <%@ include file="_footer.jsp" %>
</div>
</body>
</html>