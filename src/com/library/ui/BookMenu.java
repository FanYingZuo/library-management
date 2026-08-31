package com.library.ui;

import java.util.List;
import com.library.exception.LibraryException;
import com.library.model.Book;
import com.library.model.BookType;
import com.library.service.BookService;

// 藏書管理選單介面，負責接收使用者輸入並調用 BookService 處理藏書相關操作
// 支援 F1 藏書：新增 → 列表 → 依 ISBN 查詢 與 F5 查詢：書名／作者／類型任意組合
public class BookMenu {

    private final BookService bookService;

    // 建構子，接收並初始化傳入的 BookService
    public BookMenu(BookService bookService) {
         this.bookService = bookService;
    }

    // 藏書管理的主執行迴圈，提供選項選單並透過 switch 分流執行對應功能
    public void bookMenu() {
        while (true) {
            System.out.println("""

                    ──── 藏書管理 ────
                     1. 新增藏書
                     2. 依 ISBN 查詢
                     3. 藏書列表
                     4. 組合條件查詢
                     9. 回上層""");
            switch (InputHandler.input("請選擇")) {
                case "1" -> addBook();
                case "2" -> findBookByIsbn();
                case "3" -> listBooks();
                case "4" -> searchBooks();
                case "9" -> {
                    return;
                }
                default -> System.out.println("✘ 無效選項");
            }
        }
    }

    // F1 藏書：新增 → 列表 → 依 ISBN 查詢
    // 收集使用者輸入的新書資訊，呼叫 Service 新增藏書，並處理例外錯誤
    private void addBook() {
        try {
            String isbn = InputHandler.input("ISBN");
            String title = InputHandler.input("書名");
            String author = InputHandler.input("作者");
            BookType type = InputHandler.inputBookType();
            int copies = InputHandler.inputInt("館藏份數");
            Book book = bookService.addBook(new Book(isbn, title, author, type, copies));
            System.out.printf("✔ 新增成功：%s（%s，共 %d 份）%n",
                    book.getTitle(), type.label(), copies);
        } catch (LibraryException | IllegalArgumentException e) { // 捕捉在 try 區塊內發生的自訂錯誤或參數不合法錯誤。
            System.out.println("✘ " + e.getMessage());   //印出✘+錯誤訊息         
        }
    }

    // F1 藏書：新增 → 列表 → 依 ISBN 查詢
    // 依據輸入的 ISBN 查詢書籍並印出結果
    /* * .ifPresentOrElse()
    *有值時要執行的動作 (Consumer), 
    *沒值時要執行的動作 (Runnable)    */
    private void findBookByIsbn() {
        String isbn = InputHandler.input("ISBN");
        bookService.findByIsbn(isbn).ifPresentOrElse(
                book -> System.out.println("  " + book),      // 有東西時執行
                () -> System.out.println("查無此書"));// 是空的時執行
    }

    // F1 藏書：新增 → 列表 → 依 ISBN 查詢
    // 取得所有藏書清單並印出
    private void listBooks() {
        printBookList(bookService.listAll());
    }

    // F5 查詢：書名／作者／類型任意組合
    // 接收選擇性的組合查詢條件，呼叫 Service 進行多條件搜尋並印出結果
    private void searchBooks() {
        System.out.println("（直接按 Enter 代表該條件不限）");
        String title = InputHandler.inputOptional("書名關鍵字");
        String author = InputHandler.inputOptional("作者關鍵字");
        BookType type = InputHandler.inputOptionalBookType();
        printBookList(bookService.search(title, author, type));
    }
    
    // 接收書籍列表，若為空isEmpty()印出無資料，否則印出總筆數與逐筆內容
    private void printBookList(List<Book> books) {
        if (books.isEmpty()) {
            System.out.println("（無資料）");
            return;
        }
        System.out.println("共 " + books.size() + " 筆：");
        books.forEach(book -> System.out.println("  " + book)); // 跑forEach把每本books裡的書依序交給 book，並印出帶有空白的結果
    }
}