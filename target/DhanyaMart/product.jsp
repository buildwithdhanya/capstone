<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.dhanyamart.model.Product, com.dhanyamart.model.Review, java.util.List" %>
<%!
    private String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    .replace("\"", "&quot;").replace("'", "&#39;");
    }
    private String stars(int rating) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(i < rating ? "&#9733;" : "&#9734;");
        }
        return sb.toString();
    }
%>
<%
    Integer userId = (Integer) session.getAttribute("user_id");
    if (userId == null) {
        response.sendRedirect("login.jsp");
        return;
    }
    Product product = (Product) request.getAttribute("product");
    if (product == null) {
        response.sendRedirect("products");
        return;
    }
    List<Review> reviews = (List<Review>) request.getAttribute("reviews");
    Double reviewAverage = (Double) request.getAttribute("reviewAverage");
    int avgRounded = reviewAverage == null ? 0 : (int) Math.round(reviewAverage);
    String msg = request.getParameter("msg");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= esc(product.getName()) %> - DhanyaMart</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body class="app-body">
<div class="container page">
    <%@ include file="_nav.jsp" %>

    <% if ("out-of-stock".equals(msg)) { %>
        <div class="alert alert-error">This product is currently out of stock.</div>
    <% } else if ("added".equals(msg)) { %>
        <div class="alert alert-success">Added to cart! <a href="cart">View cart</a></div>
    <% } else if ("reviewed".equals(msg)) { %>
        <div class="alert alert-success">Thank you! Your review was saved.</div>
    <% } else if ("invalid-rating".equals(msg)) { %>
        <div class="alert alert-error">Please choose a star rating between 1 and 5.</div>
    <% } else if ("invalid-comment".equals(msg)) { %>
        <div class="alert alert-error">Please write a short review (up to 500 characters).</div>
    <% } %>

    <div class="detail-row">
        <div class="detail-image">
            <img src="<%= esc(product.getImage()) %>" alt="<%= esc(product.getName()) %>">
        </div>
        <div class="detail-info">
            <p class="product-category"><%= esc(product.getCategory()) %></p>
            <h1><%= esc(product.getName()) %></h1>
            <p class="product-price product-price-lg">&#8377;<%= product.getPriceText() %></p>

            <% if (reviews != null && !reviews.isEmpty()) { %>
                <p class="rating-line">
                    <span class="stars"><%= stars(avgRounded) %></span>
                    <%= reviewAverage == null ? 0 : String.format("%.1f", reviewAverage) %>
                    from <%= reviews.size() %> review<%= reviews.size() == 1 ? "" : "s" %>
                </p>
            <% } %>

            <% if (product.isInStock()) { %>
                <p class="badge badge-stock">In stock: <%= product.getStock() %></p>
            <% } else { %>
                <p class="badge badge-out">Out of stock</p>
            <% } %>

            <p class="product-desc"><%= esc(product.getDescription()) %></p>

            <form action="cart" method="post" class="buy-row">
                <input type="hidden" name="action" value="add">
                <input type="hidden" name="id" value="<%= product.getProductId() %>">
                <label for="qty">Qty:</label>
                <input type="number" id="qty" name="qty" value="1" min="1"
                       max="<%= Math.min(99, product.getStock() > 0 ? product.getStock() : 1) %>"
                       class="qty-input" <%= product.isInStock() ? "" : "disabled" %>>
                <button type="submit" class="btn btn-small" <%= product.isInStock() ? "" : "disabled" %>>
                    Add to Cart
                </button>
            </form>
            <p class="switch"><a href="products.jsp">&larr; Back to products</a></p>
        </div>
    </div>

    <hr class="divider">

    <h2>Reviews &amp; Ratings</h2>

    <div class="card review-form">
        <h3>Write a review</h3>
        <form action="review" method="post" onsubmit="return validateReviewForm()">
            <input type="hidden" name="productId" value="<%= product.getProductId() %>">
            <div class="form-group">
                <label for="rating">Your rating</label>
                <select id="rating" name="rating" class="search-select">
                    <option value="5">5 - Excellent</option>
                    <option value="4">4 - Good</option>
                    <option value="3">3 - Okay</option>
                    <option value="2">2 - Poor</option>
                    <option value="1">1 - Bad</option>
                </select>
            </div>
            <div class="form-group">
                <label for="comment">Your review</label>
                <textarea id="comment" name="comment" rows="3"
                          placeholder="How was the quality, taste and delivery?"></textarea>
            </div>
            <button type="submit" class="btn btn-small">Submit Review</button>
        </form>
    </div>

    <% if (reviews == null || reviews.isEmpty()) { %>
        <p class="subtitle">No reviews yet. Be the first to rate this product!</p>
    <% } else { %>
        <div class="review-list">
            <% for (Review r : reviews) { %>
                <div class="card review-item">
                    <p class="stars"><%= stars(r.getRating()) %></p>
                    <p><strong><%= esc(r.getUserName()) %></strong>
                        <span class="muted"> - <%= esc(r.getCreatedAt()) %></span></p>
                    <p><%= esc(r.getComment()) %></p>
                </div>
            <% } %>
        </div>
    <% } %>
</div>

<script>
    function validateReviewForm() {
        var comment = document.getElementById("comment").value.trim();
        if (comment === "") { alert("Please write a review comment."); return false; }
        if (comment.length > 500) { alert("Review must be at most 500 characters."); return false; }
        return true;
    }
</script>
</body>
</html>