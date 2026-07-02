"use client";

import { useState, useEffect } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import { apiFetch, decodeToken } from "@/lib/api";

interface PaymentTicketResponse {
  ticketNumber: string;
  urlPoster: string;
  concertName: string;
  seatNumber: string;
  scheduleDate: string;
  seatStatus: string;
  isValid: boolean;
}

export default function PaymentPage() {
  const searchParams = useSearchParams();
  const router = useRouter();

  const concertId = searchParams.get("concertId");
  const scheduleId = searchParams.get("scheduleId");
  const seatNumber = searchParams.get("seatNumber");
  const occupyToken = searchParams.get("occupyToken");
  const price = Number(searchParams.get("price") ?? 0);

  const [agreed, setAgreed] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [ticketResult, setTicketResult] = useState<PaymentTicketResponse | null>(null);
  const [timeLeft, setTimeLeft] = useState(600);

  useEffect(() => {
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

  const formatTime = (seconds: number) => {
    const min = Math.floor(seconds / 60);
    const sec = seconds % 60;
    return `${min}:${sec.toString().padStart(2, "0")}`;
  };

  const handlePayment = async () => {
    if (!agreed) {
      alert("약관에 동의해주세요.");
      return;
    }
    if (!concertId || !scheduleId || !seatNumber || !occupyToken) {
      alert("예매 정보가 올바르지 않습니다. 좌석 선택부터 다시 진행해주세요.");
      return;
    }

    if (!decodeToken()) {
      alert("로그인이 필요합니다.");
      router.push("/login");
      return;
    }

    try {
      const res = await apiFetch<PaymentTicketResponse>("/tickets/reserve", {
        method: "POST",
        body: JSON.stringify({
          concertId: Number(concertId),
          scheduleId: Number(scheduleId),
          seatNumber,
          occupyToken,
        }),
      });
      setTicketResult(res.data);
      setShowModal(true);
    } catch (e) {
      alert(e instanceof Error ? e.message : "결제 중 오류가 발생했습니다.");
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
          <h2 className="font-bold text-gray-700 mb-4">예매 정보</h2>
          <div className="space-y-2 text-gray-600">
            <p>
              <span className="inline-block w-24 text-gray-400">좌석</span>
              {seatNumber ?? "-"}
            </p>
            <p>
              <span className="inline-block w-24 text-gray-400">결제 금액</span>
              <span className="text-blue-600 font-bold">{price.toLocaleString()}원</span>
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
          className="w-full p-4 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-bold text-lg transition"
        >
          결제하기
        </button>
      </div>

      {showModal && ticketResult && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl p-8 max-w-md w-full">
            <h2 className="text-xl font-bold text-center text-gray-800 mb-6">
              🎉 결제가 완료되었습니다!
            </h2>
            <div className="space-y-2 text-gray-600 mb-6">
              <p><span className="inline-block w-24 text-gray-400">티켓 번호</span>{ticketResult.ticketNumber}</p>
              <p><span className="inline-block w-24 text-gray-400">콘서트</span>{ticketResult.concertName}</p>
              <p><span className="inline-block w-24 text-gray-400">좌석</span>{ticketResult.seatNumber}</p>
              <p>
                <span className="inline-block w-24 text-gray-400">공연 일시</span>
                {ticketResult.scheduleDate?.slice(0, 16).replace("T", " ")}
              </p>
            </div>
            <button
              onClick={() => router.push("/mypage")}
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