package com.library.ui;

import com.library.service.LoanService;
import com.library.service.RenewResult;
import com.library.service.ReturnResult;
import java.util.List;
import com.library.exception.LibraryException;
import com.library.model.ActiveLoanDetail;
import com.library.model.Loan;
import com.library.model.Reservation;
import com.library.model.ReservationDetail;

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
     * 借閱與預約管理主選單迴圈
     * while (true)永續迴圈搭配switch ，將使用者的選擇導向對應方法，輸入 9 則跳出迴圈返回。
     */
    public void loanMenu() {
        while (true) {
            System.out.println("""

                    ──── 借閱與預約管理 ────
                     1. 借書
                     2. 還書
                     3. 續借
                     4. 預約
                     5. 取消預約
                     6. 未歸還清單
                     7. 預約清單
                     9. 回上層""");
            switch (InputHandler.input("請選擇")) {
                case "1" -> borrow();
                case "2" -> returnBook();
                case "3" -> renew();
                case "4" -> reserve();
                case "5" -> cancelReservation();
                case "6" -> listActiveLoans();
                case "7" -> listPendingReservations();
                case "9" -> {
                    return; // 結束方法，返回上一層選單
                }
                default -> System.out.println("✘ 無效選項");
            }
        }
    }

    /**
     * 執行借書流程（F3 借書：三道檢查 ＋ 到期日 ＋ 份數 −1）
     * 依序收集書籍與會員 ID，呼叫服務層完成借閱，並顯示書籍名稱與會員姓名。
     */
    private void borrow() {
        try {
            long bookId = InputHandler.inputInt("書籍 id");
            long memberId = InputHandler.inputInt("會員 id");
            Loan loan = loanService.borrow(bookId, memberId);
            String bookTitle = loanService.getBookTitle(bookId);
            String memberName = loanService.getMemberName(memberId);
            System.out.printf("✔ 借閱成功：借閱單 #%d 《%s》（會員：%s），應還日 %s%n",
                    loan.getId(), bookTitle, memberName, loan.getDueDate());
        } catch (LibraryException | IllegalArgumentException e) {
            // 攔截自定義例外或非法引數，輸出錯誤訊息而不中斷程式
            System.out.println("✘ " + e.getMessage());
        }
    }

    /**
     * 執行還書流程（F4 還書：逾期判斷 ＋ 罰金 ＋ 份數 ＋1）
     * 接收借閱單 ID，取得還書結果（ReturnResult），並根據是否逾期動態顯示罰金與書名、會員姓名。
     */
    private void returnBook() {
        try {
            long loanId = InputHandler.inputInt("借閱單 id");
            String bookTitle = loanService.findLoanById(loanId)
                    .map(l -> loanService.getBookTitle(l.getBookId())).orElse("");
            String memberName = loanService.findLoanById(loanId)
                    .map(l -> loanService.getMemberName(l.getMemberId())).orElse("");

            ReturnResult returnResult = loanService.returnBook(loanId);
            
            if (returnResult.isOverdue()) {
                System.out.printf("✔ 還書完成：借閱單 #%d 《%s》（會員：%s）— 逾期 %d 天，罰金 %s 元%n",
                        returnResult.loan().getId(),
                        bookTitle,
                        memberName,
                        returnResult.overdueDays(),
                        returnResult.fine().toPlainString());
            } else {
                System.out.printf("✔ 還書完成：借閱單 #%d 《%s》（會員：%s）— 準時歸還，無罰金%n",
                        returnResult.loan().getId(),
                        bookTitle,
                        memberName);
            }
        } catch (LibraryException | IllegalArgumentException e) {
            System.out.println("✘ " + e.getMessage());
        }
    }

    /**
     * 執行續借流程（Renew）
     * 規則：到期前 3 天內才可辦理、已逾期不可續借、被他人預約之書籍不可續借。
     */
    private void renew() {
        try {
            long loanId = InputHandler.inputInt("借閱單 id");
            String bookTitle = loanService.findLoanById(loanId)
                    .map(l -> loanService.getBookTitle(l.getBookId())).orElse("");
            String memberName = loanService.findLoanById(loanId)
                    .map(l -> loanService.getMemberName(l.getMemberId())).orElse("");

            RenewResult renewResult = loanService.renew(loanId);
            System.out.printf("✔ 續借成功：借閱單 #%d 《%s》（會員：%s）延長借期 %d 天，原到期日 %s → 新到期日 %s%n",
                    renewResult.loan().getId(),
                    bookTitle,
                    memberName,
                    renewResult.extendedDays(),
                    renewResult.previousDueDate(),
                    renewResult.newDueDate());
        } catch (LibraryException | IllegalArgumentException e) {
            System.out.println("✘ " + e.getMessage());
        }
    }

    /**
     * 執行預約流程（Reserve）
     * 規則：預約已被借出之書籍；在庫尚有可借份數時請直接借閱。
     */
    private void reserve() {
        try {
            long bookId = InputHandler.inputInt("書籍 id");
            long memberId = InputHandler.inputInt("會員 id");
            Reservation reservation = loanService.reserve(bookId, memberId);
            String bookTitle = loanService.getBookTitle(bookId);
            String memberName = loanService.getMemberName(memberId);
            System.out.printf("✔ 預約成功：預約單 #%d 《%s》（會員：%s），預約日期 %s%n",
                    reservation.getId(), bookTitle, memberName, reservation.getReserveDate());
        } catch (LibraryException | IllegalArgumentException e) {
            System.out.println("✘ " + e.getMessage());
        }
    }

    /**
     * 取消預約流程
     */
    private void cancelReservation() {
        try {
            long reservationId = InputHandler.inputInt("預約單 id");
            loanService.cancelReservation(reservationId);
            System.out.printf("✔ 取消成功：預約單 #%d 已取消%n", reservationId);
        } catch (LibraryException | IllegalArgumentException e) {
            System.out.println("✘ " + e.getMessage());
        }
    }

    /**
     * 查詢未歸還清單（顯示書名、書籍 ID、會員姓名、會員 ID 與到期日）
     */
    private void listActiveLoans() {
        List<ActiveLoanDetail> activeLoanDetails = loanService.listActiveLoanDetails();
        
        if (activeLoanDetails.isEmpty()) {
            System.out.println("（目前無未歸還借閱）");
            return;
        }
        
        System.out.println("未歸還共 " + activeLoanDetails.size() + " 筆：");
        activeLoanDetails.forEach(loanDetail -> System.out.printf("  #%d 《%s》(書 id=%d) — 會員：%s(會員 id=%d) 應還 %s%n",
                loanDetail.id(),
                loanDetail.bookTitle(),
                loanDetail.bookId(),
                loanDetail.memberName(),
                loanDetail.memberId(),
                loanDetail.dueDate()));
    }

    /**
     * 查詢有效預約清單（顯示書名、書籍 ID、會員姓名、會員 ID、預約日與狀態）
     */
    private void listPendingReservations() {
        List<ReservationDetail> pendingReservationDetails = loanService.listPendingReservationDetails();
        if (pendingReservationDetails.isEmpty()) {
            System.out.println("（目前無有效預約）");
            return;
        }
        System.out.println("有效預約共 " + pendingReservationDetails.size() + " 筆：");
        pendingReservationDetails.forEach(reservationDetail -> System.out.printf("  #%d 《%s》(書 id=%d) — 會員：%s(會員 id=%d) 預約日 %s 狀態=%s%n",
                reservationDetail.id(),
                reservationDetail.bookTitle(),
                reservationDetail.bookId(),
                reservationDetail.memberName(),
                reservationDetail.memberId(),
                reservationDetail.reserveDate(),
                reservationDetail.status().label()));
    }
}
