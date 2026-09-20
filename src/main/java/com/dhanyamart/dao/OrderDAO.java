package com.dhanyamart.dao;

import com.dhanyamart.model.Order;
import com.dhanyamart.model.OrderItem;
import com.dhanyamart.util.CellUtil;
import com.dhanyamart.util.ExcelUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data Access Object - every operation that touches orders.xlsx goes through
 * this class. Each order stores a snapshot of its line items in the "items"
 * cell using the format  productId|name|price|qty;  repeated.
 */
public class OrderDAO {

    public static final String STATUS_PLACED = "PLACED";
    public static final String STATUS_PACKED = "PACKED";
    public static final String STATUS_SHIPPED = "SHIPPED";
    public static final String STATUS_DELIVERED = "DELIVERED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    public static final String[] STATUSES = {
            STATUS_PLACED, STATUS_PACKED, STATUS_SHIPPED, STATUS_DELIVERED, STATUS_CANCELLED
    };

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String ITEM_SEP = ";";
    private static final String FIELD_SEP = "\\|";

    // Column indexes must match ExcelUtil.ORDER_HEADERS
    private static final int COL_ID = 0;
    private static final int COL_USER_ID = 1;
    private static final int COL_USER_NAME = 2;
    private static final int COL_USER_EMAIL = 3;
    private static final int COL_PHONE = 4;
    private static final int COL_ADDRESS = 5;
    private static final int COL_ORDER_DATE = 6;
    private static final int COL_STATUS = 7;
    private static final int COL_TOTAL = 8;
    private static final int COL_ITEMS = 9;

    /** Valid flow statuses a customer or admin may set. */
    public static boolean isValidStatus(String status) {
        if (status == null) return false;
        for (String s : STATUSES) {
            if (s.equals(status)) return true;
        }
        return false;
    }

    /** Saves a new order and returns its generated order_id. */
    public int create(Order order) {
        synchronized (ExcelUtil.LOCK) {
            try (Workbook wb = ExcelUtil.openOrCreate("orders.xlsx", "orders", ExcelUtil.ORDER_HEADERS)) {
                Sheet sheet = wb.getSheetAt(0);
                int newId = nextId(sheet);
                Row row = sheet.createRow(sheet.getLastRowNum() + 1);

                row.createCell(COL_ID).setCellValue(newId);
                row.createCell(COL_USER_ID).setCellValue(order.getUserId());
                row.createCell(COL_USER_NAME).setCellValue(order.getUserName());
                row.createCell(COL_USER_EMAIL).setCellValue(order.getUserEmail());
                row.createCell(COL_PHONE).setCellValue(order.getPhone());
                row.createCell(COL_ADDRESS).setCellValue(order.getAddress());
                row.createCell(COL_ORDER_DATE).setCellValue(LocalDateTime.now().format(TIMESTAMP));
                row.createCell(COL_STATUS).setCellValue(STATUS_PLACED);
                row.createCell(COL_TOTAL).setCellValue(order.getTotal());
                row.createCell(COL_ITEMS).setCellValue(encodeItems(order.getItems()));

                ExcelUtil.saveOrders(wb);
                return newId;
            } catch (IOException e) {
                throw new RuntimeException("Could not write orders.xlsx", e);
            }
        }
    }

    /** Orders placed by one customer, newest first. */
    public List<Order> listByUser(int userId) {
        List<Order> result = new ArrayList<>();
        synchronized (ExcelUtil.LOCK) {
            try (Workbook wb = ExcelUtil.openOrCreate("orders.xlsx", "orders", ExcelUtil.ORDER_HEADERS)) {
                for (Row row : wb.getSheetAt(0)) {
                    if (row.getRowNum() == 0) continue;
                    Order o = rowToOrder(row);
                    if (o.getUserId() == userId) result.add(o);
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not read orders.xlsx", e);
            }
        }
        Collections.reverse(result);
        return result;
    }

    /** Every order in the system (admin dashboard), newest first. */
    public List<Order> listAll() {
        List<Order> result = new ArrayList<>();
        synchronized (ExcelUtil.LOCK) {
            try (Workbook wb = ExcelUtil.openOrCreate("orders.xlsx", "orders", ExcelUtil.ORDER_HEADERS)) {
                for (Row row : wb.getSheetAt(0)) {
                    if (row.getRowNum() == 0) continue;
                    result.add(rowToOrder(row));
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not read orders.xlsx", e);
            }
        }
        Collections.reverse(result);
        return result;
    }

    public Order findById(int orderId) {
        synchronized (ExcelUtil.LOCK) {
            try (Workbook wb = ExcelUtil.openOrCreate("orders.xlsx", "orders", ExcelUtil.ORDER_HEADERS)) {
                for (Row row : wb.getSheetAt(0)) {
                    if (row.getRowNum() == 0) continue;
                    if (CellUtil.asInt(row, COL_ID) == orderId) {
                        return rowToOrder(row);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not read orders.xlsx", e);
            }
        }
        return null;
    }

    public boolean updateStatus(int orderId, String status) {
        if (!isValidStatus(status)) return false;
        synchronized (ExcelUtil.LOCK) {
            try (Workbook wb = ExcelUtil.openOrCreate("orders.xlsx", "orders", ExcelUtil.ORDER_HEADERS)) {
                Sheet sheet = wb.getSheetAt(0);
                for (Row row : sheet) {
                    if (row.getRowNum() == 0) continue;
                    if (CellUtil.asInt(row, COL_ID) == orderId) {
                        row.getCell(COL_STATUS).setCellValue(status);
                        ExcelUtil.saveOrders(wb);
                        return true;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not write orders.xlsx", e);
            }
        }
        return false;
    }

    /** Total value of all non-cancelled orders (admin revenue figure). */
    public double totalRevenue() {
        double sum = 0.0;
        synchronized (ExcelUtil.LOCK) {
            try (Workbook wb = ExcelUtil.openOrCreate("orders.xlsx", "orders", ExcelUtil.ORDER_HEADERS)) {
                for (Row row : wb.getSheetAt(0)) {
                    if (row.getRowNum() == 0) continue;
                    if (!STATUS_CANCELLED.equals(CellUtil.str(row, COL_STATUS))) {
                        sum += CellUtil.asDouble(row, COL_TOTAL);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not read orders.xlsx", e);
            }
        }
        return sum;
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

    private String encodeItems(List<OrderItem> items) {
        StringBuilder sb = new StringBuilder();
        for (OrderItem item : items) {
            sb.append(item.getProductId()).append('|')
              .append(item.getProductName()).append('|')
              .append(item.getPrice()).append('|')
              .append(item.getQuantity()).append(ITEM_SEP);
        }
        return sb.toString();
    }

    private List<OrderItem> decodeItems(String raw) {
        List<OrderItem> items = new ArrayList<>();
        if (raw == null || raw.isBlank()) return items;
        for (String part : raw.split(ITEM_SEP)) {
            if (part.isBlank()) continue;
            String[] f = part.split(FIELD_SEP);
            if (f.length < 4) continue;
            OrderItem item = new OrderItem();
            item.setProductId(CellUtil.parseInt(f[0]));
            item.setProductName(f[1]);
            item.setPrice(CellUtil.parseDouble(f[2]));
            item.setQuantity(CellUtil.parseInt(f[3]));
            items.add(item);
        }
        return items;
    }

    private Order rowToOrder(Row row) {
        Order order = new Order();
        order.setOrderId(CellUtil.asInt(row, COL_ID));
        order.setUserId(CellUtil.asInt(row, COL_USER_ID));
        order.setUserName(CellUtil.str(row, COL_USER_NAME));
        order.setUserEmail(CellUtil.str(row, COL_USER_EMAIL));
        order.setPhone(CellUtil.str(row, COL_PHONE));
        order.setAddress(CellUtil.str(row, COL_ADDRESS));
        order.setOrderDate(CellUtil.str(row, COL_ORDER_DATE));
        order.setStatus(CellUtil.str(row, COL_STATUS));
        order.setTotal(CellUtil.asDouble(row, COL_TOTAL));
        order.setItems(decodeItems(CellUtil.str(row, COL_ITEMS)));
        return order;
    }
}