package com.library.exception;

/**
 * 圖書館系統：業務例外基礎類別
 * 
 * 所有自訂業務例外（如資料重複、狀態不合法、查無資料等）均須繼承此類別。
 * 採用非受檢例外（RuntimeException）設計，具備以下優勢：
 * - 精簡商業邏輯：Service 層可直接拋出，不需繁瑣的 throws 宣告。
 * - 統一錯誤攔截：UI 介面層只需用單一個 catch (LibraryException e) 即可一網打盡所有業務錯誤。
 */
public class LibraryException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 建構子：初始化自訂的錯誤提示訊息。
     * 
     * @param message 描述錯誤原因的文字，後續可透過 getMessage() 取得並呈現給使用者
     */
    public LibraryException(String message) {
        super(message);
    }

    /**
     * 建構子：同時傳遞錯誤訊息與根本原因。
     * 適用於將底層系統例外包裝成業務例外時使用。
     * 
     * @param message 描述錯誤原因的文字
     * @param cause 導致此例外發生的原始底層例外
     */
    public LibraryException(String message, Throwable cause) {
        super(message, cause);
    }
}