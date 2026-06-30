package com.back.domain.user.dto;

import com.back.domain.ticket.entity.Ticket;
import com.fasterxml.jackson.annotation.JsonProperty;

public record TicketInfo(
        Long ticketId,
        String urlPoster,
        String concertName,
        String startDate,
        String endDate,
        @JsonProperty("isValid") boolean isValid,
        int ticketPrice,
        String createdAt,
        String ticketNumber
) {
    public static TicketInfo from(Ticket ticket) {
        return new TicketInfo(
                ticket.getTicketId(),
                ticket.getSchedule().getConcert().getUrlPoster(),
                ticket.getSchedule().getConcert().getConcertName(),
                ticket.getSchedule().getConcert().getStartDate().toLocalDate().toString(),
                ticket.getSchedule().getConcert().getEndDate().toLocalDate().toString(),
                ticket.isValid(),
                ticket.getTicketPrice(),
                ticket.getCreateDate().toLocalDate().toString(),
                ticket.getTicketNumber()
        );
    }
}