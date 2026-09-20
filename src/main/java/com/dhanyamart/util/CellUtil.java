package com.dhanyamart.util;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

import java.math.BigDecimal;

/**
 * Shared helpers for reading Excel cells. Excel stores numbers as doubles,
 * so an ID written as 1001 comes back as 1001.0; this class converts it to a
 * clean string ("1001") while preserving decimals for prices ("149.50").
 */
public class CellUtil {

    private CellUtil() {
    }

    /** Reads any cell as a String. Missing cells -> "". */
    public static String str(Row row, int col) {
        if (row.getCell(col) == null) {
            return "";
        }
        CellType type = row.getCell(col).getCellType();
        switch (type) {
            case STRING:
                return row.getCell(col).getStringCellValue();
            case NUMERIC:
                return num(row.getCell(col).getNumericCellValue());
            default:
                return "";
        }
    }

    /** Formats a double cleanly: 1001.0 -> "1001", 149.5 -> "149.5". */
    public static String num(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    /** Parses an int cell safely, returning 0 on empty/blank/invalid values. */
    public static int asInt(Row row, int col) {
        return parseInt(str(row, col));
    }

    /** Parses a double cell safely, returning 0.0 on invalid values. */
    public static double asDouble(Row row, int col) {
        try {
            return Double.parseDouble(str(row, col));
        } catch (Exception e) {
            return 0.0;
        }
    }

    /** Parses a String as an int, returning fallback (0) on failure. */
    public static int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    /** Parses a String as a double, returning fallback (0.0) on failure. */
    public static double parseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return 0.0;
        }
    }
}