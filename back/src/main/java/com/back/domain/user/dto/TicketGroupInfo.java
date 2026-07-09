package com.back.domain.user.dto;

import com.back.domain.ticket.entity.Ticket;

import java.util.List;

public record TicketGroupInfo(
        Long scheduleId,
        String concertName,
        String urlPoster,
        String startDate,
        String endDate,
        int round,
        int totalPrice,
        List<TicketSummary> tickets
) {
    public static TicketGroupInfo from(List<Ticket> tickets) {
        Ticket first = tickets.get(0);
        return new TicketGroupInfo(
                first.getSchedule().getScheduleId(),
                first.getSchedule().getConcert().getConcertName(),
                first.getSchedule().getConcert().getUrlPoster(),
                first.getSchedule().getConcert().getStartDate().toLocalDate().toString(),
                first.getSchedule().getConcert().getEndDate().toLocalDate().toString(),
                first.getSchedule().getRound(),
                tickets.stream().mapToInt(Ticket::getTicketPrice).sum(),
                tickets.stream().map(TicketSummary::from).toList()
        );
    }
}