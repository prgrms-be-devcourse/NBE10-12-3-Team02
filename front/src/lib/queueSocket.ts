import { Client, type IMessage } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { BASE_URL, getAccessToken } from "./api";

export interface QueueRankUpdatedEvent {
  scheduleId: number;
  userId: number;
  currentRank: number;
  totalWaitingCount: number;
  updatedAt: string;
}

export interface EntryAllowedEvent {
  scheduleId: number;
  userId: number;
  entryToken: string;
  expiredAt: number;
}

interface QueueEventResponse<T> {
  eventType: "ENTRY_ALLOWED" | "QUEUE_RANK_UPDATED" | "QUEUE_ERROR";
  data: T;
}

interface ConnectQueueSocketOptions {
  scheduleId: number | string;
  // STOMP 연결 + 구독까지 전부 끝난 다음 호출된다.
  // 대기열 "등록" API는 반드시 이 콜백 안에서 호출해야, 등록하자마자 서버가 곧바로
  // 입장을 허가해도(EntryAllowedEvent) 그 메시지를 놓치지 않는다.
  onConnected: () => void;
  onRankUpdated: (event: QueueRankUpdatedEvent) => void;
  onEntryAllowed: (event: EntryAllowedEvent) => void;
  onError?: (error: unknown) => void;
}

// 대기열 실시간 알림(WebSocket/STOMP)에 연결한다.
// 서버는 순번이 바뀔 때(/status)와 입장이 허가됐을 때(/entry) 두 종류의 메시지를 보내준다.
export function connectQueueSocket({
  scheduleId,
  onConnected,
  onRankUpdated,
  onEntryAllowed,
  onError,
}: ConnectQueueSocketOptions): Client {
  const client = new Client({
    webSocketFactory: () => new SockJS(`${BASE_URL}/ws`),
    connectHeaders: {
      Authorization: `Bearer ${getAccessToken() ?? ""}`,
    },
    reconnectDelay: 3000,
    onConnect: () => {
      client.subscribe(`/user/queue/schedules/${scheduleId}/status`, (message: IMessage) => {
        try {
          const body: QueueEventResponse<QueueRankUpdatedEvent> = JSON.parse(message.body);
          onRankUpdated(body.data);
        } catch (e) {
          onError?.(e);
        }
      });

      client.subscribe(`/user/queue/schedules/${scheduleId}/entry`, (message: IMessage) => {
        try {
          const body: QueueEventResponse<EntryAllowedEvent> = JSON.parse(message.body);
          onEntryAllowed(body.data);
        } catch (e) {
          onError?.(e);
        }
      });

      onConnected();
    },
    onStompError: (frame) => {
      onError?.(frame);
    },
  });

  client.activate();
  return client;
}