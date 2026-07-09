package com.back.domain.queue.listener;

import com.back.domain.queue.constant.QueueEventType;
import com.back.domain.queue.dto.QueueEventResponse;
import com.back.domain.queue.event.EntryAllowedEvent;
import com.back.domain.queue.event.QueueStatusEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Async
public class QueueEntryEventHandler {
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleQueueStatusUpdated(QueueStatusEvent event) {
        QueueEventResponse<QueueStatusEvent> response =
                QueueEventResponse.of(QueueEventType.QUEUE_STATUS_UPDATED, event);

        messagingTemplate.convertAndSend(
                "/queue/schedules/%s/status".formatted(event.scheduleId()),
                response
        );
    }

    @EventListener
    public void handleEntryAllowed(EntryAllowedEvent event) {
        QueueEventResponse<EntryAllowedEvent> response =
                QueueEventResponse.of(QueueEventType.ENTRY_ALLOWED, event);

        messagingTemplate.convertAndSendToUser(
                event.userId().toString(),
                "/queue/schedules/%s/entry".formatted(event.scheduleId()),
                response
        );
    }
}
