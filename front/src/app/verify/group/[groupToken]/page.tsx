"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";

const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL;

interface SeatVerifyInfo {
  seatNumber: string;
  isValid: boolean;
}

interface TicketGroupVerifyResponse {
  concertName: string;
  venueName: string;
  scheduleDate: string;
  seats: SeatVerifyInfo[];
}

type VerifyState =
  | { status: "loading" }
  | { status: "found"; data: TicketGroupVerifyResponse }
  | { status: "notfound" }
  | { status: "error" };

export default function GroupVerifyPage() {
  const params = useParams();
  const groupToken = params.groupToken as string;
  const [state, setState] = useState<VerifyState>({ status: "loading" });

  useEffect(() => {
    if (!groupToken) {
      setState({ status: "notfound" });
      return;
    }

    fetch(`${BASE_URL}/api/v1/tickets/verify/group/${groupToken}`)
      .then(async (res) => {
        if (res.status === 404) {
          setState({ status: "notfound" });
          return;
        }
        if (!res.ok) {
          setState({ status: "error" });
          return;
        }
        const json = await res.json();
        setState({ status: "found", data: json.data });
      })
      .catch(() => setState({ status: "error" }));
  }, [groupToken]);

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center p-6">
      <div className="bg-white rounded-2xl shadow-sm p-8 max-w-sm w-full text-center">
        {state.status === "loading" && (
          <p className="text-gray-400 text-sm">검증 중...</p>
        )}

        {state.status === "found" && (
          <>
            <div className="text-5xl mb-4">🎟️</div>
            <h1 className="text-xl font-bold text-gray-800 mb-1">티켓 정보</h1>

            <div className="space-y-3 text-sm text-left border-t border-gray-100 pt-5 mt-4">
              <div className="flex justify-between">
                <span className="text-gray-400 w-20 shrink-0">콘서트</span>
                <span className="text-gray-800 font-semibold text-right">{state.data.concertName}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-400 w-20 shrink-0">공연장</span>
                <span className="text-gray-800 font-semibold text-right">{state.data.venueName}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-400 w-20 shrink-0">일시</span>
                <span className="text-gray-800 font-semibold text-right">
                  {new Date(state.data.scheduleDate).toLocaleString("ko-KR", {
                    year: "numeric",
                    month: "long",
                    day: "numeric",
                    hour: "2-digit",
                    minute: "2-digit",
                  })}
                </span>
              </div>
            </div>

            <div className="mt-5 border-t border-gray-100 pt-4">
              <p className="text-xs text-gray-400 mb-3 text-left">좌석별 유효 여부</p>
              <div className="space-y-2">
                {state.data.seats.map((seat) => (
                  <div
                    key={seat.seatNumber}
                    className={`flex items-center justify-between px-4 py-2.5 rounded-xl text-sm font-semibold ${
                      seat.isValid
                        ? "bg-green-50 text-green-700"
                        : "bg-gray-100 text-gray-400"
                    }`}
                  >
                    <span>{seat.seatNumber}</span>
                    <span>{seat.isValid ? "✅ 유효" : "❌ 취소됨"}</span>
                  </div>
                ))}
              </div>
            </div>
          </>
        )}

        {state.status === "notfound" && (
          <>
            <div className="text-5xl mb-4">❌</div>
            <h1 className="text-xl font-bold text-gray-800 mb-2">유효하지 않은 티켓입니다</h1>
            <p className="text-sm text-gray-400">QR 코드를 다시 확인해주세요.</p>
          </>
        )}

        {state.status === "error" && (
          <>
            <div className="text-5xl mb-4">⚠️</div>
            <h1 className="text-xl font-bold text-gray-800 mb-2">오류가 발생했습니다</h1>
            <p className="text-sm text-gray-400">잠시 후 다시 시도해주세요.</p>
          </>
        )}
      </div>
    </div>
  );
}
