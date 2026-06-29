"use client";

import { useState } from "react";

export default function MyPage() {
  const [showWithdrawModal, setShowWithdrawModal] = useState(false);
  const [cancelTargetId, setCancelTargetId] = useState<number | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const ticketsPerPage = 5;

  const user = {
    name: "홍길동",
    loginId: "exampleId",
    email: "example@naver.com",
  };

  const [tickets, setTickets] = useState([
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
  ]);

  const handleWithdraw = () => {
    setShowWithdrawModal(false);
    alert("회원 탈퇴가 완료되었습니다. (나중에 API 연동)");
  };

  const handleCancel = () => {
    if (cancelTargetId === null) return;
    // 🔗 나중에 여기서 PATCH /api/v1/tickets/cancel/{id} 호출
    setTickets((prev) =>
      prev.map((t) =>
        t.ticketId === cancelTargetId ? { ...t, status: "CANCELED" } : t
      )
    );
    setCancelTargetId(null);
  };

  // 최신순 정렬 (ticketId 높은 게 최근)
  const sortedTickets = [...tickets].sort((a, b) => b.ticketId - a.ticketId);
  const totalPages = Math.ceil(sortedTickets.length / ticketsPerPage);
  const pagedTickets = sortedTickets.slice(
    (currentPage - 1) * ticketsPerPage,
    currentPage * ticketsPerPage
  );

  return (
    <div className="min-h-screen bg-gray-50 p-10">
      <div className="max-w-3xl mx-auto">
        {/* 상단 헤더 */}
        <div className="flex justify-between items-center mb-8">
          <div>
            <p className="text-gray-400 text-sm">안녕하세요</p>
            <h1 className="text-2xl font-bold text-gray-800">{user.name}님 👋</h1>
          </div>
          <button
            onClick={() => setShowWithdrawModal(true)}
            className="px-4 py-2 bg-red-500 hover:bg-red-700 text-white text-sm font-semibold rounded-lg transition"
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
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-bold text-gray-700">내 티켓</h2>
          <span className="text-sm text-gray-400">{tickets.length}개의 티켓</span>
        </div>

        <div className="space-y-6">
          {pagedTickets.map((ticket) => (
            <div
              key={ticket.ticketId}
              className="flex shadow-md rounded-2xl overflow-hidden"
            >
              {/* 왼쪽 포스터 */}
              <div className="relative flex-shrink-0 w-36 bg-gradient-to-br from-blue-200 to-indigo-300 flex items-center justify-center text-white font-bold text-sm">
                포스터
                {/* 반원 홈 제거 */}
              </div>

              {/* 점선 */}
              <div className="border-l-2 border-dashed border-gray-200 my-4" />

              {/* 오른쪽 정보 */}
              <div className="flex-1 bg-white p-6">
                <div className="flex justify-between items-start mb-3">
                  <h3 className="font-bold text-gray-800 text-lg">{ticket.concertName}</h3>
                  <div className="flex items-center gap-2">
                    {/* 취소 버튼을 뱃지 왼쪽에 */}
                    {ticket.status !== "CANCELED" && (
                      <button
                        onClick={() => setCancelTargetId(ticket.ticketId)}
                        className="text-xs bg-red-500 text-white hover:bg-red-700 px-3 py-1 rounded-lg transition font-bold"
                      >
                        예매 취소
                      </button>
                    )}
                    <span className={`px-2 py-1 text-xs rounded-full font-semibold ${ticket.status === "CANCELED"
                        ? "bg-gray-100 text-gray-400"
                        : "bg-green-100 text-green-700"
                      }`}>
                      {ticket.status === "CANCELED" ? "취소됨" : "예매완료"}
                    </span>
                  </div>
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

      {/* 페이징 */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 mt-8">
          <button
            onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
            disabled={currentPage === 1}
            className="px-3 py-2 rounded-lg border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-default"
          >
            이전
          </button>
          {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
            <button
              key={page}
              onClick={() => setCurrentPage(page)}
              className={`w-10 h-10 rounded-lg border text-sm font-semibold ${currentPage === page
                  ? "bg-blue-600 border-blue-600 text-white"
                  : "bg-white border-gray-200 text-gray-600 hover:bg-gray-50"
                }`}
            >
              {page}
            </button>
          ))}
          <button
            onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
            disabled={currentPage === totalPages}
            className="px-3 py-2 rounded-lg border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-default"
          >
            다음
          </button>
        </div>
      )}
    </div>

      {/* 결제 취소 확인 모달 */ }
  {
    cancelTargetId !== null && (
      <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4">
        <div className="bg-white rounded-2xl p-8 max-w-sm w-full">
          <h2 className="text-xl font-bold text-center text-gray-800 mb-3">
            예매를 취소하시겠어요?
          </h2>
          <p className="text-center text-gray-500 text-sm mb-6">
            취소 후에는 되돌릴 수 없습니다.
          </p>
          <div className="flex gap-3">
            <button
              onClick={() => setCancelTargetId(null)}
              className="flex-1 p-3 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-lg font-bold transition"
            >
              돌아가기
            </button>
            <button
              onClick={handleCancel}
              className="flex-1 p-3 bg-red-500 hover:bg-red-600 text-white rounded-lg font-bold transition"
            >
              취소하기
            </button>
          </div>
        </div>
      </div>
    )
  }

  {/* 회원탈퇴 확인 모달 */ }
  {
    showWithdrawModal && (
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
    )
  }
    </div >
  );
}