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
      endDate: "2026-06-08",
      ticketPrice: 120000,
      ticketNumber: "TKT-101-20260606-A12",
      status: "BOOKED",
    },
    {
      ticketId: 2,
      concertName: "2026 SUMMER LIVE",
      seatNumber: "B-3",
      startDate: "2026-08-10",
      endDate: "2026-08-12",
      ticketPrice: 150000,
      ticketNumber: "TKT-102-20260810-B3",
      status: "BOOKED",
    },
  ];

  const handleWithdraw = () => {
    setShowWithdrawModal(false);
    alert("회원 탈퇴가 완료되었습니다. (나중에 API 연동)");
  };

  return (
    <div className="min-h-screen bg-gray-50 p-10">
      <div className="max-w-3xl mx-auto">
        {/* 상단 헤더 */}
        <div className="flex justify-between items-center mb-8">
          <div>
            <p className="text-gray-400 text-sm">안녕하세요</p>
            <h1 className="text-2xl font-bold text-gray-800">
              {user.name}님 👋
            </h1>
          </div>
          {/* 빨간 박스 버튼 */}
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
        <h2 className="text-lg font-bold text-gray-700 mb-4">내 티켓</h2>
        <div className="space-y-6">
          {tickets.map((ticket) => (
            <div
              key={ticket.ticketId}
              className="flex shadow-md rounded-2xl overflow-hidden"
            >
              {/* 왼쪽 포스터 영역 */}
              <div className="relative flex-shrink-0 w-36 bg-gradient-to-br from-blue-200 to-indigo-300 flex items-center justify-center text-white font-bold text-sm">
                포스터
                {/* 오른쪽 반원 홈 */}
                <div className="absolute -right-3 top-1/2 -translate-y-1/2 w-6 h-6 bg-gray-50 rounded-full z-10" />
              </div>

              {/* 점선 구분선 */}
              <div className="border-l-2 border-dashed border-gray-200 my-4" />

              {/* 오른쪽 정보 영역 — 오돌토돌한 오른쪽 끝 */}
              <div
                className="flex-1 bg-white p-6"
                style={{
                  // 오른쪽 끝을 오돌토돌하게 (반원들이 파인 모양)
                  maskImage: `radial-gradient(circle 8px at 100% 8px, transparent 100%, black 0),
                               radial-gradient(circle 8px at 100% 24px, transparent 100%, black 0),
                               radial-gradient(circle 8px at 100% 40px, transparent 100%, black 0),
                               radial-gradient(circle 8px at 100% 56px, transparent 100%, black 0),
                               radial-gradient(circle 8px at 100% 72px, transparent 100%, black 0),
                               radial-gradient(circle 8px at 100% 88px, transparent 100%, black 0),
                               radial-gradient(circle 8px at 100% 104px, transparent 100%, black 0),
                               radial-gradient(circle 8px at 100% 120px, transparent 100%, black 0),
                               radial-gradient(circle 8px at 100% 136px, transparent 100%, black 0),
                               radial-gradient(circle 8px at 100% 152px, transparent 100%, black 0),
                               linear-gradient(black, black)`,
                  maskComposite: "intersect",
                  WebkitMaskImage: `radial-gradient(circle 8px at 100% 8px, transparent 100%, black 0),
                                    radial-gradient(circle 8px at 100% 24px, transparent 100%, black 0),
                                    radial-gradient(circle 8px at 100% 40px, transparent 100%, black 0),
                                    radial-gradient(circle 8px at 100% 56px, transparent 100%, black 0),
                                    radial-gradient(circle 8px at 100% 72px, transparent 100%, black 0),
                                    radial-gradient(circle 8px at 100% 88px, transparent 100%, black 0),
                                    radial-gradient(circle 8px at 100% 104px, transparent 100%, black 0),
                                    radial-gradient(circle 8px at 100% 120px, transparent 100%, black 0),
                                    radial-gradient(circle 8px at 100% 136px, transparent 100%, black 0),
                                    radial-gradient(circle 8px at 100% 152px, transparent 100%, black 0),
                                    linear-gradient(black, black)`,
                  WebkitMaskComposite: "destination-in",
                }}
              >
                <div className="flex justify-between items-start mb-3">
                  <h3 className="font-bold text-gray-800 text-lg">
                    {ticket.concertName}
                  </h3>
                  <span className="px-2 py-1 text-xs bg-green-100 text-green-700 rounded-full font-semibold">
                    {ticket.status}
                  </span>
                </div>

                <div className="space-y-1 text-sm text-gray-500">
                  <p>
                    <span className="inline-block w-20 text-gray-400">예매번호</span>
                    <span className="text-gray-600 text-xs">{ticket.ticketNumber}</span>
                  </p>
                  <p>
                    <span className="inline-block w-20 text-gray-400">공연기간</span>
                    {ticket.startDate} ~ {ticket.endDate}
                  </p>
                  <p>
                    <span className="inline-block w-20 text-gray-400">좌석</span>
                    {ticket.seatNumber}
                  </p>
                  <p>
                    <span className="inline-block w-20 text-gray-400">결제금액</span>
                    <span className="text-blue-600 font-bold">
                      {ticket.ticketPrice.toLocaleString()}원
                    </span>
                  </p>
                </div>
              </div>
            </div>
          ))}
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