"use client";

import { Suspense, useState, useEffect, useRef } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import { apiFetch, decodeToken, restoreSession } from "@/lib/api";
import { showAlert, showError } from "@/lib/alert";
import { Loader2 } from "lucide-react";

interface PaymentTicketResponse {
  ticketNumber: string;
  urlPoster: string;
  concertName: string;
  seatNumber: string;
  scheduleDate: string;
  seatStatus: string;
  isValid: boolean;
}

interface OccupiedSeat {
  seatNumber: string;
  occupyToken: string;
  price: number;
}

function parseSeats(raw: string | null): OccupiedSeat[] {
  if (!raw) return [];
  try {
    const parsed = JSON.parse(decodeURIComponent(raw));
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function PaymentContent() {
  const searchParams = useSearchParams();
  const router = useRouter();

  const concertId = searchParams.get("concertId");
  const scheduleId = searchParams.get("scheduleId");
  const seats = parseSeats(searchParams.get("seats"));
  const queueToken = searchParams.get("queueToken");

  const totalPrice = seats.reduce((sum, s) => sum + s.price, 0);

  const [agreed, setAgreed] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [ticketResults, setTicketResults] = useState<PaymentTicketResponse[]>([]);
  const [timeLeft, setTimeLeft] = useState(600);
  const paymentCompletedRef = useRef(false);
  const isMountedRef = useRef(false);
  const [isProcessing, setIsProcessing] = useState(false);
  const releasedRef = useRef(false);

  // 선점해뒀던 좌석들을 전부 풀어준다. 결제 실패/이탈 등 여러 상황에서 재사용한다.
  const releaseSeats = () => {
    if (releasedRef.current || paymentCompletedRef.current) return;
    if (!concertId || !scheduleId) return;
    releasedRef.current = true;
    seats.forEach(({ seatNumber }) => {
      apiFetch(`/concerts/${concertId}/schedules/${scheduleId}/seats/occupy`, {
        method: "DELETE",
        body: JSON.stringify({ seatNumber }),
      }).catch(() => {});
    });
  };

  useEffect(() => {
    restoreSession();
    const timer = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, []);

  useEffect(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
      setTimeout(() => {
        if (!isMountedRef.current) {
          releaseSeats();
        }
      }, 50);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [concertId, scheduleId]);

  const formatTime = (seconds: number) => {
    const min = Math.floor(seconds / 60);
    const sec = seconds % 60;
    return `${min}:${sec.toString().padStart(2, "0")}`;
  };

  const handlePayment = async () => {
    if (!agreed) {
      showAlert("약관에 동의해주세요.");
      return;
    }
    if (!concertId || !scheduleId || seats.length === 0) {
      showAlert("예매 정보가 올바르지 않습니다. 좌석 선택부터 다시 진행해주세요.");
      return;
    }

    await restoreSession();
    if (!decodeToken()) {
      await showAlert("로그인이 필요합니다.");
      router.replace("/login");
      return;
    }

    setIsProcessing(true);
    try {
      await new Promise((resolve) => setTimeout(resolve, 2000));

      const res = await apiFetch<PaymentTicketResponse[]>(
        `/tickets/reserve/schedule/${scheduleId}`,
        {
          method: "POST",
          headers: queueToken ? { "X-Queue-Token": queueToken } : undefined,
          body: JSON.stringify({
            concertId: Number(concertId),
            seatHolds: seats.map(({ seatNumber, occupyToken }) => ({
              seatNumber,
              occupyToken,
            })),
          }),
        },
      );

      paymentCompletedRef.current = true;
      setTicketResults(res.data);
      setShowModal(true);
    } catch (e) {
      await showError(e instanceof Error ? e.message : "결제 중 오류가 발생했습니다.");
      // 결제(예매)가 실패했으니, 선점해뒀던 좌석을 바로 풀어주고 좌석 선택 페이지로 돌려보낸다.
      releaseSeats();
      if (concertId) {
        router.replace(`/concerts/${concertId}/seats?scheduleId=${scheduleId}`);
      }
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 p-10">
      <div className="max-w-2xl mx-auto">
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-2xl font-bold text-gray-800">예매 정보 입력</h1>
          <div className="text-red-500 font-bold">예매 가능 시간 {formatTime(timeLeft)}</div>
        </div>

        <div className="bg-white rounded-2xl shadow-sm p-8 mb-6">
          <h2 className="font-bold text-gray-700 mb-4">예매 정보 ({seats.length}매)</h2>
          <div className="space-y-3 text-gray-600">
            {seats.map((s) => (
              <div key={s.seatNumber} className="flex justify-between text-sm border-b border-gray-100 pb-2">
                <span>좌석 {s.seatNumber}</span>
                <span className="font-semibold text-gray-700">{s.price.toLocaleString()}원</span>
              </div>
            ))}
            <p className="pt-2">
              <span className="inline-block w-24 text-gray-400">결제 금액</span>
              <span className="text-blue-600 font-bold">{totalPrice.toLocaleString()}원</span>
            </p>
          </div>
        </div>

        <div className="bg-white rounded-2xl shadow-sm p-8 mb-6">
          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="checkbox"
              checked={agreed}
              onChange={(e) => setAgreed(e.target.checked)}
              className="w-5 h-5"
            />
            <span className="text-gray-700">예매 및 취소/환불 약관에 동의합니다.</span>
          </label>
        </div>

        <button
          onClick={handlePayment}
          disabled={isProcessing}
          className="w-full p-4 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-bold text-lg transition disabled:bg-blue-400 disabled:cursor-not-allowed"
        >
          결제하기
        </button>
      </div>

      {isProcessing && (
        <div className="fixed inset-0 z-50 bg-black/30 backdrop-blur-[2px] flex items-center justify-center">
          <div className="bg-white rounded-2xl p-8 shadow-2xl flex flex-col items-center gap-4 max-w-xs w-full border border-gray-100">
            <Loader2 className="h-10 w-10 text-blue-600 animate-spin" />
            <div className="text-center">
              <h3 className="font-bold text-gray-800 text-lg">결제 처리 중</h3>
              <p className="text-xs text-gray-400 mt-1">안전하게 예매를 완료하고 있습니다.</p>
            </div>
          </div>
        </div>
      )}

      {showModal && ticketResults.length > 0 && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl p-8 max-w-md w-full max-h-[85vh] overflow-y-auto">
            <h2 className="text-xl font-bold text-center text-gray-800 mb-6">
              🎉 결제가 완료되었습니다! ({ticketResults.length}매)
            </h2>
            <div className="space-y-4 mb-6">
              {ticketResults.map((ticket) => (
                <div key={ticket.ticketNumber} className="space-y-2 text-gray-600 border-b border-gray-100 pb-4 last:border-none">
                  <div className="flex items-start gap-2">
                    <span className="w-20 flex-shrink-0 text-gray-400">티켓 번호</span>
                    <span className="break-all text-sm">{ticket.ticketNumber}</span>
                  </div>
                  <div className="flex items-start gap-2">
                    <span className="w-20 flex-shrink-0 text-gray-400">콘서트</span>
                    <span className="break-words text-sm">{ticket.concertName}</span>
                  </div>
                  <div className="flex items-start gap-2">
                    <span className="w-20 flex-shrink-0 text-gray-400">좌석</span>
                    <span className="text-sm">{ticket.seatNumber}</span>
                  </div>
                  <div className="flex items-start gap-2">
                    <span className="w-20 flex-shrink-0 text-gray-400">공연 일시</span>
                    <span className="text-sm">{ticket.scheduleDate?.slice(0, 16).replace("T", " ")}</span>
                  </div>
                </div>
              ))}
            </div>
            <button
              onClick={() => router.replace("/mypage")}
              className="w-full p-3 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-bold transition"
            >
              마이페이지로 이동
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export default function PaymentPage() {
  return (
    <Suspense fallback={<p className="text-center text-gray-400 py-20">불러오는 중...</p>}>
      <PaymentContent />
    </Suspense>
  );
}