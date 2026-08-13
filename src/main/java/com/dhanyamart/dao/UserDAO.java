package com.dhanyamart.dao;

import com.dhanyamart.model.User;
import com.dhanyamart.util.ExcelUtil;
import com.dhanyamart.util.PasswordUtil;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Data Access Object - every operation that touches users.xlsx goes through
 * this class. This is the ONLY class the controllers talk to for user data,
 * so if you later switch to MySQL you only have to change this file
 * (that is where a JDBC connection would plug in).
 */
public class UserDAO {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Column indexes must match the header order in ExcelUtil.HEADERS
    private static final int COL_ID = 0;
    private static final int COL_NAME = 1;
    private static final int COL_EMAIL = 2;
    private static final int COL_PASSWORD = 3;
    private static final int COL_PHONE = 4;
    private static final int COL_ADDRESS = 5;
    private static final int COL_CREATED_AT = 6;

    /** Returns true if a row with the given email already exists. */
    public boolean emailExists(String email) {
        return findByEmail(email) != null;
    }

    /** Finds a user by email (email match is case-insensitive). */
    public User findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        synchronized (ExcelUtil.LOCK) {
            try (Workbook wb = ExcelUtil.openOrCreate()) {
                for (Row row : wb.getSheetAt(0)) {
                    if (row.getRowNum() == 0) continue; // skip header
                    if (email.equalsIgnoreCase(cellValue(row, COL_EMAIL))) {
                        return rowToUser(row);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not read users.xlsx", e);
            }
        }
        return null;
    }

    /**
     * Verifies email + password against users.xlsx.
     * Returns the matching User (without the password hash) or null on failure.
     */
    public User login(String email, String rawPassword) {
        if (email == null || email.isBlank() || rawPassword == null) {
            return null;
        }
        synchronized (ExcelUtil.LOCK) {
            try (Workbook wb = ExcelUtil.openOrCreate()) {
                for (Row row : wb.getSheetAt(0)) {
                    if (row.getRowNum() == 0) continue; // skip header

                    if (email.equalsIgnoreCase(cellValue(row, COL_EMAIL))) {
                        String storedHash = cellValue(row, COL_PASSWORD);
                        if (PasswordUtil.verifyPassword(rawPassword, storedHash)) {
                            User user = rowToUser(row);
                            user.setPassword(null); // never pass the hash around
                            return user;
                        }
                        return null; // email found but wrong password
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not read users.xlsx", e);
            }
        }
        return null; // email not found
    }

    /**
     * Adds a new user row to users.xlsx. The password is hashed before saving.
     * Returns false if the email is already taken (nothing is written).
     */
    public boolean registerUser(User user) {
        synchronized (ExcelUtil.LOCK) {
            try (Workbook wb = ExcelUtil.openOrCreate()) {
                Sheet sheet = wb.getSheetAt(0);

                // Duplicate email check inside the lock (re-read the file,
                // do not trust a stale read from the servlet).
                for (Row row : sheet) {
                    if (row.getRowNum() == 0) continue;
                    if (user.getEmail().equalsIgnoreCase(cellValue(row, COL_EMAIL))) {
                        return false;
                    }
                }

                int newId = nextUserId(sheet);
                Row row = sheet.createRow(sheet.getLastRowNum() + 1);

                row.createCell(COL_ID).setCellValue(newId);
                row.createCell(COL_NAME).setCellValue(user.getName());
                row.createCell(COL_EMAIL).setCellValue(user.getEmail());
                // ONLY the salted hash is written to Excel, never the plain text
                row.createCell(COL_PASSWORD).setCellValue(PasswordUtil.hashPassword(user.getPassword()));
                row.createCell(COL_PHONE).setCellValue(user.getPhone());
                row.createCell(COL_ADDRESS).setCellValue(user.getAddress());
                row.createCell(COL_CREATED_AT).setCellValue(LocalDateTime.now().format(TIMESTAMP));

                ExcelUtil.save(wb);
                return true;
            } catch (IOException e) {
                throw new RuntimeException("Could not write users.xlsx", e);
            }
        }
    }

    /** Generates the next user_id = max existing id + 1 (starts at 1001). */
    private int nextUserId(Sheet sheet) {
        int max = 1000;
        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue;
            try {
                int id = Integer.parseInt(cellValue(row, COL_ID));
                if (id > max) max = id;
            } catch (NumberFormatException ignored) {
                // ignore bad cells
            }
        }
        return max + 1;
    }

    /** Maps one Excel row into a User object. */
    private User rowToUser(Row row) {
        User user = new User();
        user.setUserId(parseInt(cellValue(row, COL_ID)));
        user.setName(cellValue(row, COL_NAME));
        user.setEmail(cellValue(row, COL_EMAIL));
        user.setPassword(cellValue(row, COL_PASSWORD));
        user.setPhone(cellValue(row, COL_PHONE));
        user.setAddress(cellValue(row, COL_ADDRESS));
        user.setCreatedAt(cellValue(row, COL_CREATED_AT));
        return user;
    }

    /** Safely reads a cell as a String, whatever its stored type is. */
    private String cellValue(Row row, int col) {
        if (row.getCell(col) == null) {
            return "";
        }
        CellType type = row.getCell(col).getCellType();
        switch (type) {
            case STRING:
                return row.getCell(col).getStringCellValue();
            case NUMERIC:
                return String.valueOf((long) row.getCell(col).getNumericCellValue());
            default:
                return "";
        }
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
