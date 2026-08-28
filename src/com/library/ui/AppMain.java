package com.library.ui;

import com.library.dao.BookDAO;
import com.library.dao.LoanDAO;
import com.library.dao.MemberDAO;
import com.library.service.BookService;
import com.library.service.LoanService;
import com.library.service.MemberService;

// 系統主程式與進入點，負責整合各模組與主選單控制
public class AppMain {

    // 宣告業務邏輯層服務物件
    private final BookService bookService;
    private final MemberService memberService;
    private final LoanService loanService;

    // 初始化建構子，建立 Service 並藉由依賴注入傳入對應的 DAO 物件
    public AppMain() {
        this.bookService = new BookService(new BookDAO());
        this.memberService = new MemberService(new MemberDAO());
        this.loanService = new LoanService(new BookDAO(), new MemberDAO(), new LoanDAO());
    }

    // Java 應用程式執行的進入點
    public static void main(String[] args) {
        new AppMain().run();
    }

    // 主執行迴圈，持續顯示主選單並依使用者輸入分流至對應的子選單
    public void run() {
        System.out.println("════════ 圖書館借閱管理系統 ════════");
        while (true) {
            printMainMenu(); 
            
            // 讀取輸入選項並進行條件分支
            switch (InputHandler.input("請選擇")) {
                case "1" -> new BookMenu(this.bookService).bookMenu();
                case "2" -> new MemberMenu(this.memberService).memberMenu();
                case "3" -> new LoanMenu(this.loanService).loanMenu();
                case "4" -> new ReportMenu(this.loanService).reportMenu();
                case "0" -> {
                    System.out.println("再見！");
                    return; // 結束方法執行以終止迴圈
                }
                default -> System.out.println("✘ 無效選項，請重新輸入");
            }
        }
    }

    // 輸出主選單介面文字
    private void printMainMenu() {
        System.out.println("""

                ──────── 主選單 ────────
                 1. 藏書管理
                 2. 會員管理
                 3. 借閱管理
                 4. 報表
                 0. 離開""");
    }

}