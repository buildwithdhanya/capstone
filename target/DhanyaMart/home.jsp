<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.dhanyamart.dao.OrderDAO, com.dhanyamart.dao.ProductDAO" %>
<%@ page import="com.dhanyamart.model.Product" %>
<%@ page import="java.util.ArrayList, java.util.LinkedHashMap, java.util.List, java.util.Map" %>
<%!
    // Escapes HTML so any user-typed text is shown safely (prevents XSS)
    private String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    .replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    // ---------- PROTECTED PAGE CHECK ----------
    Integer userId = (Integer) session.getAttribute("user_id");
    if (userId == null) {
        response.sendRedirect("login.jsp");
        return;
    }
    String name = (String) session.getAttribute("name");
    String role = (String) session.getAttribute("role");

    // ----- Live dashboard data (read from the Excel store) -----
    int dashCartCount = 0;
    Object cartObj = session.getAttribute("cart");
    if (cartObj instanceof Map) {
        for (Object v : ((Map<?, ?>) cartObj).values()) {
            dashCartCount += ((Number) v).intValue();
        }
    }

    ProductDAO dashProductDAO = new ProductDAO();
    List<Product> dashAll = dashProductDAO.listAll();
    List<String> dashCats = dashProductDAO.listCategories();
    int dashOrderCount = new OrderDAO().listByUser(userId).size();
    int dashProductCount = dashAll.size();

    Map<String, String> catImage = new LinkedHashMap<>();
    for (Product p : dashAll) {
        if (!catImage.containsKey(p.getCategory()) && p.getImage() != null) {
            catImage.put(p.getCategory(), p.getImage());
        }
    }

    List<Product> featured = new ArrayList<>();
    for (int i = 0; i < dashAll.size() && featured.size() < 4; i++) {
        featured.add(dashAll.get(i));
    }

    java.time.LocalTime nowTime = java.time.LocalTime.now();
    String greet = nowTime.getHour() < 12 ? "Good morning"
                 : nowTime.getHour() < 17 ? "Good afternoon" : "Good evening";
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard - DhanyaMart</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body class="app-body">
<div class="container page">
    <%@ include file="_nav.jsp" %>

    <section class="hero">
        <h1><%= greet %>, <%= esc(name) %>!</h1>
        <p>Handcrafted crafts, handloom textiles and ethnic decor &mdash; straight from the looms and kilns of local artisans to your home.</p>
        <span class="role-chip"><%= esc(role) %> account</span>
        <div class="hero-actions">
            <a href="products" class="btn btn-small icon-btn">Browse the collection &rarr;</a>
            <% if (!"ADMIN".equals(role)) { %>
            <a href="cart" class="btn btn-small ghost-white">View my cart</a>
            <% } %>
        </div>
    </section>

    <div class="stat-strip">
        <div class="stat-card">
            <span class="stat-val"><%= dashCartCount %></span>
            <span class="stat-lbl">Items in your cart</span>
            <br><a class="stat-link" href="cart">Open cart &rarr;</a>
        </div>
        <div class="stat-card s2">
            <span class="stat-val"><%= dashOrderCount %></span>
            <span class="stat-lbl">Orders placed</span>
            <br><a class="stat-link" href="orders">View orders &rarr;</a>
        </div>
        <div class="stat-card s3">
            <span class="stat-val"><%= dashProductCount %></span>
            <span class="stat-lbl">Crafts available</span>
            <br><a class="stat-link" href="products">Browse all &rarr;</a>
        </div>
        <div class="stat-card s4">
            <span class="stat-val"><%= dashCats.size() %></span>
            <span class="stat-lbl">Craft categories</span>
            <br><a class="stat-link" href="products">Explore &rarr;</a>
        </div>
    </div>

    <div class="section-head">
        <h2>Shop by category</h2>
        <a class="link" href="products">View full catalogue &rarr;</a>
    </div>
    <% if (dashCats != null && !dashCats.isEmpty()) { %>
    <div class="category-grid">
        <% for (String c : dashCats) {
            String img = catImage.get(c);
            int count = 0;
            for (Product p : dashAll) if (c.equals(p.getCategory())) count++;
        %>
            <a class="cat-card" href="products?cat=<%= esc(c) %>">
                <% if (img != null) { %>
                    <img src="<%= esc(img) %>" alt="<%= esc(c) %>" loading="lazy">
                <% } %>
                <span class="cat-cap"><%= esc(c) %><small><%= count %> item<%= count == 1 ? "" : "s" %></small></span>
            </a>
        <% } %>
    </div>
    <% } %>

    <% if (!featured.isEmpty()) { %>
    <div class="section-head">
        <h2>Featured this week</h2>
        <a class="link" href="products">See more &rarr;</a>
    </div>
    <div class="mini-grid">
        <% for (Product p : featured) { %>
            <a class="mini-card" href="product?id=<%= p.getProductId() %>">
                <img src="<%= esc(p.getImage()) %>" alt="<%= esc(p.getName()) %>" loading="lazy">
                <span class="mini-info">
                    <span class="mini-name"><%= esc(p.getName()) %></span>
                    <span class="mini-sub"><%= esc(p.getCategory()) %></span>
                    <span class="mini-price">&#8377;<%= p.getPriceText() %></span>
                </span>
            </a>
        <% } %>
    </div>
    <% } %>

    <div class="section-head">
        <h2>Jump back in</h2>
    </div>
    <div class="grid grid-3">
        <a href="products" class="card link-card">
            <h3>Browse Products</h3>
            <p>Explore the full catalogue of crafts, handlooms, pottery and more.</p>
        </a>
        <a href="cart" class="card link-card">
            <h3>Your Cart</h3>
            <p>Review items, change quantities or remove products before checkout.</p>
        </a>
        <a href="orders" class="card link-card">
            <h3>My Orders</h3>
            <p>Track placed orders and their current status.</p>
        </a>
        <% if ("SELLER".equals(role) || "ADMIN".equals(role)) { %>
        <a href="seller/products" class="card link-card">
            <h3>Seller Dashboard</h3>
            <p>Add, edit and manage the products you sell.</p>
        </a>
        <% } %>
        <% if ("ADMIN".equals(role)) { %>
        <a href="admin" class="card link-card">
            <h3>Admin Dashboard</h3>
            <p>Manage users, products and orders across the store.</p>
        </a>
        <% } %>
    </div>

    <div class="band">
        <span class="band-badge">A</span>
        <p><strong>Why DhanyaMart?</strong> Every product is handcrafted by a real artisan. Your purchase keeps traditional crafts alive &mdash; support local, shop conscious.</p>
    </div>

    <%@ include file="_footer.jsp" %>
</div>
</body>
</html>