package com.back.domain.ticket.listener;

import com.back.domain.ticket.event.PaymentCompletedEvent;
import com.back.domain.waiting.service.WaitingQueueManager;
import com.back.domain.waiting.service.WaitingQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PaymentCompletedEventHandler {

    private final WaitingQueueManager waitingQueueManager;
    private final WaitingQueueService waitingQueueService;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handlePaymentCompleted(
            PaymentCompletedEvent event
    ) {
        waitingQueueManager.removeActiveUser(
                event.scheduleId(),
                event.userId()
        );

        waitingQueueService.allowEntry(
                event.concertId(),
                event.scheduleId()
        );
    }
}