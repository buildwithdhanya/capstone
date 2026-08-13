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
 * The data file lives at:  <user home>/dhanyamart-data/users.xlsx
 * (Windows example: C:\\Users\\<you>\\dhanyamart-data\\users.xlsx)
 *
 * To change the location, set the Java system property  dhanyamart.data.dir
 * or the environment variable  DHANYAMART_DATA_DIR  to your preferred folder.
 *
 * users.xlsx columns (row 0 is the header row):
 *   user_id | name | email | password | phone | address | created_at
 *
 * If the file does not exist yet, it is created automatically on first use.
 */
public class ExcelUtil {

    /** Shared lock so concurrent requests never corrupt the Excel file. */
    public static final Object LOCK = new Object();

    public static final String[] HEADERS = {
            "user_id", "name", "email", "password", "phone", "address", "created_at"
    };

    /**
     * Returns the File object for users.xlsx.
     */
    public static File getUsersFile() {
        String dataDir = System.getProperty("dhanyamart.data.dir");
        if (dataDir == null || dataDir.trim().isEmpty()) {
            dataDir = System.getenv("DHANYAMART_DATA_DIR");
        }
        if (dataDir == null || dataDir.trim().isEmpty()) {
            dataDir = System.getProperty("user.home") + File.separator + "dhanyamart-data";
        }
        return new File(new File(dataDir), "users.xlsx");
    }

    /**
     * Opens the workbook from disk. If it does not exist, creates a new one
     * (with the header row) and saves it. Caller MUST close the returned
     * Workbook (preferably via try-with-resources).
     */
    public static Workbook openOrCreate() throws IOException {
        File file = getUsersFile();

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
        Sheet sheet = wb.createSheet("users");

        Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i).setCellValue(HEADERS[i]);
        }
        for (int i = 0; i < HEADERS.length; i++) {
            sheet.setColumnWidth(i, 22 * 256); // ~22 characters wide
        }

        save(wb);
        return wb;
    }

    /**
     * Writes the in-memory workbook back to users.xlsx.
     */
    public static void save(Workbook wb) throws IOException {
        File file = getUsersFile();
        try (FileOutputStream out = new FileOutputStream(file)) {
            wb.write(out);
        }
    }
}
