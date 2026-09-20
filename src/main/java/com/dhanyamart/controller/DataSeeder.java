package com.dhanyamart.controller;

import com.dhanyamart.dao.ProductDAO;
import com.dhanyamart.dao.ReviewDAO;
import com.dhanyamart.dao.UserDAO;
import com.dhanyamart.model.Product;
import com.dhanyamart.model.Review;
import com.dhanyamart.model.User;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.util.List;

/**
 * Runs once when the web app starts up. Creates the default admin/seller/customer
 * demo accounts (if they do not exist yet) and seeds the product catalogue the
 * first time the app is deployed. Nothing is overwritten.
 *
 * Demo accounts:
 *   admin@dhanyamart.com  /  Admin@123   (Admin dashboard)
 *   seller@dhanyamart.com /  Seller@123  (Seller dashboard)
 *   demo@dhanyamart.com   /  Demo@123    (ordinary customer with a review)
 */
@WebListener
public class DataSeeder implements ServletContextListener {

    private final UserDAO userDAO = new UserDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final ReviewDAO reviewDAO = new ReviewDAO();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        seedUser("admin@dhanyamart.com", "Admin@123", "DhanyaMart Admin", UserDAO.ROLE_ADMIN);
        User seller = seedUser("seller@dhanyamart.com", "Seller@123", "CraftWorks Store", UserDAO.ROLE_SELLER);
        User demo = seedUser("demo@dhanyamart.com", "Demo@123", "Demo Customer", UserDAO.ROLE_CUSTOMER);

        seedProducts(seller != null ? seller.getUserId() : 1002);
        seedReviews(demo != null ? demo.getUserId() : 1003);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // nothing to clean up
    }

    /** Creates the account only when the email is not taken yet. */
    private User seedUser(String email, String password, String name, String role) {
        User existing = userDAO.findByEmail(email);
        if (existing != null) {
            return existing;
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPhone("0000000000");
        user.setAddress("DhanyaMart Demo Account");
        user.setPassword(password);
        userDAO.registerUser(user, role);
        return userDAO.findByEmail(email);
    }

    /** Seeds the catalogue on the very first run (when products.xlsx has no rows). */
    private void seedProducts(int sellerId) {
        List<Product> existing = productDAO.listAll();
        if (!existing.isEmpty()) {
            return;
        }

        add("Warli Folk Art Wall Painting", "Paintings & Art",
                "Hand-painted Warli art on canvas, made with rice paste paints by tribal artisans.", 2450.00, 12,
                "images/products/art.jpg", sellerId);
        add("Miniature Tanjore Ganesha Painting", "Paintings & Art",
                "Traditional Tanjore-style painting with gold foil work on a wooden panel.", 3800.00, 8,
                "images/products/painting.jpg", sellerId);
        add("Kalamkari Cotton Saree", "Textiles & Handloom",
                "Handblock-printed Kalamkari saree in natural vegetable dyes on pure cotton.", 1499.00, 25,
                "images/products/kalamkari.jpg", sellerId);
        add("Kasavu Kerala Cotton Saree", "Textiles & Handloom",
                "Classic cream saree with gold kasavu border, handwoven on traditional looms.", 1799.00, 20,
                "images/products/kasavu.jpg", sellerId);
        add("Kanchipuram Silk Dupatta", "Textiles & Handloom",
                "Soft Kanchipuram silk dupatta with zari border, woven in temple-town looms.", 1299.00, 30,
                "images/products/kanchipuram.jpg", sellerId);
        add("Terracotta Decor Pot (Set of 3)", "Pottery & Ceramics",
                "Hollow terracotta pots in three sizes, fired in a traditional kiln.", 699.00, 40,
                "images/products/pottery.jpg", sellerId);
        add("Hand-Moulded Clay Diyas (Pack of 10)", "Pottery & Ceramics",
                "Eco-friendly clay diyas, handmade and sun-dried, ready to decorate.", 199.00, 200,
                "images/products/diya.jpg", sellerId);
        add("Chettinad Athangudi Tiles (Set of 6)", "Home & Decor",
                "Handmade Athangudi tiles with floral motifs, made the traditional Chettinad way.", 1200.00, 35,
                "images/products/tiles.jpg", sellerId);
        add("Brass Kuthuvilakku Lamp", "Home & Decor",
                "Classic four-wick brass lamp (kuthuvilakku) with intricate floral carvings.", 1650.00, 18,
                "images/products/brass.jpg", sellerId);
        add("Rosewood Carved Jewellery Box", "Woodwork & Decor",
                "Hand-carved rosewood box with floral etchings, cushioned interior.", 2400.00, 12,
                "images/products/wood.jpg", sellerId);
        add("Block-Printed Teak Coasters (Set of 6)", "Woodwork & Decor",
                "Teak wood coasters printed with traditional hand-carved blocks.", 450.00, 50,
                "images/products/coasters.jpg", sellerId);
        add("Cane & Bamboo Storage Basket", "Woodwork & Decor",
                "Woven cane basket with bamboo frame, perfect for storage or decor.", 880.00, 22,
                "images/products/basket.jpg", sellerId);
        add("Temple Jewellery Kausala Set", "Jewellery & Accessories",
                "Traditional temple jewellery kausala with garnet stones and antique finish.", 3200.00, 10,
                "images/products/jewellery.jpg", sellerId);
        add("Brass Lakshmi Pendant", "Jewellery & Accessories",
                "Hand-engraved brass Lakshmi pendant with cotton cord, antique oxidised finish.", 950.00, 16,
                "images/products/pendant.jpg", sellerId);
        add("Handwoven Cotton Dhurrie Rug", "Textiles & Handloom",
                "Thick cotton dhurrie rug in earthy stripes, woven on a flat loom.", 2100.00, 9,
                "images/products/dhurrie.jpg", sellerId);
    }

    private void add(String name, String category, String description,
                     double price, int stock, String image, int sellerId) {
        Product p = new Product();
        p.setName(name);
        p.setCategory(category);
        p.setDescription(description);
        p.setPrice(price);
        p.setStock(stock);
        p.setImage(image);
        p.setSellerId(sellerId);
        productDAO.add(p);
    }

    /** A few example ratings so reviews appear on the detail pages. */
    private void seedReviews(int demoUserId) {
        List<Product> products = productDAO.listAll();
        for (Product p : products) {
            if (!reviewDAO.listByProduct(p.getProductId()).isEmpty()) {
                return; // already has reviews -> do not touch
            }
        }
        if (products.size() < 5) {
            return;
        }
        addReview(products.get(0), demoUserId, 5, "Authentic Warli art - every line tells a story. Excellent finish.");
        addReview(products.get(1), demoUserId, 4, "Rich colours and beautiful gold foil work. Looks grand on my wall.");
        addReview(products.get(8), demoUserId, 5, "Classic kuthuvilakku with a perfect finish. Lights up my pooja room.");
    }

    private void addReview(Product product, int userId, int rating, String comment) {
        Review r = new Review();
        r.setProductId(product.getProductId());
        r.setUserId(userId);
        r.setUserName("Demo Customer");
        r.setRating(rating);
        r.setComment(comment);
        reviewDAO.addOrUpdate(r);
    }
}