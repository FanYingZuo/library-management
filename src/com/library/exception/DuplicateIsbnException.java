package com.library.exception;

/**
 * 新增藏書時 ISBN 已存在。
 */
public class DuplicateIsbnException extends LibraryException {

    private static final long serialVersionUID = 1L;

    public DuplicateIsbnException(String isbn) {
        super("藏書新增失敗：ISBN「" + isbn + "」已存在於館藏中，請確認後重新輸入");
    }
}