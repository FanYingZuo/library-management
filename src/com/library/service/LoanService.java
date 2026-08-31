package com.library.service;

import com.library.dao.BookDAO;
import com.library.dao.LoanDAO;
import com.library.dao.MemberDAO;
import com.library.dao.ReservationDAO;
import com.library.exception.BookNotAvailableException;
import com.library.exception.BorrowLimitExceededException;
import com.library.exception.EntityNotFoundException;
import com.library.exception.OverdueBlockException;
import com.library.exception.RenewException;
import com.library.exception.ReservationException;
import com.library.model.ActiveLoanDetail;
import com.library.model.Book;
import com.library.model.Loan;
import com.library.model.Member;
import com.library.model.MemberRankingRow;
import com.library.model.OverdueReportRow;
import com.library.model.Reservation;
import com.library.model.ReservationDetail;
import com.library.model.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 借閱業務邏輯（F3 借書、F4 還書）與報表（F6）。
 * <p>
 * 本層是業務規則的落腳處：三道借書檢查、到期日計算、
 * 逾期罰金結算全部集中在此。
 */
public class LoanService {

    private final BookDAO bookDao;
    private final MemberDAO memberDao;
    private final LoanDAO loanDao;
    private final ReservationDAO reservationDao;

    public LoanService(BookDAO bookDao, MemberDAO memberDao, LoanDAO loanDao) {
        this(bookDao, memberDao, loanDao, new ReservationDAO());
    }

    public LoanService(BookDAO bookDao, MemberDAO memberDao, LoanDAO loanDao, ReservationDAO reservationDao) {
        this.bookDao = bookDao;
        this.memberDao = memberDao;
        this.loanDao = loanDao;
        this.reservationDao = reservationDao;
    }

    /**
     * 借書（F3 借書：三道檢查 ＋ 到期日 ＋ 份數 −1）。依序通過三道檢查後才寫入借閱紀錄並扣減份數。
     *
     * @throws BookNotAvailableException    已無可借份數
     * @throws BorrowLimitExceededException 達同時借書上限
     * @throws OverdueBlockException        有逾期未還書籍
     *
     *  orElseThrow()如果裡面有東西，就把資料拿出來用；如果是空的，就立刻拋出（Throw）指定的例外（Exception）！
     */
    public Loan borrow(long bookId, long memberId) {
        Book book = bookDao.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("找不到書籍 id=" + bookId));
        Member member = memberDao.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("找不到會員 id=" + memberId));

        // 檢查 1：可借份數（電子書不限份數）
        if (!book.hasAvailableCopy()) {
            throw new BookNotAvailableException(book);
        }
        // 檢查 2：同時借書上限
        if (loanDao.countActiveByMember(memberId) >= member.borrowLimit()) {
            throw new BorrowLimitExceededException(member);
        }
        // 檢查 3：逾期封鎖
        if (loanDao.hasOverdue(memberId)) {
            throw new OverdueBlockException(member);
        }

        // 到期日 = 今天 + 書的借期 + 會員延長天數
        LocalDate today = LocalDate.now(); //取得今天的系統日期
        LocalDate due = today.plusDays(book.loanDays() + member.extraDays());//today.plusDays()把「今天」加上計算出來的總天數，得出最終的還書期限（due）
        
        // 先寫紀錄再扣份數；任一檢查失敗都不會走到這裡，不留半套狀態
        Loan loan = loanDao.insert(new Loan(bookId, memberId, today, due));
        if (book.getType().isCopyLimited()) {
            bookDao.decrementAvailable(bookId);
        }
        return loan;
    }

    /**
     * 還書（F4 還書：逾期判斷 ＋ 罰金 ＋ 份數 ＋1）。計算逾期天數與罰金，更新紀錄並回補份數。
     *
     * @param loanId 借閱紀錄 id
     * @throws EntityNotFoundException 找不到未歸還的借閱紀錄
     * 
     * orElseThrow()如果裡面有東西，就把資料拿出來用；如果是空的，就立刻拋出（Throw）指定的例外（Exception）！
     */
    public ReturnResult returnBook(long loanId) {
        Loan loan = loanDao.findActiveById(loanId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "找不到未歸還的借閱紀錄 id=" + loanId));
        Book book = bookDao.findById(loan.getBookId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "借閱對應的書籍不存在 id=" + loan.getBookId()));

        LocalDate today = LocalDate.now(); //取得今天的系統日期
        long overdueDays = loan.overdueDays(today);//loan 內部會拿今天的日期去和原本的「應還期限」做比對

        // 罰金 = 逾期天數 × 類型費率（電子書費率為 0）
        BigDecimal fine = BigDecimal.valueOf(overdueDays * (long) book.finePerDay());

        loan.close(today, fine);
        loanDao.update(loan);

        // 電子書不占份數，不需回補
        if (book.getType().isCopyLimited()) {
            bookDao.incrementAvailable(book.getId());
        }
        return new ReturnResult(loan, overdueDays, fine);
    }

    /**
     * 會員續借（Renew）。
     * 續借規則：
     * 1. 借閱紀錄必須存在且仍在借閱中。
     * 2. 已逾期之書籍不可續借。
     * 3. 如果書在到期日大於 3 天續借必須跳錯誤（僅限到期前 3 天內辦理）。
     * 4. 續借到別人預約的書必須跳錯誤（若有其他人預約則禁止續借）。
     *
     * @param loanId 借閱紀錄 ID
     * @return 續借結果（包含新舊到期日與延長天數）
     */
    public RenewResult renew(long loanId) {
        Loan loan = loanDao.findActiveById(loanId)
                .orElseThrow(() -> new EntityNotFoundException("找不到進行中的借閱紀錄 id=" + loanId));
        Book book = bookDao.findById(loan.getBookId())
                .orElseThrow(() -> new EntityNotFoundException("借閱對應的書籍不存在 id=" + loan.getBookId()));
        Member member = memberDao.findById(loan.getMemberId())
                .orElseThrow(() -> new EntityNotFoundException("借閱對應的會員不存在 id=" + loan.getMemberId()));

        LocalDate today = LocalDate.now();

        // 檢查 1：是否已逾期
        if (loan.isOverdue(today)) {
            long overdueDays = loan.overdueDays(today);
            throw new RenewException(String.format("續借失敗：書籍《%s》已逾期 %d 天，請先辦理還書並繳納罰金", book.getTitle(), overdueDays));
        }

        // 檢查 2：如果書在到期日大於 3 天續借必須跳錯誤
        long daysRemaining = loan.daysRemaining(today);
        if (daysRemaining > 3) {
            throw new RenewException(String.format("續借失敗：距離到期日尚有 %d 天，僅允許在到期前 3 天內辦理續借", daysRemaining));
        }

        // 檢查 3：續借到別人預約的書必須跳錯誤
        if (reservationDao.hasPendingReservationByOther(loan.getBookId(), loan.getMemberId())) {
            throw new RenewException(String.format("續借失敗：書籍《%s》已被其他讀者預約，無法辦理續借", book.getTitle()));
        }

        // 續借延長：書籍基本借期 + 會員額外借期
        int extendedDays = book.loanDays() + member.extraDays();
        LocalDate previousDueDate = loan.getDueDate();
        LocalDate newDueDate = previousDueDate.plusDays(extendedDays);

        loan.setDueDate(newDueDate);
        loanDao.updateDueDate(loan.getId(), newDueDate);

        return new RenewResult(loan, previousDueDate, newDueDate, extendedDays);
    }

    /**
     * 預約書籍（Reserve）。
     * 預約規則：
     * 1. 預約只能預約別人已借出、尚未辦理歸還的書籍；若館內尚有在架可借份數則直接借閱即可。
     * 2. 會員若有逾期未還書籍則禁止預約。
     * 3. 會員不可預約自己正在借閱中的書籍。
     * 4. 會員不可重複預約同一本書籍。
     *
     * @param bookId   書籍 ID
     * @param memberId 會員 ID
     * @return 預約紀錄物件
     */
    public Reservation reserve(long bookId, long memberId) {
        Book book = bookDao.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("找不到書籍 id=" + bookId));
        Member member = memberDao.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("找不到會員 id=" + memberId));

        LocalDate today = LocalDate.now();

        // 檢查 1：會員是否有逾期未還書籍
        if (loanDao.hasOverdue(memberId)) {
            throw new OverdueBlockException(member);
        }

        // 檢查 2：會員是否目前正借閱這本書（不能預約自己已借出的書）
        if (loanDao.findActiveByBookAndMember(bookId, memberId).isPresent()) {
            throw new ReservationException(String.format("預約失敗：您目前正借閱《%s》，不可預約自己正在借閱的書籍", book.getTitle()));
        }

        // 檢查 3：會員是否已預約過此書（不可重複預約）
        if (reservationDao.findPendingByBookAndMember(bookId, memberId).isPresent()) {
            throw new ReservationException(String.format("預約失敗：您已預約過《%s》（預約中），請勿重複預約", book.getTitle()));
        }

        // 檢查 4：館內是否尚有在庫可借份數（預約只能預約別人還未辦理借書以及續借的書）
        if (book.getType().isCopyLimited() && book.getAvailableCopies() > 0) {
            throw new ReservationException(String.format("預約失敗：書籍《%s》目前尚有在庫可借份數（%d 份），請直接辦理借書",
                    book.getTitle(), book.getAvailableCopies()));
        }

        // 建立預約記錄並寫入資料庫
        return reservationDao.insert(new Reservation(bookId, memberId, today, ReservationStatus.PENDING));
    }

    /**
     * 取消預約。
     *
     * @param reservationId 預約紀錄 ID
     */
    public void cancelReservation(long reservationId) {
        Reservation reservation = reservationDao.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("找不到預約紀錄 id=" + reservationId));
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ReservationException("該預約紀錄非有效預約狀態，無法取消");
        }
        reservationDao.updateStatus(reservationId, ReservationStatus.CANCELLED);
    }

    /** 依 ID 取得借閱紀錄。 */
    public Optional<Loan> findLoanById(long loanId) {
        return loanDao.findActiveById(loanId);
    }

    /** 依書籍 ID 查詢書名。 */
    public String getBookTitle(long bookId) {
        return bookDao.findById(bookId).map(Book::getTitle).orElse("（未知書籍）");
    }

    /** 依會員 ID 查詢會員姓名。 */
    public String getMemberName(long memberId) {
        return memberDao.findById(memberId).map(Member::getName).orElse("（未知會員）");
    }

    /** 查詢所有有效預約中（PENDING）的詳細清單（含書名與會員姓名）。 */
    public List<ReservationDetail> listPendingReservationDetails() {
        return reservationDao.findAllPendingDetails();
    }

    /** 查詢所有有效預約中（PENDING）的預約清單。 */
    public List<Reservation> listPendingReservations() {
        return reservationDao.findAllPending();
    }

    /** 查詢全館所有未歸還的借閱詳細紀錄（含書名與會員姓名）。 */
    public List<ActiveLoanDetail> listActiveLoanDetails() {
        return loanDao.findAllActiveDetails();
    }

    /** 全部未歸還的借閱紀錄。 */
    public List<Loan> listActiveLoans() {
        return loanDao.findAllActive();
    }

    /** 逾期借閱清單（F6 報表：逾期清單 ＋ 借閱排行），按逾期天數排序。 */
    public List<OverdueReportRow> overdueReport() {
        return loanDao.overdueReport();
    }

    /** 會員借閱排行（F6 報表：逾期清單 ＋ 借閱排行），預設前 10 名。 */
    public List<MemberRankingRow> memberRanking() {
        return loanDao.memberRanking(10);
    }
}
