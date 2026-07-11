package com.weib.exception;

public class DuplicateAppealException extends RuntimeException {
    public DuplicateAppealException() {
        super("该处罚已有待审核申诉");
    }
}