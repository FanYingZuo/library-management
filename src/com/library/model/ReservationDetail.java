package com.library.model;

import java.time.LocalDate;

/**
 * 預約詳細資料（供 UI 完整呈現書名與會員姓名）。
 *
 * @param id          預約單 ID
 * @param bookId      書籍 ID
 * @param bookTitle   書名
 * @param memberId    會員 ID
 * @param memberName  會員姓名
 * @param reserveDate 預約日期
 * @param status      預約狀態
 */
public record ReservationDetail(
        long id,
        long bookId,
        String bookTitle,
        long memberId,
        String memberName,
        LocalDate reserveDate,
        ReservationStatus status
) {
}
