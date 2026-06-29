package com.back.global.exception;

import com.back.global.rsData.RsData;
import lombok.Getter;

@Getter
public class ServiceException extends RuntimeException {

    private final ErrorCode errorCode;

    public ServiceException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public RsData<Void> getRsData() {
        return new RsData<>(
                errorCode.getResultCode(),
                errorCode.getMessage(),
                null
        );
    }
}
