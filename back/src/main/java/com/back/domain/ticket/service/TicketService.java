package com.back.domain.ticket.service;

import com.back.domain.concert.service.ConcertService;
import com.back.domain.concert.service.SeatOccupyManager;
import com.back.domain.schedule.entity.Schedule;
import com.back.domain.schedule.entity.ScheduleSeat;
import com.back.domain.schedule.entity.SeatStatus;
import com.back.domain.schedule.repository.ScheduleRepository;
import com.back.domain.schedule.repository.ScheduleSeatRepository;
import com.back.domain.ticket.dto.PaymentTicketRequest;
import com.back.domain.ticket.dto.PaymentTicketResponse;
import com.back.domain.ticket.dto.SeatHoldInfo;
import com.back.domain.ticket.entity.Ticket;
import com.back.domain.ticket.event.PaymentCompletedEvent;
import com.back.domain.ticket.event.TicketCancelledEvent;
import com.back.domain.ticket.repository.TicketRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.domain.waiting.service.WaitingQueueService;
import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketService {
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleSeatRepository scheduleSeatRepository;
    private final StringRedisTemplate redisTemplate;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public List<PaymentTicketResponse> createTicket(Long userId, Long scheduleId, PaymentTicketRequest request) {
        User user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        Schedule schedule = scheduleRepository
                .findByScheduleIdAndConcert_ConcertId(scheduleId, request.concertId())
                .orElseThrow(() -> new ServiceException(ErrorCode.INVALID_CONCERT_SCHEDULE));

        long alreadyPurchasedCount = ticketRepository.countByUser_UserIdAndSchedule_ScheduleIdAndIsValidTrue(userId, scheduleId);
        if (alreadyPurchasedCount + request.seatHolds().size() > 3) {
            throw new ServiceException(ErrorCode.EXCEED_TICKET_LIMIT);
        }

        List<SeatHoldInfo> sortedSeatHolds = request.seatHolds().stream()
                .sorted(Comparator.comparing(SeatHoldInfo::seatNumber))
                .toList();

        List<ScheduleSeat> scheduleSeats = new ArrayList<>();
        for (SeatHoldInfo holdInfo : sortedSeatHolds) {
            ScheduleSeat scheduleSeat = scheduleSeatRepository
                    .findWithLockByScheduleIdAndSeatNumber(scheduleId, holdInfo.seatNumber())
                    .orElseThrow(() -> new ServiceException(ErrorCode.SEAT_NOT_FOUND));
            if (scheduleSeat.getSeatStatus() == SeatStatus.SOLD_OUT) {
                throw new ServiceException(ErrorCode.SEAT_ALREADY_SOLD);
            }
            scheduleSeats.add(scheduleSeat);
        }

        validateSeatHold(userId, request.concertId(), scheduleId, sortedSeatHolds);

        scheduleSeats.forEach(seat -> seat.updateSeatStatus(SeatStatus.SOLD_OUT));

        List<String> redisKeys = sortedSeatHolds.stream()
                .map(hold -> SeatOccupyManager.generateSeatOccupyKey(request.concertId(), scheduleId, hold.seatNumber()))
                .toList();
        redisTemplate.delete(redisKeys);

        List<Ticket> tickets = scheduleSeats.stream()
                .map(seat -> Ticket.create(
                        user,
                        schedule,
                        seat,
                        createTicketNumber(),
                        seat.getSeatPrice()
                ))
                .toList();
        ticketRepository.saveAll(tickets);

        eventPublisher.publishEvent(
                new PaymentCompletedEvent(
                        request.concertId(),
                        scheduleId,
                        userId
                )
        );

        return IntStream.range(0, tickets.size())
                .mapToObj(i -> PaymentTicketResponse.from(scheduleSeats.get(i), schedule, tickets.get(i)))
                .toList();
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

        removeSeatHold(
                ticket.getSchedule().getConcert().getConcertId(),
                ticket.getSchedule().getScheduleId(),
                ticket.getScheduleSeat().getSeatNumber()
        );
        eventPublisher.publishEvent(new TicketCancelledEvent(
                ticket.getSchedule().getConcert().getConcertId(),
                ticket.getSchedule().getScheduleId(),
                userId
        ));
    }

    public String createTicketNumber() {
        return UUID.randomUUID().toString();
    }

    private void validateSeatHold(Long userId, Long concertId, Long scheduleId, List<SeatHoldInfo> seatHolds) {
        for (SeatHoldInfo hold : seatHolds) {
            String redisKey = SeatOccupyManager.generateSeatOccupyKey(concertId, scheduleId, hold.seatNumber());

            List<Object> values = redisTemplate.opsForHash().multiGet(redisKey, List.of("userId", "occupyToken"));

            if (values.get(0) == null || values.get(1) == null) {
                throw new ServiceException(ErrorCode.SEAT_HOLD_EXPIRED);
            }

            String holdUserId = (String) values.get(0);
            String holdOccupyToken = (String) values.get(1);
            if (!userId.toString().equals(holdUserId)) {
                throw new ServiceException(ErrorCode.SEAT_HELD_BY_OTHER_USER);
            }
            if (!hold.occupyToken().equals(holdOccupyToken)) {
                throw new ServiceException(ErrorCode.INVALID_OCCUPY_TOKEN);
            }
        }
    }

    private void removeSeatHold(Long concertId, Long scheduleId, String seatNumber) {
        String redisKey = SeatOccupyManager.generateSeatOccupyKey(concertId, scheduleId, seatNumber);
        redisTemplate.delete(redisKey);

        String indexKey = SeatOccupyManager.generateSeatOccupyIndexKey(concertId, scheduleId);
        redisTemplate.opsForZSet().remove(indexKey, seatNumber);
    }
}