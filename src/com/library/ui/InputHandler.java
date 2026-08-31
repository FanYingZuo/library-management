package com.library.ui;

import java.util.Scanner;
import com.library.model.BookType;
import com.library.model.MemberType;

public class InputHandler {
    /**
     *new Scanner 來讀取使用者的鍵盤輸入
     *private 確保內部私用、static 讓所有靜態方法共用、final 確保指標不會被更改
     *⚠️ 注意：千萬不能呼叫 inputScanner.close()，否則底層的 System.in 會被關閉，導致整個程式無法再接收鍵盤輸入！*/
    private static final Scanner inputScanner = new Scanner(System.in);

    /**
     *統一使用 label 
     *變數作用域隔離：參數屬於區域變數，其作用域僅限於各自的方法內部，彼此獨立且互不干擾。
     *顯示提示文字並讀取輸入，自動去除前後空白trim() 
     *一律以 nextLine() 讀整行再自行轉型，避免 nextInt() 殘留換行的陷阱。*/
    public static String input(String label) {
        System.out.print(label + "：");
        return inputScanner.nextLine().trim();
    }

    /** 允許空白的輸入，如果直接按 Enter 沒填就回傳 null */
    public static String inputOptional(String label) {
        String inputValue = input(label);
        return inputValue.isBlank() ? null : inputValue; //三元運算端子 ? true : false
    }

    /** 強制使用者輸入數字，輸入錯了就一直叫他重打，直到對為止 */
    public static int inputInt(String label) {
        while (true) {
            try {
                return Integer.parseInt(input(label));
            } catch (NumberFormatException e) {
                System.out.println("✘ 請輸入數字");
            }
        }
    }

    /** 讓使用者選擇書本類型，選錯就一直重來，直到選 1~3 為止 */
    public static BookType inputBookType() {
        while (true) {
            String choice = input("類型 (1)紙本 (2)電子 (3)有聲");
            switch (choice) {
                case "1":
                    return BookType.PAPER;
                case "2":
                    return BookType.EBOOK;
                case "3":
                    return BookType.AUDIO;
                default:
                    System.out.println("✘ 請輸入 1~3");
            }
        }
    }

    /** 讓使用者選書本類型，直接按 Enter 就當作不限（回傳 null） */
    public static BookType inputOptionalBookType() {
        String choice = input("類型 (1)紙本 (2)電子 (3)有聲，Enter 代表不限");
        return switch (choice) {
            case "1" -> BookType.PAPER;
            case "2" -> BookType.EBOOK;
            case "3" -> BookType.AUDIO;
            default -> null;
        };
    }

    /** 讓使用者選擇身分，選錯就一直重來，直到選 1 或 2 為止 */
    public static MemberType inputMemberType() {
        while (true) {
            String choice = input("身份 (1)學生 (2)教職員");
            switch (choice) {
                case "1":
                    return MemberType.STUDENT;
                case "2":
                    return MemberType.STAFF;
                default:
                    System.out.println("✘ 請輸入 1 或 2");
            }
        }
    }
}