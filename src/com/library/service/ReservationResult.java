package com.library.service;

import com.library.model.Reservation;

/**
 * 預約結果，供 UI 顯示。
 *
 * @param reservation 建立的預約紀錄
 * @param bookTitle   書籍名稱
 * @param memberName  會員姓名
 */
public record ReservationResult(Reservation reservation, String bookTitle, String memberName) {
}
