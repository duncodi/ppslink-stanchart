package com.duncodi.ppslink.stanchart.exceptions;

import lombok.Getter;

import java.io.Serial;

@Getter
public class CustomException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private CustomErrorCode errorCode;

    public CustomException(String message) {
        super(message);
    }

    public CustomException(CustomErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public CustomException(CustomErrorCode errorCode) {
        super(errorCode.getName());
        this.errorCode = errorCode;
    }

    public String getCodeAndMessage() {

        String code = getErrorCode()!=null?getErrorCode().name():"UNKNOWN";
        String message = getMessage()!=null?getMessage():"Unknown Error";

        return code + ": " + message;

    }

    public CustomErrorCode getErrorCode() {
        return errorCode;
    }
}
