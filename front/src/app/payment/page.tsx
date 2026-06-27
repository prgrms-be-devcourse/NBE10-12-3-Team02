"use client";

import { useState, useEffect } from "react";

export default function PaymentPage() {
  const [agreed, setAgreed] = useState(false);
  const [showModal, setShowModal] = useState(false);

  // 남은 시간 (초 단위), 10분 = 600초
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

  // 초 → "M:SS" 형식으로 변환
  const formatTime = (seconds: number) => {
    const min = Math.floor(seconds / 60);
    const sec = seconds % 60;
    return `${min}:${sec.toString().padStart(2, "0")}`;
  };

  // 가짜 예매 정보 (나중에 좌석 페이지에서 넘겨받을 값)
  const booking = {
    concertName: "2026 SUMMER LIVE",
    seatNumber: "A-1",
    price: 150000,
    dateTime: "2026-08-10 19:00",
  };

  const handlePayment = () => {
    if (!agreed) {
      alert("약관에 동의해주세요.");
      return;
    }
    // 나중에 여기서 결제 API 호출
    setShowModal(true);
  };

  return (
    <div className="min-h-screen bg-gray-50 p-10">
      <div className="max-w-2xl mx-auto">
        {/* 제목 + 타이머 */}
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-2xl font-bold text-gray-800">예매 정보 입력</h1>
          <div className="text-red-500 font-bold">
            예매 가능 시간 {formatTime(timeLeft)}
          </div>
        </div>

        {/* 예매 정보 */}
        <div className="bg-white rounded-2xl shadow-sm p-8 mb-6">
          <h2 className="font-bold text-gray-700 mb-4">예매 정보</h2>
          <div className="space-y-2 text-gray-600">
            <p>
              <span className="inline-block w-24 text-gray-400">콘서트</span>
              {booking.concertName}
            </p>
            <p>
              <span className="inline-block w-24 text-gray-400">공연 일시</span>
              {booking.dateTime}
            </p>
            <p>
              <span className="inline-block w-24 text-gray-400">좌석</span>
              {booking.seatNumber}
            </p>
            <p>
              <span className="inline-block w-24 text-gray-400">결제 금액</span>
              <span className="text-blue-600 font-bold">
                {booking.price.toLocaleString()}원
              </span>
            </p>
          </div>
        </div>

        {/* 약관 동의 */}
        <div className="bg-white rounded-2xl shadow-sm p-8 mb-6">
          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="checkbox"
              checked={agreed}
              onChange={(e) => setAgreed(e.target.checked)}
              className="w-5 h-5"
            />
            <span className="text-gray-700">
              예매 및 취소/환불 약관에 동의합니다.
            </span>
          </label>
        </div>

        {/* 결제 버튼 */}
        <button
          onClick={handlePayment}
          className="w-full p-4 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-bold text-lg transition"
        >
          결제하기
        </button>
      </div>

      {/* 결제 완료 모달 */}
      {showModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl p-8 max-w-md w-full">
            <h2 className="text-xl font-bold text-center text-gray-800 mb-6">
              🎉 결제가 완료되었습니다!
            </h2>
            <div className="space-y-2 text-gray-600 mb-6">
              <p>
                <span className="inline-block w-24 text-gray-400">티켓 번호</span>
                TKT-101-A1
              </p>
              <p>
                <span className="inline-block w-24 text-gray-400">콘서트</span>
                {booking.concertName}
              </p>
              <p>
                <span className="inline-block w-24 text-gray-400">좌석</span>
                {booking.seatNumber}
              </p>
              <p>
                <span className="inline-block w-24 text-gray-400">공연 일시</span>
                {booking.dateTime}
              </p>
            </div>
            <button
              onClick={() => setShowModal(false)}
              className="w-full p-3 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-bold transition"
            >
              확인
            </button>
          </div>
        </div>
      )}
    </div>
  );
}