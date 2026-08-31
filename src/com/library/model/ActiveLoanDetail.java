package com.library.model;

import java.time.LocalDate;

/**
 * 未歸還借閱詳細資料（供 UI 完整呈現書名與會員姓名）。
 *
 * @param id         借閱單 ID
 * @param bookId     書籍 ID
 * @param bookTitle  書名
 * @param memberId   會員 ID
 * @param memberName 會員姓名
 * @param loanDate   借閱日期
 * @param dueDate    應還日期
 */
public record ActiveLoanDetail(
        long id,
        long bookId,
        String bookTitle,
        long memberId,
        String memberName,
        LocalDate loanDate,
        LocalDate dueDate
) {
}
