package com.library.model;

import java.time.LocalDate;

/**
 * 預約紀錄實體，對應資料表 {@code reservations}。
 */
public class Reservation {

    private long id;
    private long bookId;
    private long memberId;
    private LocalDate reserveDate;
    private ReservationStatus status = ReservationStatus.PENDING;

    public Reservation() {
    }

    /** 新增預約用：尚未有 id。 */
    public Reservation(long bookId, long memberId, LocalDate reserveDate, ReservationStatus status) {
        this.bookId = bookId;
        this.memberId = memberId;
        this.reserveDate = reserveDate;
        this.status = status == null ? ReservationStatus.PENDING : status;
    }

    /** 從資料庫載入用：完整欄位。 */
    public Reservation(long id, long bookId, long memberId, LocalDate reserveDate, ReservationStatus status) {
        this.id = id;
        this.bookId = bookId;
        this.memberId = memberId;
        this.reserveDate = reserveDate;
        this.status = status == null ? ReservationStatus.PENDING : status;
    }

    public boolean isPending() {
        return this.status == ReservationStatus.PENDING;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getBookId() {
        return bookId;
    }

    public void setBookId(long bookId) {
        this.bookId = bookId;
    }

    public long getMemberId() {
        return memberId;
    }

    public void setMemberId(long memberId) {
        this.memberId = memberId;
    }

    public LocalDate getReserveDate() {
        return reserveDate;
    }

    public void setReserveDate(LocalDate reserveDate) {
        this.reserveDate = reserveDate;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return String.format("[預約 #%d] 書籍 id=%d, 會員 id=%d, 預約日期=%s, 狀態=%s",
                id, bookId, memberId, reserveDate, status.label());
    }
}
