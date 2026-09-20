<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.dhanyamart.model.CartItem, com.dhanyamart.model.Product, java.util.List" %>
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
    if (request.getAttribute("cartItems") == null) {
        response.sendRedirect("cart");
        return;
    }
    List<CartItem> items = (List<CartItem>) request.getAttribute("cartItems");
    Double total = (Double) request.getAttribute("cartTotal");
    String msg = request.getParameter("msg");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cart - DhanyaMart</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body class="app-body">
<div class="container page">
    <%@ include file="_nav.jsp" %>

    <h1>Shopping Cart</h1>

    <% if ("stock".equals(msg)) { %>
        <div class="alert alert-error">Some items exceed the available stock. Please adjust the quantity.</div>
    <% } else if ("unavailable".equals(msg)) { %>
        <div class="alert alert-error">Some items are no longer available. Please review your cart.</div>
    <% } %>

    <% if (items == null || items.isEmpty()) { %>
        <div class="card empty-state">
            <p>Your cart is empty.</p>
            <p class="switch"><a href="products">Browse products &rarr;</a></p>
        </div>
    <% } else { %>
        <table class="table">
            <thead>
                <tr>
                    <th>Product</th>
                    <th>Price</th>
                    <th>Quantity</th>
                    <th>Subtotal</th>
                    <th></th>
                </tr>
            </thead>
            <tbody>
            <% for (CartItem item : items) {
                   Product p = item.getProduct(); %>
                <tr>
                    <td>
                        <div class="cell-product">
                            <img src="<%= esc(p.getImage()) %>" alt="<%= esc(p.getName()) %>"
                                 class="thumb">
                            <div>
                                <a class="product-name" href="product?id=<%= p.getProductId() %>">
                                    <%= esc(p.getName()) %></a>
                                <% if (!p.isInStock()) { %>
                                    <span class="badge badge-out">Out of stock</span>
                                <% } %>
                            </div>
                        </div>
                    </td>
                    <td>&#8377;<%= p.getPriceText() %></td>
                    <td>
                        <form action="cart" method="post" class="inline-form">
                            <input type="hidden" name="action" value="update">
                            <input type="hidden" name="id" value="<%= p.getProductId() %>">
                            <input type="number" name="qty" value="<%= item.getQuantity() %>"
                                   min="1" max="<%= Math.min(99, p.getStock() > 0 ? p.getStock() : 1) %>"
                                   class="qty-input">
                            <button type="submit" class="btn btn-small">Update</button>
                        </form>
                    </td>
                    <td>&#8377;<%= item.getSubtotalText() %></td>
                    <td>
                        <form action="cart" method="post" class="inline-form">
                            <input type="hidden" name="action" value="remove">
                            <input type="hidden" name="id" value="<%= p.getProductId() %>">
                            <button type="submit" class="btn btn-small btn-danger">Remove</button>
                        </form>
                    </td>
                </tr>
            <% } %>
            </tbody>
            <tfoot>
                <tr>
                    <td colspan="3" class="right"><strong>Total</strong></td>
                    <td colspan="2"><strong>&#8377;<%= String.format("%,.2f", total) %></strong></td>
                </tr>
            </tfoot>
        </table>

        <div class="action-row">
            <a href="products" class="btn btn-small btn-ghost">&larr; Continue shopping</a>
            <a href="checkout" class="btn btn-small">Proceed to Checkout &rarr;</a>
        </div>
    <% } %>

    <%@ include file="_footer.jsp" %>
</div>
</body>
</html>