package com.dhanyamart.util;

import java.math.BigDecimal;

/**
 * Shared string->number helpers used by the servlets when parsing request
 * parameters. (The Excel cell helpers that used to live here were removed
 * when the project moved to MySQL storage.)
 */
public class CellUtil {

    private CellUtil() {
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

    /** Formats a double cleanly: 1001.0 -> "1001", 149.5 -> "149.5". */
    public static String num(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}