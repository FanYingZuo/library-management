package com.library.dao;

import com.library.exception.DataAccessException;
import com.library.model.Reservation;
import com.library.model.ReservationStatus;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 預約紀錄資料存取物件（ReservationDAO）。
 * 專責於 Reservation 物件與資料庫 reservations 資料表之間的 CRUD 與狀態管理。
 */
public class ReservationDAO {

    /**
     * 新增預約紀錄。
     * 寫入後自動回填主鍵 ID。
     *
     * @param reservation 準備新增的預約物件
     * @return 已回填 ID 的預約物件
     */
    public Reservation insert(Reservation reservation) {
        String sql = """
                INSERT INTO reservations (book_id, member_id, reserve_date, status)
                VALUES (?, ?, ?, ?)""";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, reservation.getBookId());
            ps.setLong(2, reservation.getMemberId());
            ps.setDate(3, Date.valueOf(reservation.getReserveDate()));
            ps.setString(4, reservation.getStatus().name());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    reservation.setId(keys.getLong(1));
                }
            }
            return reservation;
        } catch (SQLException e) {
            throw new DataAccessException("新增預約紀錄失敗", e);
        }
    }

    /**
     * 更新預約狀態（如取消或借出完成）。
     *
     * @param reservationId 預約紀錄 ID
     * @param status        新狀態
     */
    public void updateStatus(long reservationId, ReservationStatus status) {
        String sql = "UPDATE reservations SET status = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, reservationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("更新預約狀態失敗", e);
        }
    }

    /**
     * 依主鍵 ID 查詢預約紀錄。
     */
    public Optional<Reservation> findById(long id) {
        String sql = "SELECT * FROM reservations WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("查詢預約紀錄失敗", e);
        }
    }

    /**
     * 檢查指定書籍是否已被「其他讀者」預約中（PENDING）。
     * 常用於續借前防呆：若有其他人預約則禁止續借。
     *
     * @param bookId   書籍 ID
     * @param memberId 當前操作或借閱該書的會員 ID
     * @return 若有其他會員正在預約中則回傳 true
     */
    public boolean hasPendingReservationByOther(long bookId, long memberId) {
        String sql = "SELECT 1 FROM reservations WHERE book_id = ? AND member_id != ? AND status = 'PENDING' LIMIT 1";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            ps.setLong(2, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("查詢他人預約狀態失敗", e);
        }
    }

    /**
     * 查詢特定會員對特定書籍是否存在「預約中（PENDING）」的預約。
     * 用於防止同一位會員重複預約同一本書。
     */
    public Optional<Reservation> findPendingByBookAndMember(long bookId, long memberId) {
        String sql = "SELECT * FROM reservations WHERE book_id = ? AND member_id = ? AND status = 'PENDING'";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            ps.setLong(2, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("查詢會員預約記錄失敗", e);
        }
    }

    /**
     * 統計特定書籍目前有效預約（PENDING）的總數量。
     */
    public int countPendingByBook(long bookId) {
        String sql = "SELECT COUNT(*) FROM reservations WHERE book_id = ? AND status = 'PENDING'";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("統計書籍預約數失敗", e);
        }
    }

    /**
     * 查詢全館所有「預約中（PENDING）」的紀錄。
     */
    public List<Reservation> findAllPending() {
        String sql = "SELECT * FROM reservations WHERE status = 'PENDING' ORDER BY reserve_date, id";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Reservation> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("查詢預約清單失敗", e);
        }
    }

    /**
     * 查詢全館所有「預約中（PENDING）」的詳細紀錄（JOIN 書籍與會員以取得書名與姓名）。
     */
    public List<com.library.model.ReservationDetail> findAllPendingDetails() {
        String sql = """
                SELECT r.id, r.book_id, b.title AS book_title,
                       r.member_id, m.name AS member_name,
                       r.reserve_date, r.status
                FROM reservations r
                JOIN books b   ON b.id = r.book_id
                JOIN members m ON m.id = r.member_id
                WHERE r.status = 'PENDING'
                ORDER BY r.reserve_date, r.id""";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<com.library.model.ReservationDetail> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new com.library.model.ReservationDetail(
                        rs.getLong("id"),
                        rs.getLong("book_id"),
                        rs.getString("book_title"),
                        rs.getLong("member_id"),
                        rs.getString("member_name"),
                        rs.getDate("reserve_date").toLocalDate(),
                        ReservationStatus.valueOf(rs.getString("status"))));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("查詢預約詳細清單失敗", e);
        }
    }

    /**
     * 查詢全館所有預約紀錄。
     */
    public List<Reservation> findAll() {
        String sql = "SELECT * FROM reservations ORDER BY reserve_date DESC, id DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Reservation> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("查詢歷史預約失敗", e);
        }
    }

    /**
     * 共用私有方法：資料列對應器（Row Mapper）。
     */
    private Reservation mapRow(ResultSet rs) throws SQLException {
        return new Reservation(
                rs.getLong("id"),
                rs.getLong("book_id"),
                rs.getLong("member_id"),
                rs.getDate("reserve_date").toLocalDate(),
                ReservationStatus.valueOf(rs.getString("status")));
    }
}
