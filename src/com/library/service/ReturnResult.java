package com.library.service;

import com.library.model.Loan;

import java.math.BigDecimal;

/**
 * 還書結果，供 UI 顯示。
 *
 * @param loan        已結算的借閱紀錄
 * @param overdueDays 逾期天數（未逾期為 0）
 * @param fine        罰金金額
 */
public record ReturnResult(Loan loan, long overdueDays, BigDecimal fine) {
    /**
     * Java Record 核心：
     * 本質作用： 專門用來封裝與傳遞資料的唯讀資料載體（Immutable DTO）。
     * 最大優勢： 只要一行宣告，編譯器即自動生成建構子、Getter、toString()、equals() 與 hashCode()。
     * 資料安全： 所有欄位預設均為 final（不可變），有效防止資料在傳遞過程中被意外竄改。
     */
    // 是否逾期。
    public boolean isOverdue() {
        return overdueDays > 0;
    }
}
