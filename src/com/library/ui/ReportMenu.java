package com.library.ui;

import java.util.List;

import com.library.service.LoanService;
import com.library.model.OverdueReportRow;
import com.library.model.MemberRankingRow;

// 報表選單介面，負責接收使用者輸入並調用 LoanService 輸出統計報表
// 支援 F6 報表：逾期清單 ＋ 借閱排行
public class ReportMenu {

    private final LoanService loanService;

    /**
     * 建構子注入（Dependency Injection）
     * 外部傳入 LoanService，讓選單專注於報表資料的呈現，不自己建立服務實例。
     */
    public ReportMenu(LoanService loanService) {
         this.loanService = loanService;
    }

    /**
     * 報表主選單迴圈
     * 採用while (true)迴圈搭配switch 語法，將使用者的選擇導向對應的統計報表方法，輸入 9 則返回上層。
     */
    public void reportMenu() {
        while (true) {
            System.out.println("""

                    ──── 報表 ────
                     1. 逾期借閱清單
                     2. 會員借閱排行
                     9. 回上層""");
            switch (InputHandler.input("請選擇")) {
                case "1" -> overdueReport();
                case "2" -> memberRanking();
                case "9" -> {
                    return; // 結束方法，返回上一層選單
                }
                default -> System.out.println("✘ 無效選項");
            }
        }
    }

    /** 
     * 逾期借閱清單報表（按逾期天數由多到少排序）
     * F6 報表：逾期清單 ＋ 借閱排行
     * 採用防禦性檢查（Guard Clause），若無資料則提早返回；有資料則逐筆格式化印出細節。
     */
    private void overdueReport() {
        List<OverdueReportRow> overdueList = loanService.overdueReport();
        
        // 防禦性檢查：若清單為空則提早返回（Early Return），避免印出空白報表
        if (overdueList.isEmpty()) {
            System.out.println("（目前無逾期借閱）");
            return;
        }
        
        System.out.println("逾期借閱清單（按逾期天數）：");
        for (OverdueReportRow overdueRow : overdueList) {
            // 使用 printf 進行排版，精準帶入會員名稱、書名、應還日與逾期天數
            System.out.printf("  %s — 《%s》 應還 %s，逾期 %d 天%n",
                    overdueRow.memberName(), overdueRow.bookTitle(), overdueRow.dueDate(), overdueRow.overdueDays());
        }
    }

    /**
     * 會員借閱排行報表
     * F6 報表：逾期清單 ＋ 借閱排行
     * 利用外部計數器（rank）在迴圈中動態累加名次，直覺呈現會員的借閱活躍度。
     */
    private void memberRanking() {
        List<MemberRankingRow> rankingList = loanService.memberRanking();
        
        // 防禦性檢查：若無借閱紀錄則提早返回
        if (rankingList.isEmpty()) {
            System.out.println("（尚無借閱紀錄）");
            return;
        }
        
        System.out.println("會員借閱排行：");
        int rank = 1; // 初始化名次計數器->從第 1 名開始數
        for (MemberRankingRow rankingRow : rankingList) {
            // 使用 rank++ 邊印出名次邊遞增，並顯示會員姓名、身分別與借閱次數
            System.out.printf("  %d. %s（%s）— %d 次%n",
                    rank++,                  // 印出當前名次，印完之後 rank 自動加 1
                    rankingRow.memberName(),  // 會員姓名
                    rankingRow.memberType(),  // 會員身分別
                    rankingRow.loanCount());  // 借閱次數
        }
    }
}