"use client";

import { useState, useEffect, useRef } from "react";
import { useRouter } from "next/navigation";
import { apiFetch, decodeToken, restoreSession, setAccessToken } from "@/lib/api";
import { showAlert, showSuccess, showError } from "@/lib/alert";
import { getLocalConcertPoster } from "@/lib/concertDetailImages";
import { Loader2 } from "lucide-react";
import PasswordStrengthMeter from "@/app/components/PasswordStrengthMeter";

interface TicketInfo {
  ticketId: number;
  urlPoster: string;
  concertName: string;
  startDate: string;
  endDate: string;
  isValid: boolean;
  ticketPrice: number;
  createdAt: string;
  ticketNumber: string;
}

interface MyPageData {
  name: string;
  id: string;
  email: string;
  loginType: string;
  ticketList: TicketInfo[];
}

type StatusFilter = "all" | "valid" | "canceled";

export default function MyPage() {
  const router = useRouter();
  const hasCheckedAuth = useRef(false);

  const [data, setData] = useState<MyPageData | null>(null);
  const [loading, setLoading] = useState(true);
  const [showWithdrawModal, setShowWithdrawModal] = useState(false);
  const [cancelTargetId, setCancelTargetId] = useState<number | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [isProcessing, setIsProcessing] = useState(false);
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("all");
  const ticketsPerPage = 5;

  const [isEditing, setIsEditing] = useState(false);
  const [editName, setEditName] = useState("");
  const [editEmail, setEditEmail] = useState("");
  const [editPassword, setEditPassword] = useState("");
  const [editPasswordCheck, setEditPasswordCheck] = useState("");
  const [isSavingProfile, setIsSavingProfile] = useState(false);

  useEffect(() => {
    if (hasCheckedAuth.current) return;
    hasCheckedAuth.current = true;

    const initializeMyPage = async () => {
      await restoreSession();

      if (!decodeToken()) {
        await showAlert("로그인이 필요합니다.");
        router.push("/login");
        return;
      }

      try {
        const res = await apiFetch<MyPageData>(`/users/me`);
        setData(res.data);
      } catch (e) {
        showError(e instanceof Error ? e.message : "마이페이지 조회에 실패했습니다.");
      } finally {
        setLoading(false);
      }
    };

    initializeMyPage();
  }, []);

  const handleWithdraw = async () => {
    try {
      await apiFetch(`/users/withdraw`, { method: "PATCH" });
      setAccessToken(null);
      await showSuccess("회원 탈퇴가 완료되었습니다.");
      router.push("/");
    } catch (e) {
      showError(e instanceof Error ? e.message : "탈퇴 처리 중 오류가 발생했습니다.");
    } finally {
      setShowWithdrawModal(false);
    }
  };

  const handleCancel = async () => {
    if (cancelTargetId === null) return;
    setIsProcessing(true);
    try {
      await new Promise((resolve) => setTimeout(resolve, 2000));
      await apiFetch(`/tickets/cancel/${cancelTargetId}`, { method: "PATCH" });
      setData((prev) =>
        prev
          ? {
              ...prev,
              ticketList: prev.ticketList.map((t) =>
                t.ticketId === cancelTargetId ? { ...t, isValid: false } : t
              ),
            }
          : prev
      );
      await showSuccess("예매가 취소되었습니다.");
    } catch (e) {
      showError(e instanceof Error ? e.message : "취소 처리 중 오류가 발생했습니다.");
    } finally {
      setCancelTargetId(null);
      setIsProcessing(false);
    }
  };

  const startEditing = () => {
    if (!data) return;
    setEditName(data.name);
    setEditEmail(data.email);
    setEditPassword("");
    setEditPasswordCheck("");
    setIsEditing(true);
  };

  const cancelEditing = () => {
    setIsEditing(false);
    setEditPassword("");
    setEditPasswordCheck("");
  };

  const handleSaveProfile = async () => {
    if (editName.trim() === "") {
      showAlert("이름을 입력해주세요.");
      return;
    }
    if (editName.includes(" ")) {
      showAlert("이름에 공백을 포함할 수 없습니다.");
      return;
    }
    if (editEmail.trim() === "") {
      showAlert("이메일을 입력해주세요.");
      return;
    }
    if (editPassword !== "") {
      if (editPassword.length < 8) {
        showAlert("비밀번호는 8자 이상이어야 합니다.");
        return;
      }
      if (editPassword !== editPasswordCheck) {
        showAlert("새 비밀번호가 일치하지 않습니다.");
        return;
      }
    }

    setIsSavingProfile(true);
    try {
      const body: Record<string, string> = {
        name: editName,
        email: editEmail,
      };
      if (editPassword !== "") {
        body.password = editPassword;
      }

      await apiFetch("/users/me", {
        method: "PATCH",
        body: JSON.stringify(body),
      });

      setData((prev) => (prev ? { ...prev, name: editName, email: editEmail } : prev));
      setIsEditing(false);
      setEditPassword("");
      setEditPasswordCheck("");
      showSuccess("정보가 수정되었습니다.");
    } catch (e) {
      showError(e instanceof Error ? e.message : "정보 수정 중 오류가 발생했습니다.");
    } finally {
      setIsSavingProfile(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <p className="text-gray-400">불러오는 중...</p>
      </div>
    );
  }

  if (!data) return null;

  const isSocialLogin = data.loginType !== "NORMAL";

  const sortedTickets = [...data.ticketList].sort((a, b) => b.ticketId - a.ticketId);
  const filteredTickets = sortedTickets.filter((t) => {
    if (statusFilter === "valid") return t.isValid;
    if (statusFilter === "canceled") return !t.isValid;
    return true;
  });
  const totalPages = Math.ceil(filteredTickets.length / ticketsPerPage);
  const pagedTickets = filteredTickets.slice(
    (currentPage - 1) * ticketsPerPage,
    currentPage * ticketsPerPage
  );

  const handleFilterChange = (filter: StatusFilter) => {
    setStatusFilter(filter);
    setCurrentPage(1);
  };

  return (
    <div className="min-h-screen bg-gray-50 p-10">
      <div className="max-w-3xl mx-auto">
        <div className="flex justify-between items-center mb-8">
          <div>
            <p className="text-gray-400 text-sm">안녕하세요</p>
            <h1 className="text-2xl font-bold text-gray-800">{data.name}님 👋</h1>
          </div>
          <button
            onClick={() => setShowWithdrawModal(true)}
            className="px-4 py-2 bg-red-500 hover:bg-red-600 text-white text-sm font-semibold rounded-lg transition"
          >
            회원탈퇴
          </button>
        </div>

        <div className="bg-white rounded-2xl shadow-sm p-8 mb-8">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-bold text-gray-700">내 정보</h2>
            {!isEditing && (
              <button
                onClick={startEditing}
                className="text-xs text-blue-600 hover:text-blue-700 border border-blue-200 hover:border-blue-300 px-3 py-1 rounded-lg transition"
              >
                정보 수정
              </button>
            )}
          </div>

          {isEditing ? (
            <div className="space-y-3">
              <div>
                <label className="block text-xs text-gray-400 mb-1">이름</label>
                <input
                  type="text"
                  value={editName}
                  onChange={(e) => setEditName(e.target.value)}
                  className="w-full p-2.5 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400"
                />
              </div>
              <div>
                <label className="block text-xs text-gray-400 mb-1">이메일</label>
                <input
                  type="email"
                  value={editEmail}
                  onChange={(e) => setEditEmail(e.target.value)}
                  className="w-full p-2.5 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400"
                />
              </div>
              {!isSocialLogin && (
                <>
                  <div>
                    <label className="block text-xs text-gray-400 mb-1">
                      새 비밀번호 (변경 시에만 입력)
                    </label>
                    <input
                      type="password"
                      value={editPassword}
                      onChange={(e) => setEditPassword(e.target.value)}
                      placeholder="8자 이상"
                      className="w-full p-2.5 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400"
                    />
                    <PasswordStrengthMeter password={editPassword} />
                  </div>
                  {editPassword !== "" && (
                    <div>
                      <label className="block text-xs text-gray-400 mb-1">새 비밀번호 확인</label>
                      <input
                        type="password"
                        value={editPasswordCheck}
                        onChange={(e) => setEditPasswordCheck(e.target.value)}
                        className="w-full p-2.5 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400"
                      />
                    </div>
                  )}
                </>
              )}
              <div className="flex gap-2 pt-2">
                <button
                  onClick={cancelEditing}
                  disabled={isSavingProfile}
                  className="flex-1 p-2.5 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-lg font-semibold text-sm transition disabled:opacity-50"
                >
                  취소
                </button>
                <button
                  onClick={handleSaveProfile}
                  disabled={isSavingProfile}
                  className="flex-1 p-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-semibold text-sm transition disabled:opacity-50"
                >
                  {isSavingProfile ? "저장 중..." : "저장"}
                </button>
              </div>
            </div>
          ) : (
            <div className="space-y-2 text-gray-600">
              <p><span className="inline-block w-20 text-gray-400">이름</span>{data.name}</p>
              <p><span className="inline-block w-20 text-gray-400">아이디</span>{data.id}</p>
              <p><span className="inline-block w-20 text-gray-400">이메일</span>{data.email}</p>
            </div>
          )}
        </div>

        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-bold text-gray-700">내 티켓</h2>
          <span className="text-sm text-gray-400">{filteredTickets.length}개의 티켓</span>
        </div>

        <div className="flex gap-2 mb-4">
          {(
            [
              { key: "all", label: "전체" },
              { key: "valid", label: "예매완료" },
              { key: "canceled", label: "취소됨" },
            ] as const
          ).map((f) => (
            <button
              key={f.key}
              onClick={() => handleFilterChange(f.key)}
              className={`px-3 py-1.5 rounded-lg text-sm font-semibold border transition ${
                statusFilter === f.key
                  ? "bg-blue-600 text-white border-blue-600"
                  : "bg-white text-gray-600 border-gray-200 hover:border-blue-400"
              }`}
            >
              {f.label}
            </button>
          ))}
        </div>

        {filteredTickets.length === 0 ? (
          <p className="text-sm text-gray-400 text-center py-10">해당 조건의 티켓이 없습니다.</p>
        ) : (
          <div className="space-y-6">
            {pagedTickets.map((ticket) => (
              <div key={ticket.ticketId} className="flex shadow-md rounded-2xl overflow-hidden">
                <div className="flex-shrink-0 w-36 bg-gradient-to-br from-blue-200 to-indigo-300 flex items-center justify-center text-white font-bold text-sm overflow-hidden">
                  {ticket.urlPoster ? (
                    <img
                      src={getLocalConcertPoster(ticket.urlPoster)}
                      alt={ticket.concertName}
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    "포스터"
                  )}
                </div>

                <div className="border-l-2 border-dashed border-gray-200 my-4" />

                <div className="flex-1 bg-white p-6">
                  <div className="flex justify-between items-start mb-3">
                    <h3 className="font-bold text-gray-800 text-lg">{ticket.concertName}</h3>
                    <div className="flex items-center gap-2">
                      {ticket.isValid && (
                        <button
                          onClick={() => setCancelTargetId(ticket.ticketId)}
                          className="text-xs text-gray-400 hover:text-red-500 border border-gray-200 hover:border-red-300 px-3 py-1 rounded-lg transition"
                        >
                          예매 취소
                        </button>
                      )}
                      <span className={`px-2 py-1 text-xs rounded-full font-semibold ${
                        !ticket.isValid ? "bg-gray-100 text-gray-400" : "bg-green-100 text-green-700"
                      }`}>
                        {!ticket.isValid ? "취소됨" : "예매완료"}
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
                      <span className="inline-block w-20 text-gray-400">결제금액</span>
                      <span className="text-blue-600 font-bold">{ticket.ticketPrice.toLocaleString()}원</span>
                    </p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

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
                className={`w-10 h-10 rounded-lg border text-sm font-semibold ${
                  currentPage === page
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

      {cancelTargetId !== null && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl p-8 max-w-sm w-full">
            <h2 className="text-xl font-bold text-center text-gray-800 mb-3">예매를 취소하시겠어요?</h2>
            <p className="text-center text-gray-500 text-sm mb-6">취소 후에는 되돌릴 수 없습니다.</p>
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
      )}

      {showWithdrawModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl p-8 max-w-sm w-full">
            <h2 className="text-xl font-bold text-center text-gray-800 mb-3">정말 탈퇴하시겠어요?</h2>
            <p className="text-center text-gray-500 text-sm mb-6">
              탈퇴 시 모든 예매 내역이 사라지며,<br />되돌릴 수 없습니다.
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

      {isProcessing && (
        <div className="fixed inset-0 z-50 bg-black/30 backdrop-blur-[2px] flex items-center justify-center">
          <div className="bg-white rounded-2xl p-8 shadow-2xl flex flex-col items-center gap-4 max-w-xs w-full border border-gray-100">
            <Loader2 className="h-10 w-10 text-blue-600 animate-spin" />
            <div className="text-center">
              <h3 className="font-bold text-gray-800 text-lg">예매 취소 처리 중</h3>
              <p className="text-xs text-gray-400 mt-1">안전하게 예매 취소를 완료하고 있습니다.</p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}