package com.back.domain.waiting.service;

import com.back.domain.concert.service.ConcertService;
import com.back.domain.queue.event.EntryAllowedEvent;
import com.back.domain.queue.event.QueueStatusEvent;
import com.back.domain.schedule.entity.SeatStatus;
import com.back.domain.schedule.repository.ScheduleSeatRepository;
import com.back.domain.user.repository.UserRepository;
import com.back.domain.waiting.dto.QueueStatusDto;
import com.back.domain.waiting.dto.WaitingQueueResponse;
import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WaitingQueueService {
    private final WaitingQueueManager waitingQueueManager;
    private final UserRepository userRepository;
    private final ConcertService concertService;
    private final ApplicationEventPublisher eventPublisher;
    private final ScheduleSeatRepository scheduleSeatRepository;

    @Value("${queue.entry-token.ttl}")
    private Duration entryTokenTtl;
    @Value("${queue.batch-size}")
    private int batchSize;
    @Value("${queue.max-active-users}")
    private int maxActiveUsers;

    public WaitingQueueResponse registerWaiting(Long concertId, Long scheduleId, Long userId) {
        validateUser(userId);
        concertService.validateConcertScheduleMatch(concertId, scheduleId);

        String activeToken = waitingQueueManager.getActiveToken(scheduleId, userId);
        if (activeToken != null) {
            return WaitingQueueResponse.of(concertId, scheduleId, userId, 0L, 0L, activeToken);
        }

        Long rank = waitingQueueManager.registerWaiting(scheduleId, userId);
        Long myQueueNumber = waitingQueueManager.getQueueSequence(scheduleId, userId);

        allowEntry(concertId, scheduleId);

        return WaitingQueueResponse.of(
                concertId,
                scheduleId,
                userId,
                rank,
                myQueueNumber,
                null
        );
    }

    public WaitingQueueResponse showWaitingRank(Long concertId, Long scheduleId, Long userId) {
        validateUser(userId);
        concertService.validateConcertScheduleMatch(concertId, scheduleId);

        String activeToken = waitingQueueManager.getActiveToken(scheduleId, userId);
        if (activeToken != null) {
            return WaitingQueueResponse.of(concertId, scheduleId, userId, 0L, 0L, activeToken);
        }

        Long rank = waitingQueueManager.showWaitingRank(scheduleId, userId);
        Long myQueueNumber = waitingQueueManager.getQueueSequence(scheduleId, userId);

        return WaitingQueueResponse.of(
                concertId,
                scheduleId,
                userId,
                rank,
                myQueueNumber,
                null
        );
    }

    public void cancelWaiting(Long concertId, Long scheduleId, Long userId) {
        validateUser(userId);
        concertService.validateConcertScheduleMatch(concertId, scheduleId);

        boolean removedFromWaiting = waitingQueueManager.cancelWaiting(scheduleId, userId);
        boolean removedFromActive = !removedFromWaiting
                && waitingQueueManager.cancelActiveUser(scheduleId, userId);

        if (!removedFromWaiting && !removedFromActive) {
            throw new ServiceException(ErrorCode.WAITING_QUEUE_NOT_FOUND);
        }

        if (removedFromActive) {
            allowEntry(concertId, scheduleId);
        } else {
            publishQueueRank(scheduleId);
        }
    }

    public void allowEntry(Long concertId, Long scheduleId) {
        concertService.validateConcertScheduleMatch(concertId, scheduleId);

        long remainingSeats =
                scheduleSeatRepository.countBySchedule_ScheduleIdAndSeatStatus(
                        scheduleId, SeatStatus.AVAILABLE
                );
        long capacity = Math.min(remainingSeats, maxActiveUsers);

        List<Long> userIds = waitingQueueManager.addActiveUser(scheduleId, capacity, batchSize, entryTokenTtl);

        for (Long userId : userIds) {
            String entryToken = waitingQueueManager.issueToken(scheduleId, userId, entryTokenTtl);
            long expiredAt = System.currentTimeMillis() + entryTokenTtl.toMillis();

            eventPublisher.publishEvent(
                    new EntryAllowedEvent(scheduleId, userId, entryToken, expiredAt)
            );
        }
        publishQueueRank(scheduleId);
    }

    private void publishQueueRank(Long scheduleId) {
        QueueStatusDto status = waitingQueueManager.getQueueStatus(scheduleId);

        if (status.totalWaitingCount() == 0) return;

        eventPublisher.publishEvent(
                QueueStatusEvent.of(
                        scheduleId,
                        status.currentAllowedSequence(),
                        status.totalWaitingCount()
                )
        );
    }

    private void validateUser(Long userId) {
        userRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));
    }
}
