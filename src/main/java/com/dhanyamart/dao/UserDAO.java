package com.dhanyamart.dao;

import com.dhanyamart.model.User;
import com.dhanyamart.util.CellUtil;
import com.dhanyamart.util.ExcelUtil;
import com.dhanyamart.util.PasswordUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object - every operation that touches users.xlsx goes through
 * this class. This is the ONLY class the controllers talk to for user data,
 * so if you later switch to MySQL you only have to change this file
 * (that is where a JDBC connection would plug in).
 *
 * Role column (optional, appended at index 7): CUSTOMER, SELLER or ADMIN.
 * Accounts created through the register form are always CUSTOMER.
 */
public class UserDAO {

    public static final String ROLE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_SELLER = "SELLER";
    public static final String ROLE_ADMIN = "ADMIN";

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Column indexes must match the header order in ExcelUtil.USER_HEADERS
    private static final int COL_ID = 0;
    private static final int COL_NAME = 1;
    private static final int COL_EMAIL = 2;
    private static final int COL_PASSWORD = 3;
    private static final int COL_PHONE = 4;
    private static final int COL_ADDRESS = 5;
    private static final int COL_CREATED_AT = 6;
    private static final int COL_ROLE = 7;

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
                    if (email.equalsIgnoreCase(CellUtil.str(row, COL_EMAIL))) {
                        return rowToUser(row);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not read users.xlsx", e);
            }
        }
        return null;
    }

    /** Finds a user by numeric id. */
    public User findById(int userId) {
        synchronized (ExcelUtil.LOCK) {
            try (Workbook wb = ExcelUtil.openOrCreate()) {
                for (Row row : wb.getSheetAt(0)) {
                    if (row.getRowNum() == 0) continue;
                    if (CellUtil.asInt(row, COL_ID) == userId) {
                        User u = rowToUser(row);
                        u.setPassword(null); // never pass the hash around
                        return u;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not read users.xlsx", e);
            }
        }
        return null;
    }

    /** Returns every user saved in users.xlsx (safest for the admin dashboard). */
    public List<User> listAll() {
        List<User> result = new ArrayList<>();
        synchronized (ExcelUtil.LOCK) {
            try (Workbook wb = ExcelUtil.openOrCreate()) {
                for (Row row : wb.getSheetAt(0)) {
                    if (row.getRowNum() == 0) continue;
                    User u = rowToUser(row);
                    u.setPassword(null);
                    result.add(u);
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not read users.xlsx", e);
            }
        }
        return result;
    }

    /** Returns the role ("CUSTOMER" when a row has none, e.g. legacy data). */
    public String getRole(int userId) {
        synchronized (ExcelUtil.LOCK) {
            try (Workbook wb = ExcelUtil.openOrCreate()) {
                for (Row row : wb.getSheetAt(0)) {
                    if (row.getRowNum() == 0) continue;
                    if (CellUtil.asInt(row, COL_ID) == userId) {
                        return roleOrDefault(CellUtil.str(row, COL_ROLE));
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not read users.xlsx", e);
            }
        }
        return ROLE_CUSTOMER;
    }

    /** Changes a user's role (admin action). */
    public boolean updateRole(int userId, String role) {
        if (role == null || role.isBlank()) {
            return false;
        }
        String normalized = role.trim().toUpperCase();
        if (!normalized.equals(ROLE_CUSTOMER) && !normalized.equals(ROLE_SELLER)
                && !normalized.equals(ROLE_ADMIN)) {
            return false;
        }
        synchronized (ExcelUtil.LOCK) {
            try (Workbook wb = ExcelUtil.openOrCreate()) {
                Sheet sheet = wb.getSheetAt(0);
                for (Row row : sheet) {
                    if (row.getRowNum() == 0) continue;
                    if (CellUtil.asInt(row, COL_ID) == userId) {
                        row.createCell(COL_ROLE).setCellValue(normalized);
                        ExcelUtil.saveUsers(wb);
                        return true;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not update users.xlsx", e);
            }
        }
        return false;
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

                    if (email.equalsIgnoreCase(CellUtil.str(row, COL_EMAIL))) {
                        String storedHash = CellUtil.str(row, COL_PASSWORD);
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
     * New registrations always get the CUSTOMER role.
     */
    public boolean registerUser(User user) {
        synchronized (ExcelUtil.LOCK) {
            try (Workbook wb = ExcelUtil.openOrCreate()) {
                Sheet sheet = wb.getSheetAt(0);

                // Duplicate email check inside the lock (re-read the file,
                // do not trust a stale read from the servlet).
                for (Row row : sheet) {
                    if (row.getRowNum() == 0) continue;
                    if (user.getEmail().equalsIgnoreCase(CellUtil.str(row, COL_EMAIL))) {
                        return false;
                    }
                }

                ensureHeader(sheet);
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
                row.createCell(COL_ROLE).setCellValue(roleOrDefault(user.getRole()));

                ExcelUtil.saveUsers(wb);
                return true;
            } catch (IOException e) {
                throw new RuntimeException("Could not write users.xlsx", e);
            }
        }
    }

    /** Internal: registers a user with an explicit role (used by the DataSeeder). */
    public boolean registerUser(User user, String role) {
        user.setRole(role);
        return registerUser(user);
    }

    /** If the header row predates the role column, add a "role" header cell. */
    private void ensureHeader(Sheet sheet) {
        Row header = sheet.getRow(0);
        if (header != null && header.getCell(COL_ROLE) == null) {
            Cell cell = header.createCell(COL_ROLE);
            cell.setCellValue("role");
        }
    }

    /** Generates the next user_id = max existing id + 1 (starts at 1001). */
    private int nextUserId(Sheet sheet) {
        int max = 1000;
        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue;
            int id = CellUtil.asInt(row, COL_ID);
            if (id > max) max = id;
        }
        return max + 1;
    }

    /** Maps one Excel row into a User object. */
    private User rowToUser(Row row) {
        User user = new User();
        user.setUserId(CellUtil.asInt(row, COL_ID));
        user.setName(CellUtil.str(row, COL_NAME));
        user.setEmail(CellUtil.str(row, COL_EMAIL));
        user.setPassword(CellUtil.str(row, COL_PASSWORD));
        user.setPhone(CellUtil.str(row, COL_PHONE));
        user.setAddress(CellUtil.str(row, COL_ADDRESS));
        user.setCreatedAt(CellUtil.str(row, COL_CREATED_AT));
        user.setRole(roleOrDefault(CellUtil.str(row, COL_ROLE)));
        return user;
    }

    private String roleOrDefault(String role) {
        if (role == null || role.isBlank()) {
            return ROLE_CUSTOMER;
        }
        return role.toUpperCase();
    }
}