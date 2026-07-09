import { Client, type IMessage } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { BASE_URL, getAccessToken } from "./api";

// 순번 알림은 이제 "나한테만" 오는 게 아니라, 그 회차를 기다리는 모두에게 방송되는 형태다.
// currentRank는 "지금 몇 번째 사람까지 입장이 허가됐는지"를 뜻하고(내 순번이 아님),
// 내가 몇 번째인지는 등록할 때 받은 myQueueNumber와 비교해서 계산해야 한다.
export interface QueueStatusEvent {
  scheduleId: number;
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
  eventType: "ENTRY_ALLOWED" | "QUEUE_STATUS_UPDATED" | "QUEUE_ERROR";
  data: T;
}

interface ConnectQueueSocketOptions {
  scheduleId: number | string;
  // STOMP 연결 + 구독까지 전부 끝난 다음 호출된다.
  // 대기열 "등록" API는 반드시 이 콜백 안에서 호출해야, 등록하자마자 서버가 곧바로
  // 입장을 허가해도(EntryAllowedEvent) 그 메시지를 놓치지 않는다.
  onConnected: () => void;
  onStatusUpdated: (event: QueueStatusEvent) => void;
  onEntryAllowed: (event: EntryAllowedEvent) => void;
  onError?: (error: unknown) => void;
}

// 대기열 실시간 알림(WebSocket/STOMP)에 연결한다.
// 순번 진행 상황(/status)은 그 회차를 기다리는 모두에게 방송되고,
// 입장 허가(/entry)는 나한테만(1:1) 온다.
export function connectQueueSocket({
  scheduleId,
  onConnected,
  onStatusUpdated,
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
      client.subscribe(`/queue/schedules/${scheduleId}/status`, (message: IMessage) => {
        try {
          const body: QueueEventResponse<QueueStatusEvent> = JSON.parse(message.body);
          onStatusUpdated(body.data);
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