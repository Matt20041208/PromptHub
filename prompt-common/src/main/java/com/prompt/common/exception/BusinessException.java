package com.prompt.common.exception;

public class BusinessException extends BaseException {
    public BusinessException(String message) {
        super(400, message);
    }

    public BusinessException(int code, String message) {
        super(code, message);
    }
}
