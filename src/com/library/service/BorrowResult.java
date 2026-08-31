package com.library.service;

import com.library.model.Loan;

/**
 * 借書結果，供 UI 顯示。
 *
 * @param loan       建立的借閱紀錄
 * @param bookTitle  書籍名稱
 * @param memberName 會員姓名
 */
public record BorrowResult(Loan loan, String bookTitle, String memberName) {
}
