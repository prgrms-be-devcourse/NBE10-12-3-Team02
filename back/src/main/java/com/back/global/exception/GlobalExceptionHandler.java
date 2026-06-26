package com.back.global.exception;

import com.back.global.rsData.RsData;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    @ExceptionHandler(com.back.global.exception.ServiceException.class)
    public RsData<Void> handle(ServiceException ex, HttpServletResponse response) {
        RsData<Void> rsData = ex.getRsData();

        response.setStatus(rsData.statusCode());

        return rsData;
    }
}
