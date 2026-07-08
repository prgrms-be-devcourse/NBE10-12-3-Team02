package com.back.domain.ticket.listener;

import com.back.domain.ticket.event.TicketCancelledEvent;
import com.back.domain.waiting.service.WaitingQueueManager;
import com.back.domain.waiting.service.WaitingQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class TicketCancelledEventHandler {

    private final WaitingQueueService waitingQueueService;
    private final WaitingQueueManager waitingQueueManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTicketCancelled(TicketCancelledEvent event) {
        waitingQueueManager.removeActiveUser(
                event.scheduleId(),
                event.userId()
        );
        waitingQueueService.allowEntry(event.concertId(), event.scheduleId());
    }
}
