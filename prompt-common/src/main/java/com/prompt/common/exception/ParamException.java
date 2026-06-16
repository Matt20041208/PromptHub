package com.prompt.common.exception;

public class ParamException extends BaseException {
    public ParamException(String message) {
        super(422, message);
    }
}
