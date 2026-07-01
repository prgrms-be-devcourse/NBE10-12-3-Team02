package com.back.domain.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentTicketRequest(
        @Schema(description = "공연 ID", example = "1")
        @NotNull(message = "공연 ID는 필수입니다.") Long concertId,
        @Schema(description = "회차 ID", example = "1")
        @NotNull(message = "회차 ID는 필수입니다.") Long scheduleId,
        @Schema(description = "예매할 좌석 번호", example = "A-1")
        @NotBlank(message = "좌석 번호는 필수입니다.") String seatNumber,
        @NotBlank(message = "좌석 선점 토큰은 필수입니다.") String occupyToken
) {
}
