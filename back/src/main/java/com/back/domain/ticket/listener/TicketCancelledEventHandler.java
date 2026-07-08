package com.back.domain.ticket.listener;

import com.back.domain.ticket.event.TicketCancelledEvent;
import com.back.domain.waiting.service.WaitingQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class TicketCancelledEventHandler {

    private final WaitingQueueService waitingQueueService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTicketCancelled(TicketCancelledEvent event) {
        waitingQueueService.allowEntry(event.concertId(), event.scheduleId());
    }
}
