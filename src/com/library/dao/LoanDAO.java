package com.library.dao;

import com.library.exception.DataAccessException;
import com.library.model.Loan;
import com.library.model.MemberRankingRow;
import com.library.model.MemberType;
import com.library.model.OverdueReportRow;

import java.math.BigDecimal;
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
 * 圖書館系統：借閱紀錄資料存取物件（LoanDAO）
 * 
 * 職責說明：
 * 專責於 Loan 物件與資料庫借閱資料列之間的 CRUD 轉換。
 * 同時包含系統中的進階統計與報表查詢功能（如 F6 逾期報表與會員借閱排行）。
 */
public class LoanDAO {

    /**
     * 新增借閱紀錄。
     * 支援 F3 借書：三道檢查 ＋ 到期日 ＋ 份數 −1
     * 執行 SQL 寫入後，會自動取得資料庫產生的自動遞增主鍵（ID），
     * 回填到傳入的 Loan 物件中，並將該物件回傳。
     * 內部妥善處理了 Java 的 LocalDate 與資料庫 java.sql.Date 的互轉，
     * 同時針對可能為 null 的還書日（未還書時）與罰款進行安全防呆。
     * 
     * @param loan 包含借閱資訊的 Loan 物件
     * @return 寫入完成且已回填主鍵 ID 的 Loan 物件
     */
    public Loan insert(Loan loan) {
        String sql = """
                INSERT INTO loans (book_id, member_id, loan_date, due_date, return_date, fine)
                VALUES (?, ?, ?, ?, ?, ?)""";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, loan.getBookId());
            ps.setLong(2, loan.getMemberId());
            ps.setDate(3, Date.valueOf(loan.getLoanDate()));
            ps.setDate(4, Date.valueOf(loan.getDueDate()));
            // 尚未還書時 return_date 為 null，若直接轉 Date 會報錯，故需先判斷
            ps.setDate(5, loan.getReturnDate() == null
                    ? null : Date.valueOf(loan.getReturnDate()));
            ps.setBigDecimal(6, loan.getFine());
            ps.executeUpdate();
            
            // 取得資料庫自動產生的主鍵並回填
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    loan.setId(keys.getLong(1));
                }
            }
            return loan;
        } catch (SQLException e) {
            throw new DataAccessException("新增借閱紀錄失敗", e);
        }
    }

    /**
     * 更新借閱紀錄。
     * 通常於書籍「歸還」時使用，負責將實際還書日期（return_date）與計算出的罰款金額（fine）寫入資料庫。
     * 支援 F4 還書：逾期判斷 ＋ 罰金 ＋ 份數 ＋1
     * 
     * @param loan 準備更新的借閱紀錄物件
     */
    public void update(Loan loan) {
        String sql = "UPDATE loans SET return_date = ?, fine = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, loan.getReturnDate() == null
                    ? null : Date.valueOf(loan.getReturnDate()));
            ps.setBigDecimal(2, loan.getFine());
            ps.setLong(3, loan.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("更新借閱紀錄失敗", e);
        }
    }

    /**
     * 更新借閱紀錄的應還期限（到期日）。
     * 用於會員「續借」成功時延長借期。
     *
     * @param loanId     借閱單 ID
     * @param newDueDate 新的應還期限
     */
    public void updateDueDate(long loanId, java.time.LocalDate newDueDate) {
        String sql = "UPDATE loans SET due_date = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(newDueDate));
            ps.setLong(2, loanId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("更新借閱到期日失敗", e);
        }
    }

    /**
     * 依借閱編號查詢「尚未歸還」的借閱紀錄。
     * 條件限定 return_date IS NULL，確保只找進行中的借閱。
     * 
     * @param loanId 借閱紀錄主鍵 ID
     * @return 包含 Loan 的 Optional 物件，若找不到或已還書則回傳 empty
     */
    public Optional<Loan> findActiveById(long loanId) {
        String sql = "SELECT * FROM loans WHERE id = ? AND return_date IS NULL";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, loanId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("查詢借閱紀錄失敗", e);
        }
    }

    /**
     * 查詢特定會員當前是否正在借閱特定書籍（未還）。
     *
     * @param bookId   書籍 ID
     * @param memberId 會員 ID
     * @return 包含 Loan 的 Optional 物件
     */
    public Optional<Loan> findActiveByBookAndMember(long bookId, long memberId) {
        String sql = "SELECT * FROM loans WHERE book_id = ? AND member_id = ? AND return_date IS NULL";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            ps.setLong(2, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("查詢會員特定借閱失敗", e);
        }
    }

    /**
     * 統計特定會員目前「未還書」的總數量。
     * 用於 Service 層進行會員借閱上限的防呆檢查。
     * 
     * @param memberId 會員主鍵 ID
     * @return 尚未歸還的書籍總冊數
     */
    public int countActiveByMember(long memberId) {
        String sql = "SELECT COUNT(*) FROM loans "
                + "WHERE member_id = ? AND return_date IS NULL";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("統計借閱數失敗", e);
        }
    }

    /**
     * 檢查會員是否有「逾期未還」的書籍。
     * 條件：未歸還（return_date IS NULL）且到期日早於今天（due_date < CURDATE()）。
     * 效能優化：使用 SELECT 1 與 LIMIT 1，只要資料庫查到第一筆違規就立刻回傳，不需掃描整張表。
     * 
     * @param memberId 會員主鍵 ID
     * @return 若有逾期未還則回傳 true，否則回傳 false
     */
    public boolean hasOverdue(long memberId) {
        String sql = "SELECT 1 FROM loans "
                + "WHERE member_id = ? AND return_date IS NULL AND due_date < CURDATE() "
                + "LIMIT 1";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("查詢逾期狀態失敗", e);
        }
    }

    /**
     * 查詢全館所有「尚未歸還」的借閱紀錄。
     * 結果集會依書籍到期日（due_date）由近到遠進行排序。
     * 
     * @return 進行中的借閱清單
     */
    public List<Loan> findAllActive() {
        String sql = "SELECT * FROM loans WHERE return_date IS NULL ORDER BY due_date";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Loan> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("列出借閱紀錄失敗", e);
        }
    }

    /**
     * 查詢全館所有「尚未歸還」的借閱詳細紀錄（JOIN 書籍與會員以取得書名與姓名）。
     */
    public List<com.library.model.ActiveLoanDetail> findAllActiveDetails() {
        String sql = """
                SELECT l.id, l.book_id, b.title AS book_title,
                       l.member_id, m.name AS member_name,
                       l.loan_date, l.due_date
                FROM loans l
                JOIN books b   ON b.id = l.book_id
                JOIN members m ON m.id = l.member_id
                WHERE l.return_date IS NULL
                ORDER BY l.due_date, l.id""";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<com.library.model.ActiveLoanDetail> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new com.library.model.ActiveLoanDetail(
                        rs.getLong("id"),
                        rs.getLong("book_id"),
                        rs.getString("book_title"),
                        rs.getLong("member_id"),
                        rs.getString("member_name"),
                        rs.getDate("loan_date").toLocalDate(),
                        rs.getDate("due_date").toLocalDate()));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("列出未歸還詳細借閱紀錄失敗", e);
        }
    }

    // ── 報表查詢（F6 報表：逾期清單 ＋ 借閱排行）────────────────────────────────────────

    /**
     * 產生逾期借閱報表。
     * 透過 SQL JOIN 串聯借閱表、藏書表與會員表，精準篩選出未還且逾期的紀錄；
     * 同時利用資料庫內建的 DATEDIFF 函數計算逾期天數，並依逾期天數由多到少排序。
     * 
     * @return 包含會員姓名、書名、到期日與逾期天數的報表列清單
     */
    public List<OverdueReportRow> overdueReport() {
        String sql = """
                SELECT m.name AS member_name,
                       b.title AS book_title,
                       l.due_date AS due_date,
                       DATEDIFF(CURDATE(), l.due_date) AS overdue_days
                FROM loans l
                JOIN books b   ON b.id = l.book_id
                JOIN members m ON m.id = l.member_id
                WHERE l.return_date IS NULL
                  AND l.due_date < CURDATE()
                ORDER BY overdue_days DESC""";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<OverdueReportRow> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new OverdueReportRow(
                        rs.getString("member_name"),
                        rs.getString("book_title"),
                        rs.getDate("due_date").toLocalDate().toString(),
                        rs.getLong("overdue_days")));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("產生逾期報表失敗", e);
        }
    }

    /**
     * 產生會員借閱排行榜。
     * 透過 JOIN 與 GROUP BY 統計每位會員的總借閱次數，
     * 依借閱次數由多到少排序，並透過帶入參數的 LIMIT 限制回傳的前 N 名數量。
     * 
     * @param limit 要查詢的前幾名（例如前 10 名）
     * @return 包含會員姓名、會員類型中文標籤與借閱次數的排行清單
     */
    public List<MemberRankingRow> memberRanking(int limit) {
        String sql = """
                SELECT m.name AS member_name,
                       m.type AS member_type,
                       COUNT(*) AS loan_count
                FROM loans l
                JOIN members m ON m.id = l.member_id
                GROUP BY m.id, m.name, m.type
                ORDER BY loan_count DESC
                LIMIT ?""";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<MemberRankingRow> list = new ArrayList<>();
                while (rs.next()) {
                    MemberType type = MemberType.valueOf(rs.getString("member_type"));
                    list.add(new MemberRankingRow(
                            rs.getString("member_name"),
                            type.label(),
                            rs.getLong("loan_count")));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new DataAccessException("產生借閱排行失敗", e);
        }
    }

    /**
     * 共用私有方法：資料列對應器（Row Mapper）。
     * 將資料庫 ResultSet 游標當前列轉換為 Loan 物件。
     * 內部特別針對可能為 null 的還書日（return_date）與罰款（fine）做安全的防呆檢查：
     * - 還書日若為 null 則保持 null。
     * - 罰款若為 null 則自動給予預設值 BigDecimal.ZERO。
     * 
     * @param rs 資料庫查詢結果集
     * @return 轉換完成的 Loan 物件
     * @throws SQLException 若欄位讀取失敗時拋出
     */
    private Loan mapRow(ResultSet rs) throws SQLException {
        Date ret = rs.getDate("return_date");
        BigDecimal fine = rs.getBigDecimal("fine");
        return new Loan(
                rs.getLong("id"),
                rs.getLong("book_id"),
                rs.getLong("member_id"),
                rs.getDate("loan_date").toLocalDate(),
                rs.getDate("due_date").toLocalDate(),
                ret == null ? null : ret.toLocalDate(),
                fine == null ? BigDecimal.ZERO : fine);
    }
}
