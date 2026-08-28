package com.library.ui;

import com.library.service.LoanService;
import com.library.service.ReturnResult;
import java.util.List;
import com.library.exception.LibraryException;
import com.library.model.Loan;

public class LoanMenu {
    
    private final LoanService loanService;

    /**
     * 建構子注入（Dependency Injection）
     * 接收外部傳入的 LoanService，讓 Menu 專注於介面互動，不處理核心業務。
     */
    public LoanMenu(LoanService loanService) {
        this.loanService = loanService;
    }

    /**
     * 借閱管理主選單迴圈
     * while (true)永續迴圈搭配switch ，將使用者的選擇導向對應方法，輸入 9 則跳出迴圈返回。
     */
    public void loanMenu() {
        while (true) {
            System.out.println("""

                    ──── 借閱管理 ────
                     1. 借書
                     2. 還書
                     3. 未歸還清單
                     9. 回上層""");
            switch (InputHandler.input("請選擇")) {
                case "1" -> borrow();
                case "2" -> returnBook();
                case "3" -> listActiveLoans();
                case "9" -> {
                    return; // 結束方法，返回上一層選單
                }
                default -> System.out.println("✘ 無效選項");
            }
        }
    }

    /**
     * 執行借書流程
     * 依序收集書籍與會員 ID，呼叫服務層完成借閱，並透過 try-catch 確保例外發生時系統不會崩潰。
     */
    private void borrow() {
        try {
            long bookId = InputHandler.inputInt("書籍 id");
            long memberId = InputHandler.inputInt("會員 id");
            Loan loan = loanService.borrow(bookId, memberId);
            System.out.printf("✔ 借閱成功：借閱單 #%d，應還日 %s%n",
                    loan.getId(), loan.getDueDate());
        } catch (LibraryException | IllegalArgumentException e) {
            // 攔截自定義例外或非法引數，輸出錯誤訊息而不中斷程式
            System.out.println("✘ " + e.getMessage());
        }
    }

    /**
     * 執行還書流程
     * 接收借閱單 ID，取得還書結果（ReturnResult），並根據是否逾期動態顯示罰金計算。
     */
    private void returnBook() {
        try {
            long loanId = InputHandler.inputInt("借閱單 id");
            ReturnResult r = loanService.returnBook(loanId);
            
            // 條件判斷：依據還書結果物件內含的狀態，決定顯示罰金或準時訊息
            if (r.isOverdue()) {
                System.out.printf("✔ 還書完成：逾期 %d 天，罰金 %s 元%n",
                        r.overdueDays(), r.fine().toPlainString());// 使用 toPlainString() 轉為標準十進位字串，避免 BigDecimal 自動變成科學記號影響金額可讀性
            } else {
                System.out.println("✔ 還書完成：準時歸還，無罰金");
            }
        } catch (LibraryException | IllegalArgumentException e) {
            System.out.println("✘ " + e.getMessage());
        }
    }

    /**
     * 查詢未歸還清單
     * 運用防禦性檢查（Guard Clause）先處理空清單狀況，再使用 Lambda 表達式逐筆印出詳情。
     */
    private void listActiveLoans() {
        List<Loan> loans = loanService.listActiveLoans();
        
        // 防禦性檢查：若清單為空則提早返回（Early Return），避免後續執行無意義的迴圈
        if (loans.isEmpty()) {
            System.out.println("（目前無未歸還借閱）");
            return;
        }
        
        System.out.println("未歸還共 " + loans.size() + " 筆：");
        // 走訪集合（Iteration）：利用 forEach 與 Lambda 表達式格式化輸出每一筆借閱紀錄
        //forEach⤵︎  對於 loans 清單裡的每一個 l，都把它格式化印出來
        loans.forEach(l -> System.out.printf("  #%d 書 id=%d 會員 id=%d 應還 %s%n",
                l.getId(), l.getBookId(), l.getMemberId(), l.getDueDate()));
    }
}
