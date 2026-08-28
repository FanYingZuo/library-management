package com.library.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * 圖書館系統：資料庫連線工具類別 (DBUtil)
 * 
 * ==============================================================================
 * 【核心觀念大白話：這支程式在做什麼？】
 * 它就像是資料庫的「電話簿與撥號專線」，負責提供通往資料庫的連線通道。
 * 
 * 1. 集中管理，不用到處寫重複帳密：
 *    如果系統有許多 DAO，若沒有這支工具，每個地方都要寫一遍資料庫網址、帳號、密碼。
 *    有了 DBUtil，連線邏輯完全集中。哪天資料庫密碼改了，只要改這裡或設定檔即可全系統生效。
 * 
 * 2. 靜態區塊 (static { }) 的威力：
 *    程式一開機、類別被載入時，靜態區塊會搶先執行一次，把帳密與網址讀好放進變數。
 *    之後每次呼叫 getConnection() 都可以直接拿來用，不用每次都去讀硬碟設定檔，效能極佳。
 * 
 * 3. 貼心的預設值防呆設計：
 *    透過 props.getProperty("key", "預設值")，如果找不到外部的 db.properties 設定檔，
 *    系統不會直接死掉，而是會自動套用後方的本機預設值來頂著用，非常強韌。
 * ==============================================================================
 */
public class DBUtil {

    // 宣告資料庫連線所需的三個核心參數（設為靜態與不可變常數，全系統共用）
    private static final String url;
    private static final String user;
    private static final String password;

    /**
     * 私有建構子 (Private Constructor)
     * 阻止外部使用 new DBUtil() 建立物件。
     * 因為這只是一個純工具類別，所有方法與變數都是 static，不需要被實例化佔用記憶體。
     */
    private DBUtil() {
        // 工具類別，不可實例化
    }

    /**
     * 靜態區塊 (Static Block)
     * 特點：當這個類別第一次被 Java 載入記憶體時，這段程式碼會自動且優先執行一次。
     * 用途：在這裡讀取設定檔，把資料庫的網址、帳號、密碼準備好，供後續隨時使用。
     */
    static {
        Properties props = new Properties();
        
        /*
         * 嘗試從專案的資源資料夾中讀取名叫 db.properties 的設定檔。
         * 這裡使用 try-with-resources 機制，確保串流用完後會自動關閉，避免資源外洩。
         */
        try (InputStream in = DBUtil.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in); // 如果成功找到檔案，就把裡面的設定讀進來
            }
        } catch (IOException e) {
            // 如果找不到檔案或讀取失敗，印出警告，並讓程式繼續運行（會使用下方的預設值作為備案）
            System.err.println("[DBUtil] 讀取 db.properties 失敗，改用預設值：" + e.getMessage());
        }

        /*
         * 從設定檔中抓取對應的屬性；
         * 如果抓不到（例如沒寫或沒檔案），就使用逗號後方的「預設值」來頂著用。
         */
        url = props.getProperty("db.url",
                "jdbc:mysql://localhost:3306/library_db"
                        + "?useSSL=false&serverTimezone=Asia/Taipei"
                        + "&characterEncoding=utf8&allowPublicKeyRetrieval=true");
        user = props.getProperty("db.user", "username");
        password = props.getProperty("db.password", "");
    }

    /**
     * 取得資料庫連線 (Get Connection)
     * 任何 DAO 需要對資料庫進行操作時，都會呼叫這個方法來拿到一條通往資料庫的連線通道。
     * 
     * @return 資料庫連線物件 (Connection)
     * @throws SQLException 如果連線失敗（例如帳號密碼錯誤、資料庫伺服器沒開）就會拋出例外
     */
    public static Connection getConnection() throws SQLException {
        /*
         * 透過 JDBC 的 DriverManager，拿剛才靜態區塊準備好的 url、帳號、密碼，
         * 向資料庫發起連線，並把通道交給呼叫端。
         */
        return DriverManager.getConnection(url, user, password);
    }
}