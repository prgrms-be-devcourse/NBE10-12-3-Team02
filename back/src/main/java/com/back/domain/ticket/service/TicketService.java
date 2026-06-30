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
import com.back.global.exception.ErrorCode;
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
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        Schedule schedule = scheduleRepository
                .findByScheduleIdAndConcert_ConcertId(request.scheduleId(), request.concertId())
                .orElseThrow(() -> new ServiceException(ErrorCode.INVALID_CONCERT_SCHEDULE));

        ScheduleSeat scheduleSeat = scheduleSeatRepository
                .findWithLockByScheduleIdAndSeatNumber(request.scheduleId(), request.seatNumber())
                .orElseThrow(() -> new ServiceException(ErrorCode.SEAT_NOT_FOUND));

        if (scheduleSeat.getSeatStatus() != SeatStatus.HOLD) {
            throw new ServiceException(ErrorCode.SEAT_SOLD_OUT);
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
                .orElseThrow(() -> new ServiceException(ErrorCode.TICKET_NOT_FOUND_FOR_USER));

        if (!ticket.isValid()) {
            throw new ServiceException(ErrorCode.TICKET_ALREADY_CANCELLED);
        }

        ticket.updateIsValid(false);
        ticket.getScheduleSeat().updateSeatStatus(SeatStatus.AVAILABLE);
    }

    public String createTicketNumber() {
        return UUID.randomUUID().toString();
    }

    private ServiceException exception(ErrorCode errorCode) {
        return new ServiceException(errorCode);
    }
}