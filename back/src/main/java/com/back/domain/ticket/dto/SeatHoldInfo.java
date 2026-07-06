package com.back.domain.ticket.dto;

import jakarta.validation.constraints.NotBlank;

public record SeatHoldInfo(
        @NotBlank(message = "좌석 번호는 필수입니다.") String seatNumber,
        @NotBlank(message = "좌석 선점 토큰은 필수입니다.") String occupyToken
) {
}
