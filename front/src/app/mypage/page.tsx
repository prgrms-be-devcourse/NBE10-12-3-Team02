"use client";

import { useState, useEffect, useRef } from "react";
import Image from "next/image";
import { useRouter } from "next/navigation";
import {
  apiFetch,
  decodeToken,
  restoreSession,
  setAccessToken,
  getAccessToken,
  BASE_URL,
} from "@/lib/api";
import { Camera } from "lucide-react";
import { showAlert, showConfirm, showSuccess, showError } from "@/lib/alert";
import { getLocalConcertPoster } from "@/lib/concertDetailImages";
import PasswordStrengthMeter from "@/app/components/PasswordStrengthMeter";

interface TicketSummary {
  ticketId: number;
  ticketNumber: string;
  groupToken?: string;
  seatNumber: string;
  gradeName: string;
  ticketPrice: number;
  isValid: boolean;
  createdAt: string;
}

interface LegacyTicketSummary extends TicketSummary {
  valid?: boolean;
}

interface TicketGroupInfo {
  scheduleId: number;
  concertName: string;
  urlPoster: string;
  startDate: string;
  endDate: string;
  round: number;
  totalPrice: number;
  tickets: TicketSummary[];
}

interface MyPageData {
  name: string;
  id: string;
  email: string;
  loginType: string;
  profileImageUrl: string;
  ticketGroups: TicketGroupInfo[];
}

type StatusFilter = "all" | "valid" | "canceled";

function SocialBadge({ provider }: { provider: string }) {
  if (provider === "KAKAO") return (
    <svg width="18" height="18" viewBox="0 0 18 18" aria-label="카카오">
      <rect width="18" height="18" rx="4" fill="#FEE500"/>
      <path fill="#3B1E1E" d="M9 3.5C5.4 3.5 2.5 5.8 2.5 8.7c0 1.9 1.1 3.5 2.8 4.5L4.5 15.5l3.2-1.5c.43.06.86.1 1.3.1 3.6 0 6.5-2.3 6.5-5.3S12.6 3.5 9 3.5z"/>
    </svg>
  );
  if (provider === "NAVER") return (
    <svg width="18" height="18" viewBox="0 0 18 18" aria-label="네이버">
      <rect width="18" height="18" rx="4" fill="#03C75A"/>
      <path fill="white" d="M4 4h2.5v6.5l5-6.5H14v10h-2.5V7.5l-5 6.5H4z"/>
    </svg>
  );
  if (provider === "GOOGLE") return (
    <svg width="18" height="18" viewBox="0 0 18 18" aria-label="구글">
      <rect width="18" height="18" rx="4" fill="white" stroke="#ddd" strokeWidth="1"/>
      <path fill="#EA4335" d="M9 5.63c.81 0 1.54.28 2.12.82l1.59-1.59A5.5 5.5 0 0 0 9 3.5a5.5 5.5 0 0 0-4.93 3l1.86 1.44C6.36 6.1 7.57 5.63 9 5.63z"/>
      <path fill="#FBBC05" d="M5.93 10.5a3.3 3.3 0 0 1 0-2.1L4.07 7.04A5.5 5.5 0 0 0 3.5 9.5c0 .88.2 1.72.57 2.47l1.86-1.47z"/>
      <path fill="#34A853" d="M9 15a5.5 5.5 0 0 0 3.69-1.36l-1.83-1.42c-.5.34-1.15.54-1.86.54-1.43 0-2.64-.97-3.07-2.27H4.07v1.46C4.97 13.83 6.87 15 9 15z"/>
      <path fill="#4285F4" d="M14.5 9.18c0-.46-.04-.89-.11-1.31H9v2.48h3.04c-.14.7-.54 1.3-1.13 1.69v1.4h1.83c1.07-.98 1.76-2.42 1.76-4.26z"/>
      <rect fill="#4285F4" x="9" y="8.7" width="5" height="1.4" rx=".7"/>
    </svg>
  );
  return null;
}

export default function MyPage() {
  const router = useRouter();
  const hasCheckedAuth = useRef(false);

  const [data, setData] = useState<MyPageData | null>(null);
  const [loading, setLoading] = useState(true);
  const [showWithdrawModal, setShowWithdrawModal] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("all");
  const [cancelingKey, setCancelingKey] = useState<number | null>(null);
  const ticketsPerPage = 5;

  const [isEditing, setIsEditing] = useState(false);
  const [editName, setEditName] = useState("");
  const [editEmail, setEditEmail] = useState("");
  const [editPassword, setEditPassword] = useState("");
  const [editPasswordCheck, setEditPasswordCheck] = useState("");
  const [isSavingProfile, setIsSavingProfile] = useState(false);

  const fileInputRef = useRef<HTMLInputElement>(null);
  const [profilePreviewUrl, setProfilePreviewUrl] = useState<string | null>(null);
  const [selectedProfileFile, setSelectedProfileFile] = useState<File | null>(null);
  const [isUploadingProfile, setIsUploadingProfile] = useState(false);
  const [profileCacheKey, setProfileCacheKey] = useState(() => Date.now());
  const [profileImgError, setProfileImgError] = useState(false);

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
        showError(
          e instanceof Error ? e.message : "마이페이지 조회에 실패했습니다.",
        );
      } finally {
        setLoading(false);
      }
    };

    initializeMyPage();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleWithdraw = async () => {
    try {
      await apiFetch(`/users/withdraw`, { method: "PATCH" });
      setAccessToken(null);
      await showSuccess("회원 탈퇴가 완료되었습니다.");
      window.location.href = "/";
    } catch (e) {
      showError(
        e instanceof Error ? e.message : "탈퇴 처리 중 오류가 발생했습니다.",
      );
    } finally {
      setShowWithdrawModal(false);
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

      setData((prev) =>
        prev ? { ...prev, name: editName, email: editEmail } : prev,
      );
      setIsEditing(false);
      setEditPassword("");
      setEditPasswordCheck("");
      showSuccess("정보가 수정되었습니다.");
    } catch (e) {
      showError(
        e instanceof Error ? e.message : "정보 수정 중 오류가 발생했습니다.",
      );
    } finally {
      setIsSavingProfile(false);
    }
  };

  const handleProfileFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    e.target.value = "";
    const ALLOWED = ["image/jpeg", "image/png", "image/webp"];
    if (!ALLOWED.includes(file.type)) {
      showAlert("jpg, jpeg, png, webp 파일만 업로드할 수 있습니다.");
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      showAlert("파일 크기는 5MB 이하여야 합니다.");
      return;
    }
    setSelectedProfileFile(file);
    setProfilePreviewUrl(URL.createObjectURL(file));
  };

  const handleUploadProfileImage = async () => {
    if (!selectedProfileFile) return;
    const formData = new FormData();
    formData.append("file", selectedProfileFile);
    setIsUploadingProfile(true);
    try {
      const token = getAccessToken();
      const res = await fetch(`${BASE_URL}/api/v1/users/me/profile-image`, {
        method: "POST",
        credentials: "include",
        headers: token ? { Authorization: `Bearer ${token}` } : {},
        body: formData,
      });
      if (!res.ok) {
        const json = await res.json().catch(() => ({}));
        throw new Error((json as { msg?: string }).msg || "업로드에 실패했습니다.");
      }
      const newToken = res.headers.get("Authorization");
      if (newToken?.startsWith("Bearer ")) setAccessToken(newToken.slice(7));
      if (profilePreviewUrl) URL.revokeObjectURL(profilePreviewUrl);
      setProfilePreviewUrl(null);
      setSelectedProfileFile(null);
      setProfileImgError(false);
      setProfileCacheKey(Date.now());
      showSuccess("프로필 사진이 변경되었습니다.");
    } catch (e) {
      showError(e instanceof Error ? e.message : "업로드에 실패했습니다.");
    } finally {
      setIsUploadingProfile(false);
    }
  };

  const handleDeleteProfileImage = async () => {
    const confirmed = await showConfirm(
      "프로필 사진을 기본 이미지로 변경하시겠어요?",
      { title: "기본 이미지로 변경", confirmText: "변경", cancelText: "취소" },
    );
    if (!confirmed) return;
    try {
      await apiFetch("/users/me/profile-image", { method: "DELETE" });
      if (profilePreviewUrl) URL.revokeObjectURL(profilePreviewUrl);
      setProfilePreviewUrl(null);
      setSelectedProfileFile(null);
      setProfileImgError(true);
      showSuccess("기본 이미지로 변경되었습니다.");
    } catch (e) {
      showError(e instanceof Error ? e.message : "삭제에 실패했습니다.");
    }
  };

  const cancelProfileEdit = () => {
    if (profilePreviewUrl) URL.revokeObjectURL(profilePreviewUrl);
    setProfilePreviewUrl(null);
    setSelectedProfileFile(null);
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

  // 백엔드는 "같은 회차(scheduleId)"를 기준으로만 묶어주는데, 같은 회차를 여러 번 따로 결제한
  // 경우(예: 취소한 옛날 예매 + 새로 산 예매)까지 하나로 합쳐버릴 수 있다.
  // 그래서 그룹 안에서 다시 한번, "한 번의 결제로 생성된 티켓들은 ticketId가 바로 이어진다"는
  // 규칙으로 진짜 예매 단위로 쪼갠다.
  // 결제 단위(groupToken)별로 개별 예매 그룹으로 쪼갠다.
  const splitIntoReservations = (group: TicketGroupInfo): TicketGroupInfo[] => {
    const map = new Map<string, TicketSummary[]>();
    for (const ticket of group.tickets) {
      const key = ticket.groupToken || `ticket-${ticket.ticketId}`;
      if (!map.has(key)) map.set(key, []);
      map.get(key)!.push(ticket);
    }
    return Array.from(map.values()).map((tickets) => ({
      ...group,
      tickets,
      totalPrice: tickets.reduce((sum, t) => sum + t.ticketPrice, 0),
    }));
  };
  const reservations = data.ticketGroups.flatMap(splitIntoReservations);

  // 최근 예매가 먼저 보이도록, 그룹 안에서 가장 큰 ticketId를 기준으로 정렬한다.
  const sortedGroups = [...reservations].sort((a, b) => {
    const maxA = Math.max(...a.tickets.map((t) => t.ticketId));
    const maxB = Math.max(...b.tickets.map((t) => t.ticketId));
    return maxB - maxA;
  });

  const isTicketValid = (t: TicketSummary): boolean => {
    const legacyTicket = t as LegacyTicketSummary;
    if (t.isValid !== undefined) return t.isValid;
    if (legacyTicket.valid !== undefined) return legacyTicket.valid;
    return true;
  };

  const ticketGroups = sortedGroups.filter((group) => {
    const allInvalid = group.tickets.every((t) => !isTicketValid(t));
    if (statusFilter === "valid") return !allInvalid;
    if (statusFilter === "canceled") return allInvalid;
    return true;
  });

  const totalPages = Math.ceil(ticketGroups.length / ticketsPerPage);
  const pagedGroups = ticketGroups.slice(
    (currentPage - 1) * ticketsPerPage,
    currentPage * ticketsPerPage,
  );

  const handleFilterChange = (filter: StatusFilter) => {
    setStatusFilter(filter);
    setCurrentPage(1);
  };

  const goToTicketDetail = (group: TicketGroupInfo) => {
    const encoded = encodeURIComponent(JSON.stringify(group));
    router.push(`/mypage/tickets?group=${encoded}`);
  };

  const handleCancelGroup = async (group: TicketGroupInfo) => {
    const validTickets = group.tickets.filter((t) => t.isValid);
    const confirmed = await showConfirm(
      validTickets.length > 1
        ? `좌석 ${validTickets.length}매를 모두 취소하시겠어요?`
        : "예매를 취소하시겠어요?",
      {
        title: "예매 취소",
        confirmText: "취소하기",
        cancelText: "돌아가기",
        danger: true,
      },
    );
    if (!confirmed) return;

    setCancelingKey(group.tickets[0].ticketId);
    try {
      await Promise.all(
        validTickets.map((t) =>
          apiFetch(`/tickets/cancel/${t.ticketId}`, { method: "PATCH" }),
        ),
      );
      const canceledIds = new Set(validTickets.map((t) => t.ticketId));
      setData((prev) =>
        prev
          ? {
              ...prev,
              ticketGroups: prev.ticketGroups.map((g) =>
                g.scheduleId === group.scheduleId
                  ? {
                      ...g,
                      tickets: g.tickets.map((t) =>
                        canceledIds.has(t.ticketId)
                          ? { ...t, isValid: false }
                          : t,
                      ),
                    }
                  : g,
              ),
            }
          : prev,
      );
      await showSuccess("예매가 취소되었습니다.");
    } catch (e) {
      showError(
        e instanceof Error ? e.message : "취소 처리 중 오류가 발생했습니다.",
      );
    } finally {
      setCancelingKey(null);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 p-10">
      <div className="max-w-3xl mx-auto">
        <div className="flex justify-between items-center mb-8">
          <div>
            <p className="text-gray-400 text-sm">안녕하세요</p>
            <h1 className="text-2xl font-bold text-gray-800">
              {data.name}님 👋
            </h1>
          </div>
          <button
            onClick={() => setShowWithdrawModal(true)}
            className="px-4 py-2 bg-red-500 hover:bg-red-600 text-white text-sm font-semibold rounded-lg transition"
          >
            회원탈퇴
          </button>
        </div>

        <div className="bg-white rounded-2xl shadow-sm p-8 mb-8">
          <div className="flex flex-col items-center mb-6">
            <div className="relative w-20 h-20 rounded-full overflow-hidden border-2 border-gray-100 mb-2">
              <Image
                src={
                  profilePreviewUrl ||
                  (!data.profileImageUrl || profileImgError
                    ? "/default-avatar.svg"
                    : `${data.profileImageUrl}?t=${profileCacheKey}`)
                }
                alt="프로필 사진"
                fill
                unoptimized
                onError={() => setProfileImgError(true)}
                className="object-cover"
              />
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                className="absolute inset-0 bg-black/30 flex items-center justify-center opacity-0 hover:opacity-100 transition"
                aria-label="프로필 사진 변경"
              >
                <Camera size={18} className="text-white" />
              </button>
            </div>
            <input
              ref={fileInputRef}
              type="file"
              accept=".jpg,.jpeg,.png,.webp"
              className="hidden"
              onChange={handleProfileFileSelect}
            />
            {selectedProfileFile ? (
              <div className="flex gap-2 mt-1">
                <button
                  type="button"
                  onClick={cancelProfileEdit}
                  className="text-xs text-gray-500 hover:text-gray-700 border border-gray-200 px-3 py-1 rounded-lg transition"
                >
                  취소
                </button>
                <button
                  type="button"
                  onClick={handleUploadProfileImage}
                  disabled={isUploadingProfile}
                  className="text-xs bg-blue-600 hover:bg-blue-700 text-white px-3 py-1 rounded-lg transition disabled:opacity-50"
                >
                  {isUploadingProfile ? "저장 중..." : "저장"}
                </button>
              </div>
            ) : (
              <button
                type="button"
                onClick={handleDeleteProfileImage}
                className="text-xs text-gray-400 hover:text-gray-600 mt-1 transition"
              >
                기본 이미지로 변경
              </button>
            )}
          </div>

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
                <label className="block text-xs text-gray-400 mb-1">
                  이메일
                </label>
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
                      <label className="block text-xs text-gray-400 mb-1">
                        새 비밀번호 확인
                      </label>
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
              <p>
                <span className="inline-block w-20 text-gray-400">이름</span>
                {data.name}
              </p>
              <p className="flex items-center gap-1.5">
                <span className="inline-block w-20 shrink-0 text-gray-400">아이디</span>
                <span>{data.id}</span>
                {isSocialLogin && <SocialBadge provider={data.loginType} />}
              </p>
              <p>
                <span className="inline-block w-20 text-gray-400">이메일</span>
                {data.email}
              </p>
            </div>
          )}
        </div>

        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-bold text-gray-700">내 티켓</h2>
          <span className="text-sm text-gray-400">
            {ticketGroups.reduce((sum, g) => sum + g.tickets.length, 0)}개의
            티켓
          </span>
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

        {ticketGroups.length === 0 ? (
          <p className="text-sm text-gray-400 text-center py-10">
            해당 조건의 티켓이 없습니다.
          </p>
        ) : (
          <div className="space-y-6">
            {pagedGroups.map((group) => {
              // "일부 취소"는 두지 않는다 — 한 장이라도 유효하면 "예매완료", 전부 취소됐을 때만 "취소됨".
              const allInvalid = group.tickets.every((t) => !isTicketValid(t));
              const statusLabel = allInvalid ? "취소됨" : "예매완료";
              const statusClass = allInvalid
                ? "bg-gray-100 text-gray-400"
                : "bg-green-100 text-green-700";

              return (
                <div
                  key={group.tickets[0].ticketId}
                  onClick={() => goToTicketDetail(group)}
                  role="button"
                  tabIndex={0}
                  className="w-full flex shadow-md rounded-2xl overflow-hidden text-left hover:shadow-lg transition cursor-pointer"
                >
                  <div className="flex-shrink-0 w-36 relative aspect-[3/4] bg-gradient-to-br from-blue-200 to-indigo-300 flex items-center justify-center text-white font-bold text-sm overflow-hidden">
                    {group.urlPoster ? (
                      <Image
                        fill
                        unoptimized
                        src={getLocalConcertPoster(group.urlPoster)}
                        alt={group.concertName}
                        sizes="144px"
                        className="object-cover"
                      />
                    ) : (
                      "포스터"
                    )}
                  </div>

                  <div className="border-l-2 border-dashed border-gray-200 my-4" />

                  <div className="flex-1 bg-white p-6">
                    <div className="flex justify-between items-start mb-3">
                      <h3 className="font-bold text-gray-800 text-lg">
                        {group.concertName}
                      </h3>
                      <div className="flex items-center gap-2">
                        {!allInvalid && (
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              handleCancelGroup(group);
                            }}
                            disabled={
                              cancelingKey === group.tickets[0].ticketId
                            }
                            className="text-xs text-gray-400 hover:text-red-500 border border-gray-200 hover:border-red-300 px-3 py-1 rounded-lg transition disabled:opacity-50"
                          >
                            {cancelingKey === group.tickets[0].ticketId
                              ? "취소 중..."
                              : "예매 취소"}
                          </button>
                        )}
                        <span
                          className={`px-2 py-1 text-xs rounded-full font-semibold ${statusClass}`}
                        >
                          {statusLabel}
                        </span>
                      </div>
                    </div>

                    <div className="space-y-1 text-sm text-gray-500">
                      <p>
                        <span className="inline-block w-20 text-gray-400">
                          좌석
                        </span>
                        {group.tickets.length}매 (
                        {group.tickets.map((t) => t.seatNumber).join(", ")})
                      </p>
                      <p>
                        <span className="inline-block w-20 text-gray-400">
                          공연기간
                        </span>
                        {group.startDate} ~ {group.endDate}
                      </p>
                      <p>
                        <span className="inline-block w-20 text-gray-400">
                          결제금액
                        </span>
                        <span className="text-blue-600 font-bold">
                          {group.totalPrice.toLocaleString()}원
                        </span>
                      </p>
                    </div>
                  </div>
                </div>
              );
            })}
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

      {showWithdrawModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl p-8 max-w-sm w-full">
            <h2 className="text-xl font-bold text-center text-gray-800 mb-3">
              정말 탈퇴하시겠어요?
            </h2>
            <p className="text-center text-gray-500 text-sm mb-6">
              탈퇴 시 모든 예매 내역이 사라지며,
              <br />
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
