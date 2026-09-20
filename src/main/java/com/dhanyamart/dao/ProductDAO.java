package com.dhanyamart.dao;

import com.dhanyamart.model.Product;
import com.dhanyamart.util.CellUtil;
import com.dhanyamart.util.ExcelUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Data Access Object - every operation that touches products.xlsx goes through
 * this class (add / edit / delete / search / stock management).
 */
public class ProductDAO {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Column indexes must match ExcelUtil.PRODUCT_HEADERS
    private static final int COL_ID = 0;
    private static final int COL_SELLER_ID = 1;
    private static final int COL_NAME = 2;
    private static final int COL_CATEGORY = 3;
    private static final int COL_DESCRIPTION = 4;
    private static final int COL_PRICE = 5;
    private static final int COL_STOCK = 6;
    private static final int COL_IMAGE = 7;
    private static final int COL_CREATED_AT = 8;

    public static final String DEFAULT_IMAGE = "images/products/generic.svg";

    /** All products, in file order. */
    public List<Product> listAll() {
        return search(null, null);
    }

    /**
     * Products matching an optional text query (name/description, case-insensitive)
     * and an optional category (exact, case-insensitive). Null/blank means "all".
     */
    public List<Product> search(String query, String category) {
        List<Product> result = new ArrayList<>();
        String q = query == null ? "" : query.trim().toLowerCase();
        String cat = category == null ? "" : category.trim();
        synchronized (ExcelUtil.LOCK) {
            try (Workbook wb = ExcelUtil.openOrCreate("products.xlsx", "products", ExcelUtil.PRODUCT_HEADERS)) {
                for (Row row : wb.getSheetAt(0)) {
                    if (row.getRowNum() == 0) continue;
                    Product p = rowToProduct(row);
                    if (!cat.isEmpty() && !p.getCategory().equalsIgnoreCase(cat)) {
                        continue;
                    }
                    if (!q.isEmpty()) {
                        boolean match = p.getName().toLowerCase().contains(q)
                                || p.getDescription().toLowerCase().contains(q);
                        if (!match) continue;
                    }
                    result.add(p);
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not read products.xlsx", e);
            }
        }
        return result;
    }

    /** Distinct categories (sorted), used for the filter dropdown. */
    public List<String> listCategories() {
        TreeSet<String> set = new TreeSet<>();
        synchronized (ExcelUtil.LOCK) {
            try (Workbook wb = ExcelUtil.openOrCreate("products.xlsx", "products", ExcelUtil.PRODUCT_HEADERS)) {
                for (Row row : wb.getSheetAt(0)) {
                    if (row.getRowNum() == 0) continue;
                    String c = CellUtil.str(row, COL_CATEGORY);
                    if (!c.isBlank()) set.add(c.trim());
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not read products.xlsx", e);
            }
        }
        return new ArrayList<>(set);
    }

    public Product findById(int productId) {
        synchronized (ExcelUtil.LOCK) {
            try (Workbook wb = ExcelUtil.openOrCreate("products.xlsx", "products", ExcelUtil.PRODUCT_HEADERS)) {
                for (Row row : wb.getSheetAt(0)) {
                    if (row.getRowNum() == 0) continue;
                    if (CellUtil.asInt(row, COL_ID) == productId) {
                        return rowToProduct(row);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not read products.xlsx", e);
            }
        }
        return null;
    }

    /** Adds a new product. The id is generated automatically. */
    public boolean add(Product product) {
        synchronized (ExcelUtil.LOCK) {
            try (Workbook wb = ExcelUtil.openOrCreate("products.xlsx", "products", ExcelUtil.PRODUCT_HEADERS)) {
                Sheet sheet = wb.getSheetAt(0);
                int newId = nextId(sheet);
                Row row = sheet.createRow(sheet.getLastRowNum() + 1);

                row.createCell(COL_ID).setCellValue(newId);
                row.createCell(COL_SELLER_ID).setCellValue(product.getSellerId());
                row.createCell(COL_NAME).setCellValue(product.getName());
                row.createCell(COL_CATEGORY).setCellValue(product.getCategory());
                row.createCell(COL_DESCRIPTION).setCellValue(product.getDescription());
                row.createCell(COL_PRICE).setCellValue(product.getPrice());
                row.createCell(COL_STOCK).setCellValue(product.getStock());
                row.createCell(COL_IMAGE).setCellValue(safeImage(product.getImage()));
                row.createCell(COL_CREATED_AT).setCellValue(LocalDateTime.now().format(TIMESTAMP));

                ExcelUtil.saveProducts(wb);
                return true;
            } catch (IOException e) {
                throw new RuntimeException("Could not write products.xlsx", e);
            }
        }
    }

    /** Updates an existing product row (matched by product_id). */
    public boolean update(Product product) {
        synchronized (ExcelUtil.LOCK) {
            try (Workbook wb = ExcelUtil.openOrCreate("products.xlsx", "products", ExcelUtil.PRODUCT_HEADERS)) {
                Sheet sheet = wb.getSheetAt(0);
                for (Row row : sheet) {
                    if (row.getRowNum() == 0) continue;
                    if (CellUtil.asInt(row, COL_ID) == product.getProductId()) {
                        row.getCell(COL_NAME).setCellValue(product.getName());
                        row.getCell(COL_CATEGORY).setCellValue(product.getCategory());
                        row.getCell(COL_DESCRIPTION).setCellValue(product.getDescription());
                        row.getCell(COL_PRICE).setCellValue(product.getPrice());
                        row.getCell(COL_STOCK).setCellValue(product.getStock());
                        row.getCell(COL_IMAGE).setCellValue(safeImage(product.getImage()));
                        ExcelUtil.saveProducts(wb);
                        return true;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not write products.xlsx", e);
            }
        }
        return false;
    }

    /** Deletes a product row (admin/seller action). */
    public boolean delete(int productId) {
        synchronized (ExcelUtil.LOCK) {
            try (Workbook wb = ExcelUtil.openOrCreate("products.xlsx", "products", ExcelUtil.PRODUCT_HEADERS)) {
                Sheet sheet = wb.getSheetAt(0);
                for (Row row : sheet) {
                    if (row.getRowNum() == 0) continue;
                    if (CellUtil.asInt(row, COL_ID) == productId) {
                        int rowIndex = row.getRowNum();
                        sheet.removeRow(row);
                        if (sheet.getLastRowNum() >= rowIndex) {
                            sheet.shiftRows(rowIndex + 1, sheet.getLastRowNum(), -1);
                        }
                        ExcelUtil.saveProducts(wb);
                        return true;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not write products.xlsx", e);
            }
        }
        return false;
    }

    /** Lowers the stock level after an order is placed. */
    public void decrementStock(int productId, int quantity) {
        synchronized (ExcelUtil.LOCK) {
            try (Workbook wb = ExcelUtil.openOrCreate("products.xlsx", "products", ExcelUtil.PRODUCT_HEADERS)) {
                Sheet sheet = wb.getSheetAt(0);
                for (Row row : sheet) {
                    if (row.getRowNum() == 0) continue;
                    if (CellUtil.asInt(row, COL_ID) == productId) {
                        int stock = CellUtil.asInt(row, COL_STOCK);
                        row.getCell(COL_STOCK).setCellValue(Math.max(0, stock - quantity));
                        ExcelUtil.saveProducts(wb);
                        return;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not write products.xlsx", e);
            }
        }
    }

    private int nextId(Sheet sheet) {
        int max = 1000;
        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue;
            int id = CellUtil.asInt(row, COL_ID);
            if (id > max) max = id;
        }
        return max + 1;
    }

    private Product rowToProduct(Row row) {
        Product p = new Product();
        p.setProductId(CellUtil.asInt(row, COL_ID));
        p.setSellerId(CellUtil.asInt(row, COL_SELLER_ID));
        p.setName(CellUtil.str(row, COL_NAME));
        p.setCategory(CellUtil.str(row, COL_CATEGORY));
        p.setDescription(CellUtil.str(row, COL_DESCRIPTION));
        p.setPrice(CellUtil.asDouble(row, COL_PRICE));
        p.setStock(CellUtil.asInt(row, COL_STOCK));
        p.setImage(safeImage(CellUtil.str(row, COL_IMAGE)));
        p.setCreatedAt(CellUtil.str(row, COL_CREATED_AT));
        return p;
    }

    private String safeImage(String image) {
        if (image == null || image.isBlank()) {
            return DEFAULT_IMAGE;
        }
        return image;
    }
}