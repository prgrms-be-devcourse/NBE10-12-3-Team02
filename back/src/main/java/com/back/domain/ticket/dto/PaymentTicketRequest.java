package com.back.domain.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PaymentTicketRequest(
        @Schema(description = "공연 ID", example = "1")
        @NotNull(message = "공연 ID는 필수입니다.") Long concertId,
        @Schema(description = "예매할 좌석 번호", example = "A-1")
        List<SeatHoldInfo> seatHolds
) {
}