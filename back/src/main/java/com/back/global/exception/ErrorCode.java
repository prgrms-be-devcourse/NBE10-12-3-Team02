package com.back.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Auth
    AUTH_ID_NOT_FOUND("401-1", "존재하지 않는 아이디입니다."),
    AUTH_PASSWORD_MISMATCH("401-2", "비밀번호가 일치하지 않습니다."),
    AUTH_INVALID_BEARER_HEADER("401-3", "Authorization 헤더가 Bearer 형식이 아닙니다."),
    AUTH_INVALID_CREDENTIALS("401-4", "인증 정보가 유효하지 않습니다."),
    AUTH_INVALID_REFRESH_TOKEN("401-5", "유효하지 않은 리프레시 토큰입니다."),
    AUTH_REFRESH_TOKEN_NOT_FOUND_IN_REDIS("401-6", "Redis에 리프레시 토큰이 존재하지 않습니다."),
    AUTH_REFRESH_TOKEN_MISMATCH("401-7", "리프레시 토큰이 일치하지 않습니다."),
    AUTH_LOGIN_REQUIRED("401-8", "로그인 후 이용해주세요."),
    AUTH_FORBIDDEN("403-1", "권한이 없습니다."),

    // User
    USER_NOT_FOUND("404-1", "회원이 존재하지 않습니다."),
    USER_ID_ALREADY_EXISTS("409-1", "이미 사용 중인 아이디입니다."),
    USER_EMAIL_ALREADY_EXISTS("409-2", "이미 사용 중인 이메일입니다."),
    USER_ACCESS_DENIED("403-2", "본인의 정보만 조회할 수 있습니다."),
    USER_NOT_FOUND_OR_DELETED("404-2", "존재하지 않거나 이미 탈퇴한 회원입니다."),

    // Concert
    CONCERT_NOT_FOUND("404-3", "존재하지 않는 콘서트입니다."),

    // Schedule
    CONCERT_SCHEDULE_EMPTY("404-4", "등록된 회차가 없습니다."),
    SCHEDULE_NOT_FOUND("404-5", "존재하지 않는 회차입니다."),
    INVALID_CONCERT_SCHEDULE("400-1", "해당 콘서트의 회차가 아닙니다."),

    // Seat
    SEAT_NOT_FOUND("404-7", "존재하지 않는 좌석입니다."),
    SEAT_SOLD_OUT("400-2", "이미 매진된 좌석입니다."),
    SEAT_ALREADY_SOLD("409-3", "이미 판매 완료된 좌석입니다."),
    SEAT_ALREADY_HOLD("409-4", "이미 점유중인 좌석입니다."),
    SEAT_HELD_BY_OTHER_USER("409-5", "다른 사용자가 선택 중인 좌석입니다."),

    // Ticket
    TICKET_NOT_FOUND_FOR_USER("404-6", "해당 유저의 티켓이 존재하지 않습니다."),
    TICKET_ALREADY_CANCELLED("400-3", "이미 취소된 티켓입니다.");


    private final String resultCode;
    private final String message;

    public int getStatusCode() {
        return Integer.parseInt(resultCode.split("-", 2)[0]);
    }

}
