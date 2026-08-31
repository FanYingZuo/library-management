package com.library.exception;

/**
 * 預約失敗例外。
 * 當預約條件不符時拋出（如：尚有在庫可借份數、使用者已借閱或重複預約等）。
 */
public class ReservationException extends LibraryException {

    private static final long serialVersionUID = 1L;

    public ReservationException(String message) {
        super(message);
    }
}
