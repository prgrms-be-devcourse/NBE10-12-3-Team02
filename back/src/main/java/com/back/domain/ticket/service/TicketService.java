package com.back.domain.ticket.service;

import com.back.domain.schedule.entity.Schedule;
import com.back.domain.schedule.entity.ScheduleSeat;
import com.back.domain.schedule.entity.SeatStatus;
import com.back.domain.schedule.repository.ScheduleRepository;
import com.back.domain.schedule.repository.ScheduleSeatRepository;
import com.back.domain.ticket.dto.PaymentTicketRequest;
import com.back.domain.ticket.dto.PaymentTicketResponse;
import com.back.domain.ticket.entity.Ticket;
import com.back.domain.ticket.repository.TicketRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketService {
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleSeatRepository scheduleSeatRepository;

    @Transactional
    public PaymentTicketResponse createTicket(Long userId, PaymentTicketRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException("404-1", "유저가 존재하지 않습니다."));

        Schedule schedule = scheduleRepository
                .findByScheduleIdAndConcert_ConcertId(request.scheduleId(), request.concertId())
                .orElseThrow(() -> new ServiceException("400-1", "해당 콘서트의 회차가 아닙니다."));

        ScheduleSeat scheduleSeat = scheduleSeatRepository
                .findWithLockByScheduleIdAndSeatNumber(request.scheduleId(), request.seatNumber())
                .orElseThrow(() -> new ServiceException("404-2", "해당 좌석이 존재하지 않습니다."));

        if (scheduleSeat.getSeatStatus() != SeatStatus.HOLD) {
            throw new ServiceException("400-2", "이미 매진된 좌석입니다.");
        }

        scheduleSeat.updateSeatStatus(SeatStatus.SOLD_OUT);

        Ticket ticket = Ticket.create(
                user,
                schedule,
                scheduleSeat,
                createTicketNumber(),
                scheduleSeat.getSeatPrice()
                );

        ticketRepository.save(ticket);

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
    @Transactional
    public void cancelTicket(Long userId, Long ticketId) {

        Ticket ticket = ticketRepository.findByTicketIdAndUser_UserId(ticketId, userId)
                .orElseThrow(() -> new ServiceException("404-2", "해당 티켓이 존재하지 않습니다."));

        if (!ticket.isValid()) {
            throw new ServiceException("400-3", "이미 취소된 티켓입니다.");
        }

        ticket.updateIsValid(false);
        ticket.getScheduleSeat().updateSeatStatus(SeatStatus.AVAILABLE);
    }

    public String createTicketNumber() {
        return UUID.randomUUID().toString();
    }
}
