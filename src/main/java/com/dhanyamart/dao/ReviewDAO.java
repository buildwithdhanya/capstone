package com.dhanyamart.dao;

import com.dhanyamart.model.Review;
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
 * Data Access Object - every operation that touches reviews.xlsx goes through
 * this class. A customer can rate a product once; a second rating replaces
 * the first (same product + user combination).
 */
public class ReviewDAO {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Column indexes must match ExcelUtil.REVIEW_HEADERS
    private static final int COL_ID = 0;
    private static final int COL_PRODUCT_ID = 1;
    private static final int COL_USER_ID = 2;
    private static final int COL_USER_NAME = 3;
    private static final int COL_RATING = 4;
    private static final int COL_COMMENT = 5;
    private static final int COL_CREATED_AT = 6;

    /** Validates and adds (or replaces) a review. Returns true on success. */
    public boolean addOrUpdate(Review review) {
        if (review.getRating() < 1 || review.getRating() > 5) return false;
        synchronized (ExcelUtil.LOCK) {
            try (Workbook wb = ExcelUtil.openOrCreate("reviews.xlsx", "reviews", ExcelUtil.REVIEW_HEADERS)) {
                Sheet sheet = wb.getSheetAt(0);
                for (Row row : sheet) {
                    if (row.getRowNum() == 0) continue;
                    if (CellUtil.asInt(row, COL_USER_ID) == review.getUserId()
                            && CellUtil.asInt(row, COL_PRODUCT_ID) == review.getProductId()) {
                        // Existing review -> replace rating/comment.
                        row.getCell(COL_RATING).setCellValue(review.getRating());
                        row.getCell(COL_COMMENT).setCellValue(review.getComment());
                        row.getCell(COL_CREATED_AT).setCellValue(LocalDateTime.now().format(TIMESTAMP));
                        ExcelUtil.saveReviews(wb);
                        return true;
                    }
                }
                int newId = nextId(sheet);
                Row row = sheet.createRow(sheet.getLastRowNum() + 1);
                row.createCell(COL_ID).setCellValue(newId);
                row.createCell(COL_PRODUCT_ID).setCellValue(review.getProductId());
                row.createCell(COL_USER_ID).setCellValue(review.getUserId());
                row.createCell(COL_USER_NAME).setCellValue(review.getUserName());
                row.createCell(COL_RATING).setCellValue(review.getRating());
                row.createCell(COL_COMMENT).setCellValue(review.getComment());
                row.createCell(COL_CREATED_AT).setCellValue(LocalDateTime.now().format(TIMESTAMP));
                ExcelUtil.saveReviews(wb);
                return true;
            } catch (IOException e) {
                throw new RuntimeException("Could not write reviews.xlsx", e);
            }
        }
    }

    /** All reviews for one product, newest first. */
    public List<Review> listByProduct(int productId) {
        List<Review> result = new ArrayList<>();
        synchronized (ExcelUtil.LOCK) {
            try (Workbook wb = ExcelUtil.openOrCreate("reviews.xlsx", "reviews", ExcelUtil.REVIEW_HEADERS)) {
                for (Row row : wb.getSheetAt(0)) {
                    if (row.getRowNum() == 0) continue;
                    if (CellUtil.asInt(row, COL_PRODUCT_ID) == productId) {
                        result.add(rowToReview(row));
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not read reviews.xlsx", e);
            }
        }
        Collections.reverse(result);
        return result;
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

    private Review rowToReview(Row row) {
        Review r = new Review();
        r.setReviewId(CellUtil.asInt(row, COL_ID));
        r.setProductId(CellUtil.asInt(row, COL_PRODUCT_ID));
        r.setUserId(CellUtil.asInt(row, COL_USER_ID));
        r.setUserName(CellUtil.str(row, COL_USER_NAME));
        r.setRating(CellUtil.asInt(row, COL_RATING));
        r.setComment(CellUtil.str(row, COL_COMMENT));
        r.setCreatedAt(CellUtil.str(row, COL_CREATED_AT));
        return r;
    }
}