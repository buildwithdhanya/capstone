package com.dhanyamart.util;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Low-level Excel (.xlsx) I/O using Apache POI.
 *
 * All data files live in:   <user home>/dhanyamart-data/
 * (Windows example: C:\\Users\\<you>\\dhanyamart-data\\)
 *
 * Data files used by the app:
 *   users.xlsx    (authentication, managed by UserDAO)
 *   products.xlsx (product catalogue, managed by ProductDAO)
 *   orders.xlsx   (placed orders, managed by OrderDAO)
 *   reviews.xlsx  (product ratings, managed by ReviewDAO)
 *
 * To change the location, set the Java system property  dhanyamart.data.dir
 * or the environment variable  DHANYAMART_DATA_DIR  to your preferred folder.
 *
 * Every file has a header row (row 0). If a file does not exist it is
 * created automatically (with headers) on first use.
 */
public class ExcelUtil {

    /** Shared lock so concurrent requests never corrupt an Excel file. */
    public static final Object LOCK = new Object();

    /** users.xlsx header (column order must never change). */
    public static final String[] USER_HEADERS = {
            "user_id", "name", "email", "password", "phone", "address", "created_at", "role"
    };

    /** products.xlsx header. */
    public static final String[] PRODUCT_HEADERS = {
            "product_id", "seller_id", "name", "category", "description",
            "price", "stock", "image", "created_at"
    };

    /** orders.xlsx header. */
    public static final String[] ORDER_HEADERS = {
            "order_id", "user_id", "user_name", "user_email", "phone", "address",
            "order_date", "status", "total", "items"
    };

    /** reviews.xlsx header. */
    public static final String[] REVIEW_HEADERS = {
            "review_id", "product_id", "user_id", "user_name", "rating", "comment", "created_at"
    };

    /** Returns the shared data directory. */
    public static File getDataDir() {
        String dataDir = System.getProperty("dhanyamart.data.dir");
        if (dataDir == null || dataDir.trim().isEmpty()) {
            dataDir = System.getenv("DHANYAMART_DATA_DIR");
        }
        if (dataDir == null || dataDir.trim().isEmpty()) {
            dataDir = System.getProperty("user.home") + File.separator + "dhanyamart-data";
        }
        return new File(dataDir);
    }

    /** Returns the File object for a data file in the data directory. */
    public static File getFile(String fileName) {
        return new File(getDataDir(), fileName);
    }

    /** Convenience: returns the File object for users.xlsx. */
    public static File getUsersFile() {
        return getFile("users.xlsx");
    }

    /** Opens the users.xlsx workbook, creating it with headers on first run. */
    public static Workbook openOrCreate() throws IOException {
        return openOrCreate("users.xlsx", "users", USER_HEADERS);
    }

    /**
     * Opens (or creates with a header row) the given workbook file.
     * The caller MUST close the returned Workbook (try-with-resources).
     *
     * @param fileName  the file name inside the data directory, e.g. "products.xlsx"
     * @param sheetName sheet name to use when creating a new file
     * @param headers   header text written into row 0 for a new file
     */
    public static Workbook openOrCreate(String fileName, String sheetName, String[] headers)
            throws IOException {
        File file = getFile(fileName);

        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create data directory: " + parent);
        }

        if (file.exists()) {
            try (FileInputStream in = new FileInputStream(file)) {
                return WorkbookFactory.create(in);
            }
        }

        // First run: build a fresh workbook with the header row.
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet(sheetName);

        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }
        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, 22 * 256); // ~22 characters wide
        }

        save(wb, file);
        return wb;
    }

    /** Writes the workbook back to users.xlsx. */
    public static void saveUsers(Workbook wb) throws IOException {
        save(wb, getUsersFile());
    }

    /** Writes the workbook back to products.xlsx. */
    public static void saveProducts(Workbook wb) throws IOException {
        save(wb, getFile("products.xlsx"));
    }

    /** Writes the workbook back to orders.xlsx. */
    public static void saveOrders(Workbook wb) throws IOException {
        save(wb, getFile("orders.xlsx"));
    }

    /** Writes the workbook back to reviews.xlsx. */
    public static void saveReviews(Workbook wb) throws IOException {
        save(wb, getFile("reviews.xlsx"));
    }

    /** Writes the workbook back to a specific data file. */
    public static void save(Workbook wb, String fileName) throws IOException {
        save(wb, getFile(fileName));
    }

    /** Writes the workbook back to a specific file. */
    public static void save(Workbook wb, File file) throws IOException {
        try (FileOutputStream out = new FileOutputStream(file)) {
            wb.write(out);
        }
    }
}