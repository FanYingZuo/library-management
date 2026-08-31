package com.library.service;

import com.library.model.Loan;
import java.time.LocalDate;

/**
 * 續借結果，供 UI 顯示。
 *
 * @param loan            已更新到期日的借閱紀錄
 * @param previousDueDate 原到期日
 * @param newDueDate      新到期日
 * @param extendedDays    本次延長天數
 */
public record RenewResult(Loan loan, LocalDate previousDueDate, LocalDate newDueDate, int extendedDays) {
}
