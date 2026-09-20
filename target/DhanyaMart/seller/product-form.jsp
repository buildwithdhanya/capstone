<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.dhanyamart.model.Product, java.util.Set" %>
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
    if (request.getAttribute("categories") == null) {
        response.sendRedirect(request.getContextPath() + "/seller/product");
        return;
    }
    Product product = (Product) request.getAttribute("product");
    Set<String> categories = (Set<String>) request.getAttribute("categories");
    boolean editing = product != null;
    String msg = request.getParameter("msg");

    String fName = editing ? product.getName() : (request.getParameter("name") != null ? request.getParameter("name") : "");
    String fCategory = editing ? product.getCategory() : (request.getParameter("category") != null ? request.getParameter("category") : "");
    String fDescription = editing ? product.getDescription() : (request.getParameter("description") != null ? request.getParameter("description") : "");
    String fPrice = editing ? String.valueOf(product.getPrice()) : (request.getParameter("price") != null ? request.getParameter("price") : "");
    String fStock = editing ? String.valueOf(product.getStock()) : (request.getParameter("stock") != null ? request.getParameter("stock") : "");
    String fImage = editing ? product.getImage() : (request.getParameter("image") != null ? request.getParameter("image") : "");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= editing ? "Edit Product" : "Add Product" %> - DhanyaMart</title>
    <base href="<%= request.getContextPath() %>/">
    <link rel="stylesheet" href="css/style.css">
</head>
<body class="app-body">
<div class="container page">
    <%@ include file="/_nav.jsp" %>

    <h1><%= editing ? "Edit Product #" + product.getProductId() : "Add New Product" %></h1>
    <p class="subtitle">Fields marked with validation rules only.</p>

    <% if ("invalid-name".equals(msg)) { %>
        <div class="alert alert-error">Name must be 3-100 characters.</div>
    <% } else if ("invalid-category".equals(msg)) { %>
        <div class="alert alert-error">Please choose a valid category.</div>
    <% } else if ("invalid-description".equals(msg)) { %>
        <div class="alert alert-error">Description must be 5-1000 characters.</div>
    <% } else if ("invalid-price".equals(msg)) { %>
        <div class="alert alert-error">Price must be greater than 0.</div>
    <% } else if ("invalid-stock".equals(msg)) { %>
        <div class="alert alert-error">Stock must be between 0 and 10000.</div>
    <% } else if ("invalid-image".equals(msg)) { %>
        <div class="alert alert-error">Image must be a local path (images/products/...) or an http(s) URL.</div>
    <% } %>

    <form action="seller/product" method="post" class="checkout-form"
          onsubmit="return validateProductForm()">
        <input type="hidden" name="action" value="save">
        <% if (editing) { %>
            <input type="hidden" name="id" value="<%= product.getProductId() %>">
        <% } %>

        <div class="form-group">
            <label for="name">Product Name</label>
            <input type="text" id="name" name="name" value="<%= esc(fName) %>"
                   placeholder="e.g. Handwoven Pichwai Painting" autofocus>
        </div>

        <div class="form-group">
            <label for="category">Category</label>
            <select id="category" name="category" class="search-select">
                <option value="">-- Choose category --</option>
                <% for (String c : categories) { %>
                    <option value="<%= esc(c) %>" <%= c.equals(fCategory) ? "selected" : "" %>><%= esc(c) %></option>
                <% } %>
            </select>
        </div>

        <div class="form-group">
            <label for="description">Description</label>
            <textarea id="description" name="description" rows="4"
                      placeholder="Quality, region, packaging..."><%= esc(fDescription) %></textarea>
        </div>

        <div class="form-row-2">
            <div class="form-group">
                <label for="price">Price (&#8377;)</label>
                <input type="number" id="price" name="price" value="<%= esc(fPrice) %>"
                       min="0.01" step="0.01" placeholder="e.g. 299.00">
            </div>
            <div class="form-group">
                <label for="stock">Stock</label>
                <input type="number" id="stock" name="stock" value="<%= esc(fStock) %>"
                       min="0" max="10000" placeholder="e.g. 50">
            </div>
        </div>

        <div class="form-group">
            <label for="image">Image</label>
            <input type="text" id="image" name="image" value="<%= esc(fImage) %>"
                   placeholder="images/products/wood.jpg or an http(s) URL">
            <p class="muted">Leave as the default or use one of the bundled images
                (art.jpg, painting.jpg, kalamkari.jpg, kasavu.jpg, kanchipuram.jpg,
                pottery.jpg, diya.jpg, tiles.jpg, brass.jpg, wood.jpg, coasters.jpg,
                basket.jpg, jewellery.jpg, pendant.jpg, dhurrie.jpg, generic.svg)
                or any public image URL.</p>
        </div>

        <div class="action-row">
            <button type="submit" class="btn btn-small"><%= editing ? "Save Changes" : "Add Product" %></button>
            <a href="seller/products" class="btn btn-small btn-ghost">Cancel</a>
        </div>
    </form>

    <%@ include file="/_footer.jsp" %>
</div>

<script>
    function validateProductForm() {
        var name = document.getElementById("name").value.trim();
        var category = document.getElementById("category").value;
        var description = document.getElementById("description").value.trim();
        var price = parseFloat(document.getElementById("price").value);
        var stock = document.getElementById("stock").value;

        if (name.length < 3 || name.length > 100) { alert("Name must be 3-100 characters."); return false; }
        if (category === "") { alert("Please choose a category."); return false; }
        if (description.length < 5 || description.length > 1000) { alert("Description must be 5-1000 characters."); return false; }
        if (isNaN(price) || price <= 0) { alert("Please enter a valid price greater than 0."); return false; }
        if (stock === "" || isNaN(stock) || stock < 0 || stock > 10000) { alert("Stock must be between 0 and 10000."); return false; }
        return true;
    }
</script>
</body>
</html>