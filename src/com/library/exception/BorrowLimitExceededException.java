package com.library.exception;

import com.library.model.Member;

/**
 * 借書時會員目前未還書數已達身份上限。
 */
public class BorrowLimitExceededException extends LibraryException {

    private static final long serialVersionUID = 1L;

    public BorrowLimitExceededException(Member member) {
        super(member.getName() + " 已達借閱上限 "
                + member.borrowLimit() + " 本，請先還書");
    }
}