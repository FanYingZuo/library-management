package com.library.exception;

/**
 * 新增會員時會員編號已存在。
 */
public class DuplicateMemberNoException extends LibraryException {

    private static final long serialVersionUID = 1L;

    public DuplicateMemberNoException(String memberNo) {
        super("會員新增失敗：會員編號「" + memberNo + "」已被註冊，請更換編號");
    }
}