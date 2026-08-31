package com.library.service;

import com.library.dao.MemberDAO;
import com.library.exception.DuplicateMemberNoException;
import com.library.model.Member;

import java.util.List;
import java.util.Optional;

/**
 * 會員管理業務邏輯（F2 會員：新增兩種身份 → 查詢）。
 */
public class MemberService {

    // 資料存取物件（使用 final 確保初始化後不可變，保障執行緒安全與依賴穩定）
    private final MemberDAO memberDao;

    /**
     * 建構子注入：由外部傳入 MemberDAO，使 Service 專注於商業邏輯，不需自行創建資料庫連線。
     */
    public MemberService(MemberDAO memberDao) {
        this.memberDao = memberDao;
    }

    /**
     * 新增會員（F2 會員：新增兩種身份 → 查詢）。
     * 執行商業規則驗證（防範空值、未指定身份與編號重複），通過後才寫入資料庫。
     * 
     * @throws IllegalArgumentException     欄位不合法（空值或未選身份）
     * @throws DuplicateMemberNoException   會員編號已存在
     */
    public Member addMember(Member member) {
        // 1. 驗證基本文字欄位不可為空或空白
        requireText(member.getMemberNo(), "會員編號不可為空");
        requireText(member.getName(), "姓名不可為空");
        
        // 2. 驗證列舉型態（MemberType）是否指定
        if (member.getType() == null) {
            throw new IllegalArgumentException("請指定會員身份");
        }

        // 3.檢查資料庫是否已存在該筆資料  .isPresent()boolean只回傳true: 已存在||false: 不存在
        if (memberDao.findByMemberNo(member.getMemberNo()).isPresent()) {
            throw new DuplicateMemberNoException(member.getMemberNo());
        }

        // 4. 驗證皆通過，委派 DAO 進行持久化寫入
        memberDao.insert(member);
        return member;
    }

    /** 依會員編號查詢（直接委派 DAO 回傳 Optional 包裝的查詢結果）。 */
    public Optional<Member> findByMemberNo(String memberNo) {
        return memberDao.findByMemberNo(memberNo);
    }

    /** 取得全部會員清單。 */
    public List<Member> listAll() {
        return memberDao.findAll();
    }

    /**
     * 共用驗證方法（Private Helper）
     * 檢查字串是否為 null 或空白，若違規則立即拋出例外，實踐 DRY 原則。
     */
    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}