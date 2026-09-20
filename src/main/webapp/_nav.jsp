<%
    // Rendered only from pages that have already run their login/role check.
    String navRole = (String) session.getAttribute("role");
    String navName = (String) session.getAttribute("name");

    int navCartCount = 0;
    Object navCartObj = session.getAttribute("cart");
    if (navCartObj instanceof java.util.Map) {
        for (Object v : ((java.util.Map<?, ?>) navCartObj).values()) {
            navCartCount += ((Number) v).intValue();
        }
    }
%>
<nav class="topbar topbar-nav">
    <div class="nav-group">
        <a class="brand" href="home.jsp">DhanyaMart</a>
        <a class="nav-link" href="products">Products</a>
    </div>
    <div class="nav-group">
        <% if ("SELLER".equals(navRole) || "ADMIN".equals(navRole)) { %>
            <a class="nav-link" href="seller/products">Seller Dashboard</a>
        <% } %>
        <% if ("ADMIN".equals(navRole)) { %>
            <a class="nav-link" href="admin">Admin Dashboard</a>
        <% } %>
        <a class="nav-link" href="orders">My Orders</a>
        <a class="nav-link" href="cart">Cart<% if (navCartCount > 0) { %> (<%= navCartCount %>)<% } %></a>
        <span class="nav-user">Hi, <%= esc(navName) %></span>
        <a href="logout" class="btn btn-small">Logout</a>
    </div>
</nav>