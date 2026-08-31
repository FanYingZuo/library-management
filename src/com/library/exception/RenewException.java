package com.library.exception;

/**
 * 續借失敗例外。
 * 當續借條件不符時拋出（如：距離到期日大於 3 天、書籍已逾期、書籍已被他人預約等）。
 */
public class RenewException extends LibraryException {

    private static final long serialVersionUID = 1L;

    public RenewException(String message) {
        super(message);
    }
}
