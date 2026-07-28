package com.back.global.exception

import com.back.global.rsData.RsData
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException::class)
    fun handle(ex: ServiceException, response: HttpServletResponse): RsData<Void> =
        ex.rsData.also { response.status = it.statusCode }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handle(ex: MethodArgumentNotValidException, response: HttpServletResponse): RsData<Void> {
        val message = ex.bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: "잘못된 입력입니다."
        return RsData<Void>(ErrorCode.BAD_REQUEST.resultCode, message).also {
            response.status = it.statusCode
        }
    }
}
