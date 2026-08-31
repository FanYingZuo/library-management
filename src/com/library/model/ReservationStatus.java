package com.library.model;

/**
 * 預約狀態列舉。
 */
public enum ReservationStatus {

    /** 預約中（有效預約，等待取書或正在排隊）。 */
    PENDING("預約中"),

    /** 已完成借閱（預約已兌現）。 */
    FULFILLED("已借出"),

    /** 已取消預約。 */
    CANCELLED("已取消");

    private final String label;

    ReservationStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
