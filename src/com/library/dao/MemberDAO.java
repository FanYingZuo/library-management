package com.library.dao;

import com.library.exception.DataAccessException;
import com.library.model.Member;
import com.library.model.MemberType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 圖書館系統：會員資料存取物件（MemberDAO）
 * 
 * 職責說明：
 * 專責於 Member 物件與資料庫會員資料列之間的 CRUD 轉換。
 * 所有資料庫操作一律使用 PreparedStatement 佔位符執行，確保資料庫安全並杜絕 SQL 注入。
 */
public class MemberDAO {

    /**
     * 新增會員記錄。
     * 支援 F2 會員：新增兩種身份 → 查詢
     * 執行 SQL 寫入後，會自動取得資料庫產生的自動遞增主鍵（ID），
     * 並回填到傳入的 Member 物件中。
     * 
     * @param member 準備新增的會員物件
     */
    public void insert(Member member) {
        String sql = """
                INSERT INTO members (member_no, name, type, email)
                VALUES (?, ?, ?, ?)""";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, member.getMemberNo());
            ps.setString(2, member.getName());
            ps.setString(3, member.getType().name());
            ps.setString(4, member.getEmail());
            ps.executeUpdate();
            
            // 取得資料庫自動產生的主鍵並回填到物件中
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    member.setId(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("新增會員失敗", e);
        }
    }

    /**
     * 依資料庫主鍵（ID）查詢單一會員。
     * 內部巧妙複用了私有的 queryOne 方法，實踐 DRY 原則，避免重複寫查詢邏輯。
     * 
     * @param id 會員唯一識別碼
     * @return 包含會員的 Optional 物件，若找不到則回傳 empty
     */
    public Optional<Member> findById(long id) {
        return queryOne("SELECT * FROM members WHERE id = ?", id);
    }

    /**
     * 依會員編號（member_no）查詢會員。
     * 通常用於 Service 層進行會員註冊時的「重複性防呆檢查」。
     * 
     * @param memberNo 會員編號
     * @return 包含會員的 Optional 物件，若找不到則回傳 empty
     */
    public Optional<Member> findByMemberNo(String memberNo) {
        String sql = "SELECT * FROM members WHERE member_no = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, memberNo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("查詢會員失敗", e);
        }
    }

    /**
     * 查詢所有會員清單。
     * 結果集預設會依會員編號（member_no）進行排序。
     * 
     * @return 所有會員的列表
     */
    public List<Member> findAll() {
        String sql = "SELECT * FROM members ORDER BY member_no";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Member> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("列出會員失敗", e);
        }
    }

    /**
     * 共用私要方法：依主鍵進行單筆查詢的通用模板。
     * 將共用的連線、PreparedStatement 設定與 ResultSet 解析封裝在此，
     * 讓像是 findById 這類方法可以直接呼叫，大幅減少重複程式碼。
     * 
     * @param sql 要執行的查詢 SQL 指令
     * @param id  要填入佔位符的會員主鍵 ID
     * @return 包含會員的 Optional 物件
     */
    private Optional<Member> queryOne(String sql, long id) {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("查詢會員失敗", e);
        }
    }

    /**
     * 共用私有方法：資料列對應器（Row Mapper）。
     * 將資料庫 ResultSet 游標當前列的各欄位資料，完整轉換並封裝為 Member 物件。
     * 
     * @param rs 資料庫查詢結果集
     * @return 轉換完成的 Member 物件
     * @throws SQLException 若欄位讀取失敗時拋出
     */
    private Member mapRow(ResultSet rs) throws SQLException {
        return new Member(
                rs.getLong("id"),
                rs.getString("member_no"),
                rs.getString("name"),
                MemberType.valueOf(rs.getString("type")),
                rs.getString("email"));
    }
}