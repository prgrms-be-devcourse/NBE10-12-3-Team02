package com.back.domain.ticket.dto;

import com.back.domain.schedule.entity.Schedule;
import com.back.domain.schedule.entity.ScheduleSeat;
import com.back.domain.schedule.entity.SeatStatus;
import com.back.domain.ticket.entity.Ticket;

import java.time.LocalDateTime;

public record PaymentTicketResponse(
        String ticketNumber,
        String urlPoster,
        String concertName,
        String seatNumber,
        LocalDateTime scheduleDate,
        SeatStatus seatStatus,
        boolean isValid
) {
    public static PaymentTicketResponse from(ScheduleSeat scheduleSeat, Schedule schedule, Ticket ticket) {
        return new PaymentTicketResponse(
                ticket.getTicketNumber(),
                schedule.getConcert().getUrlPoster(),
                schedule.getConcert().getConcertName(),
                scheduleSeat.getSeatNumber(),
                schedule.getScheduleDate(),
                scheduleSeat.getSeatStatus(),
                ticket.isValid()
        );
    }
}
