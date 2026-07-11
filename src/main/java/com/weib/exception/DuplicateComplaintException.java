package com.weib.exception;

public class DuplicateComplaintException extends RuntimeException {

    public DuplicateComplaintException() {
        super("该对象已有投诉正在审核");
    }
}
