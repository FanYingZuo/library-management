package com.library.ui;

import com.library.exception.LibraryException;
import com.library.model.MemberType;
import com.library.service.MemberService;
import com.library.model.Member;

public class MemberMenu {

    private final MemberService memberService;

    /**
     * 建構子注入（Dependency Injection）
     * 外部傳入 MemberService，讓選單專注於使用者互動，不直接碰觸資料庫或底層商業邏輯。
     */
    public MemberMenu(MemberService memberService) {
        this.memberService = memberService;
    }
  
    /**
     * 會員管理主選單迴圈
     * while (true)永續迴圈搭配switch ，將使用者的選擇導向對應方法
     */
    public void memberMenu() {
        while (true) {
            System.out.println("""

                    ──── 會員管理 ────
                     1. 新增會員
                     2. 依會員編號查詢
                     3. 會員列表
                     9. 回上層""");
            switch (InputHandler.input("請選擇")) {
                case "1" -> addMember();
                case "2" -> findMember();
                // 行內簡化：直接取得清單並用 Lambda 走訪印出每一筆會員資料
                case "3" -> memberService.listAll().forEach(m -> System.out.println("  " + m));
                case "9" -> {
                    return; // 結束方法，返回上一層選單
                }
                default -> System.out.println("✘ 無效選項");
            }
        }
    }

    /**
     * 執行新增會員流程
     * 依序收集編號、姓名、列舉型態（MemberType）與選填的電子郵件，建立物件後交由 Service 處理，並防範例外。
     */
    private void addMember() {
        try {
            String no = InputHandler.input("會員編號");
            String name = InputHandler.input("姓名");
            MemberType type = InputHandler.inputMemberType();
            String email = InputHandler.inputOptional("電子郵件"); // 允許空白的選填欄位
            
            Member member = memberService.addMember(new Member(no, name, type, email));
            System.out.printf("✔ 新增成功：%s（%s）%n", member.getName(), type.label());
        } catch (LibraryException | IllegalArgumentException e) {
            // 攔截自定義例外或非法引數，印出錯誤訊息而不讓系統崩潰
            System.out.println("✘ " + e.getMessage());
        }
    }

    /**
     * 依會員編號查詢
     * 運用 Java Optional 的函數式寫法，優雅地處理「有找到物件」與「查無資料」兩種分支，避免 NullPointerException。
     */
    private void findMember() {
        String no = InputHandler.input("會員編號");
        memberService.findByMemberNo(no).ifPresentOrElse(      //ifPresentOrElse
                m -> System.out.println("  " + m),             // 若存在（Present）：印出會員資訊
                () -> System.out.println("查無此會員")       // 若不存在（Else）：印出提示
        );                                                    
    }                                                       
}                                                                                                                        