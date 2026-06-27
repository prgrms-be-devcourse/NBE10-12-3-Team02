"use client";

import { useState } from "react";

export default function MyPage() {
  const [showWithdrawModal, setShowWithdrawModal] = useState(false);

  const user = {
    name: "홍길동",
    loginId: "exampleId",
    email: "example@naver.com",
  };

  const tickets = [
    {
      ticketId: 1,
      concertName: "흠뻑쇼 - 대전",
      seatNumber: "A-12",
      startDate: "2026-06-06",
      ticketPrice: 120000,
      status: "BOOKED",
    },
    {
      ticketId: 2,
      concertName: "2026 SUMMER LIVE",
      seatNumber: "B-3",
      startDate: "2026-08-10",
      ticketPrice: 150000,
      status: "BOOKED",
    },
  ];

  const handleWithdraw = () => {
    // 나중에 여기서 회원탈퇴 API 호출
    setShowWithdrawModal(false);
    alert("회원 탈퇴가 완료되었습니다. (나중에 API 연동)");
  };

  return (
    <div className="min-h-screen bg-gray-50 p-10">
      <div className="max-w-3xl mx-auto">
        {/* 상단 헤더 */}
        <div className="flex justify-between items-center mb-8">
          <h1 className="text-2xl font-bold text-gray-800">마이페이지</h1>
          <button
            onClick={() => setShowWithdrawModal(true)}
            className="px-4 py-2 bg-red-500 hover:bg-red-600 text-white text-sm font-semibold rounded-lg transition"
          >
            회원탈퇴
          </button>
        </div>

        {/* 사용자 정보 카드 */}
        <div className="bg-white rounded-2xl shadow-sm p-8 mb-8">
          <h2 className="text-lg font-bold text-gray-700 mb-4">내 정보</h2>
          <div className="space-y-2 text-gray-600">
            <p><span className="inline-block w-20 text-gray-400">이름</span>{user.name}</p>
            <p><span className="inline-block w-20 text-gray-400">아이디</span>{user.loginId}</p>
            <p><span className="inline-block w-20 text-gray-400">이메일</span>{user.email}</p>
          </div>
        </div>

        {/* 내 티켓 목록 */}
        <div className="bg-white rounded-2xl shadow-sm p-8">
          <h2 className="text-lg font-bold text-gray-700 mb-4">내 티켓</h2>
          <div className="space-y-4">
            {tickets.map((ticket) => (
              <div
                key={ticket.ticketId}
                className="flex justify-between items-center border border-gray-100 rounded-xl p-4 hover:shadow-md transition"
              >
                <div>
                  <p className="font-bold text-gray-800">{ticket.concertName}</p>
                  <p className="text-sm text-gray-500 mt-1">
                    {ticket.startDate} · 좌석 {ticket.seatNumber}
                  </p>
                </div>
                <div className="text-right">
                  <p className="font-bold text-blue-600">
                    {ticket.ticketPrice.toLocaleString()}원
                  </p>
                  <span className="inline-block mt-1 px-2 py-0.5 text-xs bg-green-100 text-green-700 rounded-full">
                    {ticket.status}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* 회원탈퇴 확인 모달 */}
      {showWithdrawModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl p-8 max-w-sm w-full">
            <h2 className="text-xl font-bold text-center text-gray-800 mb-3">
              정말 탈퇴하시겠어요?
            </h2>
            <p className="text-center text-gray-500 text-sm mb-6">
              탈퇴 시 모든 예매 내역이 사라지며,<br />
              되돌릴 수 없습니다.
            </p>
            <div className="flex gap-3">
              <button
                onClick={() => setShowWithdrawModal(false)}
                className="flex-1 p-3 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-lg font-bold transition"
              >
                취소
              </button>
              <button
                onClick={handleWithdraw}
                className="flex-1 p-3 bg-red-500 hover:bg-red-600 text-white rounded-lg font-bold transition"
              >
                탈퇴하기
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}