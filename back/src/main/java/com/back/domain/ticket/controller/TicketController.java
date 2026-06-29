package com.back.domain.ticket.controller;

import com.back.domain.ticket.dto.PaymentTicketRequest;
import com.back.domain.ticket.dto.PaymentTicketResponse;
import com.back.domain.ticket.service.TicketService;
import com.back.global.annotation.ApiV1;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@ApiV1
@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
@Tag(name = "Ticket", description = "Ticket API")
public class TicketController {

    private final TicketService ticketService;

    @PostMapping("/reserve")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "결제 및 티켓 생성", description = "결제 및 티켓 생성 API")
    public RsData<PaymentTicketResponse> createTicket(
            @RequestHeader(value = "userId") Long userId, @RequestBody @Valid PaymentTicketRequest request) {
        PaymentTicketResponse response = ticketService.createTicket(userId, request);
        return new RsData<>(
                "201-1",
                "결제 및 티켓 생성 성공",
                response
        );
    }

    @PatchMapping("/cancel/{ticketId}")
    @Operation(summary = "결제 취소", description = "결제 취소 API")
    public RsData<Void> cancelTicket(
            @RequestHeader(value = "userId") Long userId, @PathVariable(value = "ticketId") Long ticketId) {
        ticketService.cancelTicket(userId, ticketId);
        return new RsData<>(
                "200-1",
                "티켓 취소 성공",
                null
        );
    }
}
