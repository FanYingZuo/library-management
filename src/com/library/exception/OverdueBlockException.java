package com.library.exception;

import com.library.model.Member;

/**
 * 借書時會員有逾期未還的書，依規則禁止借新書。
 */
public class OverdueBlockException extends LibraryException {

    private static final long serialVersionUID = 1L;

    public OverdueBlockException(Member member) {
        super(member.getName() + " 尚有逾期未還的書籍，請先還清後才能借新書");
    }
}