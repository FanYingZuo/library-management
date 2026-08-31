package com.library.dao;

import com.library.exception.DataAccessException;
import com.library.model.Book;
import com.library.model.BookType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 圖書館系統：藏書資料存取物件（BookDAO）
 * 
 * 職責單一化：專責於 Book 物件與資料庫資料列之間的 CRUD 轉換。
 * 不做商業邏輯驗證或重複判斷，所有查詢均透過 PreparedStatement 佔位符執行，確保資料庫安全。
 */
public class BookDAO {

    /**
     * 新增藏書記錄。
     * 支援 F1 藏書：新增 → 列表 → 依 ISBN 查詢
     * 執行 SQL 寫入後，會自動取得資料庫產生的自動遞增主鍵（ID），並回填到傳入的 Book 物件中。
     * 
     * @param book 要新增的藏書物件
     */
    public void insert(Book book) {
        String sql = """
                INSERT INTO books
                    (isbn, title, author, type, total_copies, available_copies)
                VALUES (?, ?, ?, ?, ?, ?)""";
    /**
    *使用 try-with-resources 機制：確保程式結束（或發生例外）時，
    * Java 會自動關閉連線與 PreparedStatement，防止資料庫連線外洩。
    */
    try (Connection conn = DBUtil.getConnection();           // 取得與資料庫的連線
         PreparedStatement ps = conn.prepareStatement(       //PreparedStatement 搭配 ?(佔位符)
                 sql, Statement.RETURN_GENERATED_KEYS)) {    // 準備 SQL 指令，並特別要求資料庫回傳自動遞增的主鍵（ID）
            ps.setString(1, book.getIsbn());
            ps.setString(2, book.getTitle());
            ps.setString(3, book.getAuthor());
            ps.setString(4, book.getType().name());
            ps.setInt(5, book.getTotalCopies());
            ps.setInt(6, book.getAvailableCopies());

            ps.executeUpdate();                              //executeUpdate()用於 INSERT / UPDATE / DELETE，回傳受影響的列數（int）。
//                                                           //executeQuery()用於 SELECT，回傳 ResultSet 結果集供逐列讀取

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    book.setId(keys.getLong(1));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("新增藏書失敗", e);
        }
    }

    /**
     * 依資料庫主鍵（ID）查詢單一藏書。
     * 
     * @param id 藏書唯一識別碼
     * @return 包含藏書的 Optional 物件，若找不到則回傳 empty
     */
    public Optional<Book> findById(long id) {
        String sql = "SELECT * FROM books WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
//                                                           //executeUpdate()用於 INSERT / UPDATE / DELETE，回傳受影響的列數（int）
            try (ResultSet rs = ps.executeQuery()) {         //executeQuery()用於 SELECT，回傳 ResultSet 結果集供逐列讀取
                return rs.next()
                        ? Optional.of(mapRow(rs))
                        : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DataAccessException("查詢藏書失敗", e);
        }
    }

    /**
     * 依國際標準書號（ISBN）查詢藏書。
     * 通常用於 Service 層進行防呆與重複性檢查。
     * 
     * @param isbn 書籍 ISBN
     * @return 包含藏書的 Optional 物件，若找不到則回傳 empty
     */
    public Optional<Book> findByIsbn(String isbn) {
        String sql = "SELECT * FROM books WHERE isbn = ?";

        try (Connection conn = DBUtil.getConnection();     
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, isbn);
//                                                        //executeUpdate()用於 INSERT / UPDATE / DELETE，回傳受影響的列數（int）
            try (ResultSet rs = ps.executeQuery()) {      //executeQuery()用於 SELECT，回傳 ResultSet 結果集供逐列讀取
                return rs.next()
                        ? Optional.of(mapRow(rs))
                        : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DataAccessException("查詢藏書失敗", e);
        }
    }

    /**
     * 查詢所有藏書清單。
     * 結果集預設會依書名字母或筆劃進行排序。
     * 
     * @return 所有藏書的列表
     */
    public List<Book> findAll() {
        String sql = "SELECT * FROM books ORDER BY title";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {                //executeQuery()用於 SELECT，回傳 ResultSet 結果集供逐列讀取
//                                                              //executeUpdate()用於 INSERT / UPDATE / DELETE，回傳受影響的列數（int）
            List<Book> list = new ArrayList<>();

            while (rs.next()) {
                list.add(mapRow(rs));
            }

            return list;

        } catch (SQLException e) {
            throw new DataAccessException("列出藏書失敗", e);
        }
    }

    /**
     * 依書名、作者或書籍類型進行組合條件查詢（支援 F5 查詢：書名／作者／類型任意組合）。
     * 任一參數若為 null 或空字串，代表該條件不設限。
     * 內部採用動態 SQL 拼接骨架，但查詢數值與關鍵字一律走佔位符傳遞，杜絕 SQL 注入。
     * 
     * @param title  書名關鍵字（支援模糊查詢）
     * @param author 作者關鍵字（支援模糊查詢）
     * @param type   書籍類型
     * @return 符合條件的藏書列表
     */
    public List<Book> search(String title, String author, BookType type) {
        StringBuilder sql =
                new StringBuilder("SELECT * FROM books WHERE 1 = 1");

        List<Object> params = new ArrayList<>();

        if (title != null && !title.isBlank()) {
            sql.append(" AND title LIKE ?");
            params.add("%" + title.trim() + "%");
        }

        if (author != null && !author.isBlank()) {
            sql.append(" AND author LIKE ?");
            params.add("%" + author.trim() + "%");
        }

        if (type != null) {
            sql.append(" AND type = ?");
            params.add(type.name());
        }

        sql.append(" ORDER BY title");

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {            //executeQuery()用於 SELECT，回傳 ResultSet 結果集供逐列讀取
//                                                              //executeUpdate()用於 INSERT / UPDATE / DELETE，回傳受影響的列數（int）
                List<Book> list = new ArrayList<>();

                while (rs.next()) {
                    list.add(mapRow(rs));
                }

                return list;
            }

        } catch (SQLException e) {
            throw new DataAccessException("查詢藏書失敗", e);
        }
    }

    /**
     * 借出一份藏書：將可用份數減 1。
     * 內建防呆機制：僅在當前可用份數大於 0 時才會執行扣減。
     * 
     * @param bookId 藏書主鍵 ID
     */
    public void decrementAvailable(long bookId) {
        String sql = "UPDATE books SET available_copies = available_copies - 1 "
                + "WHERE id = ? AND available_copies > 0";

        updateCopies(sql, bookId, "扣減可用份數失敗");
    }

    /**
     * 歸還一份藏書：將可用份數加 1。
     * 內建防呆機制：僅在當前可用份數小於總館藏份數時才會執行回補。
     * 
     * @param bookId 藏書主鍵 ID
     */
    public void incrementAvailable(long bookId) {
        String sql = "UPDATE books SET available_copies = available_copies + 1 "
                + "WHERE id = ? AND available_copies < total_copies";

        updateCopies(sql, bookId, "回補可用份數失敗");
    }

    /**
     * 共用私有方法：執行館藏份數的更新動作。
     * 統一封裝 PreparedStatement 的執行與例外轉譯，實踐 DRY 原則。
     * 
     * @param sql      要執行的 SQL 更新指令
     * @param bookId   藏書主鍵 ID
     * @param errorMsg 發生例外時的自訂錯誤描述
     */
    private void updateCopies(String sql, long bookId, String errorMsg) {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, bookId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException(errorMsg, e);
        }
    }

    /**
     * 共用私有方法：資料列對應器（Row Mapper）。
     * 將目前 ResultSet 游標指向的一列資料，完整轉換並封裝為 Book 物件。
     * 
     * @param rs 資料庫查詢結果集
     * @return 轉換後的 Book 物件
     * @throws SQLException 若欄位讀取失敗時拋出
     */
    private Book mapRow(ResultSet rs) throws SQLException {
        return new Book(
                rs.getLong("id"),
                rs.getString("isbn"),
                rs.getString("title"),
                rs.getString("author"),
                BookType.valueOf(rs.getString("type")),
                rs.getInt("total_copies"),
                rs.getInt("available_copies"));
    }
}