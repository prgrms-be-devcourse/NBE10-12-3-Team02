"use client";

import { useState, useEffect, useRef, Suspense } from "react";
import Image from "next/image";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import {
  apiFetch,
  decodeToken,
  restoreSession,
  setAccessToken,
  getAccessToken,
  BASE_URL,
} from "@/lib/api";
import { Camera, Star } from "lucide-react";
import { showAlert, showConfirm, showSuccess, showError } from "@/lib/alert";
import { getLocalConcertPoster } from "@/lib/concertDetailImages";
import { formatDateTime } from "@/lib/date";
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

interface MyPostSummary {
  postId: number;
  title: string;
  rating: number | null;
  reviewType: "EXPECTATION" | "REVIEW";
  summary: string | null;
  likeCount: number;
  createdAt: string;
  concertName: string;
}

interface PostBookmarkSummary {
  postId: number;
  concertId: number;
  concertName: string;
  userName: string;
  title: string;
  content: string;
  posterUrl: string | null;
  bookmarkedAt: string | null;
}

interface PostLikeSummary {
  postId: number;
  concertId: number;
  concertName: string;
  userName: string;
  title: string;
  content: string;
  posterUrl: string | null;
  likedAt: string | null;
}

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  last: boolean;
}

const REVIEW_TYPE_BADGE: Record<
  MyPostSummary["reviewType"],
  { label: string; className: string }
> = {
  REVIEW: {
    label: "관람후기",
    className: "bg-emerald-50 text-emerald-600 border border-emerald-200",
  },
  EXPECTATION: {
    label: "기대평",
    className: "bg-amber-50 text-amber-600 border border-amber-200",
  },
};

const SOCIAL_LINK_ERROR_MESSAGES: Record<string, string> = {
  oauth2_link_request_expired: "연동 요청이 만료되었습니다. 다시 시도해주세요.",
  oauth2_provider_mismatch: "소셜 제공자 불일치 오류가 발생했습니다.",
  oauth2_user_not_found: "계정 정보를 찾을 수 없습니다.",
  oauth2_account_already_linked: "이미 소셜 계정이 연동되어 있습니다.",
  oauth2_account_already_used: "이미 다른 계정에 연결된 소셜 계정입니다.",
  oauth2_email_missing: "소셜 계정에서 이메일 정보를 가져오지 못했습니다.",
  oauth2_email_not_verified: "소셜 계정의 이메일이 인증되지 않았습니다.",
  oauth2_email_mismatch: "소셜 계정의 이메일이 현재 계정 이메일과 다릅니다.",
};

const SOCIAL_LINK_PROVIDERS = [
  {
    key: "KAKAO",
    label: "카카오 계정 연동",
    icon: "/icons/kakao.png",
    className: "bg-[#FEE500] hover:bg-[#fada0a] text-[#191919]",
  },
  {
    key: "NAVER",
    label: "네이버 계정 연동",
    icon: "/icons/naver.png",
    className: "bg-[#03C75A] hover:bg-[#02b350] text-white",
  },
  {
    key: "GOOGLE",
    label: "Google 계정 연동",
    icon: "/icons/google.png",
    className: "bg-white hover:bg-gray-50 text-gray-700 border border-gray-200",
  },
] as const;

const PROVIDER_LABELS: Record<string, string> = {
  KAKAO: "카카오",
  NAVER: "네이버",
  GOOGLE: "Google",
};

function RatingStars({ rating }: { rating: number }) {
  return (
    <div className="flex items-center gap-0.5">
      {[1, 2, 3, 4, 5].map((n) => (
        <Star
          key={n}
          size={13}
          className={
            n <= rating ? "fill-yellow-400 text-yellow-400" : "text-gray-200"
          }
        />
      ))}
    </div>
  );
}

type StatusFilter = "all" | "valid" | "canceled";
const MY_POSTS_PAGE_SIZE = 5;
const MY_BOOKMARKS_PAGE_SIZE = 5;
const MY_LIKES_PAGE_SIZE = 5;

function SocialBadge({
  provider,
  size = 18,
}: {
  provider: string;
  size?: number;
}) {
  const iconMap: Record<string, string> = {
    KAKAO: "/icons/kakao.png",
    NAVER: "/icons/naver.png",
    GOOGLE: "/icons/google.png",
  };
  const src = iconMap[provider];
  if (!src) return null;
  return (
    <Image src={src} alt={provider} width={size} height={size} unoptimized />
  );
}

function MyPageContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const hasCheckedAuth = useRef(false);
  const hasSocialLinkHandledRef = useRef(false);

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
  const [profilePreviewUrl, setProfilePreviewUrl] = useState<string | null>(
    null,
  );
  const [selectedProfileFile, setSelectedProfileFile] = useState<File | null>(
    null,
  );
  const [isUploadingProfile, setIsUploadingProfile] = useState(false);
  const [profileCacheKey, setProfileCacheKey] = useState(() => Date.now());
  const [profileImgError, setProfileImgError] = useState(false);

  const [myPosts, setMyPosts] = useState<MyPostSummary[]>([]);
  const [myPostsLoading, setMyPostsLoading] = useState(true);
  const [myPostsPage, setMyPostsPage] = useState(0);
  const [myPostsTotalPages, setMyPostsTotalPages] = useState(0);
  const [myPostsTotalElements, setMyPostsTotalElements] = useState(0);

  const fetchMyPosts = async (page: number) => {
    setMyPostsLoading(true);
    try {
      const res = await apiFetch<PageResponse<MyPostSummary>>(
        `/posts/me?page=${page}&size=${MY_POSTS_PAGE_SIZE}`,
      );
      setMyPosts(res.data.content);
      setMyPostsPage(res.data.number);
      setMyPostsTotalPages(res.data.totalPages);
      setMyPostsTotalElements(res.data.totalElements);
    } catch {
      setMyPosts([]);
      setMyPostsTotalPages(0);
      setMyPostsTotalElements(0);
    } finally {
      setMyPostsLoading(false);
    }
  };

  const [bookmarks, setBookmarks] = useState<PostBookmarkSummary[]>([]);
  const [bookmarksLoading, setBookmarksLoading] = useState(true);
  const [bookmarksPage, setBookmarksPage] = useState(0);
  const [bookmarksTotalPages, setBookmarksTotalPages] = useState(0);
  const [bookmarksTotalElements, setBookmarksTotalElements] = useState(0);

  const fetchBookmarks = async (page: number) => {
    setBookmarksLoading(true);
    try {
      const res = await apiFetch<PageResponse<PostBookmarkSummary>>(
        `/users/me/post-bookmarks?page=${page}&size=${MY_BOOKMARKS_PAGE_SIZE}`,
      );
      setBookmarks(res.data.content);
      setBookmarksPage(res.data.number);
      setBookmarksTotalPages(res.data.totalPages);
      setBookmarksTotalElements(res.data.totalElements);
    } catch {
      setBookmarks([]);
      setBookmarksTotalPages(0);
      setBookmarksTotalElements(0);
    } finally {
      setBookmarksLoading(false);
    }
  };

  const [likes, setLikes] = useState<PostLikeSummary[]>([]);
  const [likesLoading, setLikesLoading] = useState(true);
  const [likesPage, setLikesPage] = useState(0);
  const [likesTotalPages, setLikesTotalPages] = useState(0);
  const [likesTotalElements, setLikesTotalElements] = useState(0);

  const fetchLikes = async (page: number) => {
    setLikesLoading(true);
    try {
      const res = await apiFetch<PageResponse<PostLikeSummary>>(
        `/users/me/post-likes?page=${page}&size=${MY_LIKES_PAGE_SIZE}`,
      );
      setLikes(res.data.content);
      setLikesPage(res.data.number);
      setLikesTotalPages(res.data.totalPages);
      setLikesTotalElements(res.data.totalElements);
    } catch {
      setLikes([]);
      setLikesTotalPages(0);
      setLikesTotalElements(0);
    } finally {
      setLikesLoading(false);
    }
  };

  interface SocialLinkStatus {
    linked: boolean;
    provider: string | null;
  }
  const [socialLinkStatus, setSocialLinkStatus] =
    useState<SocialLinkStatus | null>(null);
  const [socialLinkLoading, setSocialLinkLoading] = useState(true);
  const [socialLinkStarting, setSocialLinkStarting] = useState<string | null>(
    null,
  );

  const [activeTab, setActiveTab] = useState<"info" | "tickets" | "posts">(
    () => {
      const t = searchParams.get("tab");
      return t === "tickets" || t === "posts" ? t : "info";
    },
  );

  useEffect(() => {
    const t = searchParams.get("tab");
    if ((t === "tickets" || t === "posts" || t === "info") && t !== activeTab) {
      // URL 쿼리 파라미터(외부 시스템)와 activeTab을 동기화하는 로직이라
      // effect 안에서 setState를 쓰는 게 맞는 경우다.
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setActiveTab(t);
    }
  }, [searchParams, activeTab]);

  const [postsSubTab, setPostsSubTab] = useState<"my" | "bookmarks" | "likes">(
    "my",
  );

  const fetchSocialLinkStatus = async () => {
    setSocialLinkLoading(true);
    try {
      const res = await apiFetch<SocialLinkStatus>("/users/me/social-links");
      setSocialLinkStatus(res.data);
    } catch {
      setSocialLinkStatus(null);
    } finally {
      setSocialLinkLoading(false);
    }
  };

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
        fetchMyPosts(0);
        fetchBookmarks(0);
        fetchLikes(0);
        fetchSocialLinkStatus();
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

  useEffect(() => {
    if (hasSocialLinkHandledRef.current) return;
    hasSocialLinkHandledRef.current = true;
    const params = new URLSearchParams(window.location.search);
    const linked = params.get("socialLink");
    const linkError = params.get("socialLinkError");
    if (!linked && !linkError) return;
    if (linked === "success") {
      showSuccess("소셜 계정 연동이 완료되었습니다.").then(() => {
        fetchSocialLinkStatus();
      });
    } else if (linkError) {
      showError(
        SOCIAL_LINK_ERROR_MESSAGES[linkError] ??
          "소셜 계정 연동 중 오류가 발생했습니다.",
      );
    }
    router.replace("/mypage");
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    const handlePageShow = (e: PageTransitionEvent) => {
      if (e.persisted) {
        setSocialLinkStarting(null);
        fetchSocialLinkStatus();
      }
    };
    window.addEventListener("pageshow", handlePageShow);
    return () => window.removeEventListener("pageshow", handlePageShow);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (showWithdrawModal) {
      document.body.style.overflow = "hidden";
      return () => {
        document.body.style.overflow = "";
      };
    }
  }, [showWithdrawModal]);

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

  const handleSocialLink = async (provider: string) => {
    setSocialLinkStarting(provider);
    try {
      const res = await apiFetch<{ authorizationUrl: string }>(
        `/users/me/social-links/${provider}`,
        { method: "POST" },
      );
      // eslint-disable-next-line react-hooks/immutability
      window.location.href = `${BASE_URL}${res.data.authorizationUrl}`;
    } catch (e) {
      showError(
        e instanceof Error ? e.message : "소셜 계정 연동을 시작할 수 없습니다.",
      );
      setSocialLinkStarting(null);
    }
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
        throw new Error(
          (json as { msg?: string }).msg || "업로드에 실패했습니다.",
        );
      }
      const newToken = res.headers.get("Authorization");
      if (newToken?.startsWith("Bearer ")) setAccessToken(newToken.slice(7));
      if (profilePreviewUrl) URL.revokeObjectURL(profilePreviewUrl);
      setProfilePreviewUrl(null);
      setSelectedProfileFile(null);
      setProfileImgError(false);
      setProfileCacheKey(Date.now());
      window.dispatchEvent(new Event("profile-image-changed"));
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
      window.dispatchEvent(new Event("profile-image-changed"));
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
      <div className="max-w-4xl mx-auto">
        <div className="flex justify-between items-center mb-8">
          <div>
            <p className="text-gray-400 text-sm">안녕하세요</p>
            <h1 className="text-2xl font-bold text-gray-800">
              {data.name}님 👋
            </h1>
          </div>
        </div>

        <div className="flex gap-6 items-start">
          <nav className="w-48 shrink-0">
            <div className="bg-white rounded-2xl shadow-sm overflow-hidden">
              {(
                [
                  { key: "info", label: "내 정보" },
                  { key: "tickets", label: "내 티켓" },
                  { key: "posts", label: "내 게시글" },
                ] as const
              ).map((tab) => (
                <button
                  key={tab.key}
                  onClick={() => {
                    setActiveTab(tab.key);
                    router.replace(`/mypage?tab=${tab.key}`, { scroll: false });
                  }}
                  className={`w-full text-left px-4 py-3.5 text-sm font-semibold border-l-4 transition ${
                    activeTab === tab.key
                      ? "border-blue-600 bg-blue-50 text-blue-700"
                      : "border-transparent text-gray-600 hover:bg-gray-50"
                  }`}
                >
                  {tab.label}
                </button>
              ))}
            </div>
          </nav>

          <div className="flex-1 min-w-0">
            {activeTab === "info" && (
              <>
                <div className="bg-white rounded-2xl shadow-sm p-8 mb-6">
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
                        <label className="block text-xs text-gray-400 mb-1">
                          이름
                        </label>
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
                                onChange={(e) =>
                                  setEditPasswordCheck(e.target.value)
                                }
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
                        <span className="inline-block w-20 text-gray-400">
                          이름
                        </span>
                        {data.name}
                      </p>
                      {isSocialLogin ? (
                        <p className="flex items-center gap-1.5">
                          <span className="inline-block w-20 shrink-0 text-gray-400">
                            로그인 방식
                          </span>
                          <SocialBadge provider={data.loginType} />
                        </p>
                      ) : (
                        <p>
                          <span className="inline-block w-20 text-gray-400">
                            아이디
                          </span>
                          {data.id}
                        </p>
                      )}
                      <p>
                        <span className="inline-block w-20 text-gray-400">
                          이메일
                        </span>
                        {data.email}
                      </p>
                    </div>
                  )}
                </div>

                <div className="bg-white rounded-2xl shadow-sm p-8">
                  <h2 className="text-lg font-bold text-gray-700 mb-4">
                    소셜 계정 연동
                  </h2>
                  {socialLinkLoading ? (
                    <p className="text-sm text-gray-400">불러오는 중...</p>
                  ) : socialLinkStatus?.linked ? (
                    <div className="flex items-center gap-3 p-4 bg-gray-50 rounded-xl border border-gray-100">
                      <SocialBadge
                        provider={socialLinkStatus.provider!}
                        size={24}
                      />
                      <div>
                        <p className="text-sm font-semibold text-gray-700">
                          {PROVIDER_LABELS[socialLinkStatus.provider!] ??
                            socialLinkStatus.provider}{" "}
                          계정 연동됨
                        </p>
                        <p className="text-xs text-gray-400 mt-0.5">
                          해당 소셜 계정으로 로그인할 수 있습니다.
                        </p>
                      </div>
                    </div>
                  ) : (
                    <div>
                      <p className="text-sm text-gray-500 mb-4">
                        소셜 계정을 연동하면 해당 소셜 계정으로도 로그인할 수
                        있습니다.
                      </p>
                      <div className="flex flex-col gap-2">
                        {SOCIAL_LINK_PROVIDERS.map((p) => (
                          <button
                            key={p.key}
                            onClick={() => handleSocialLink(p.key)}
                            disabled={!!socialLinkStarting}
                            className={`flex items-center gap-3 px-4 py-3 rounded-lg font-semibold text-sm transition disabled:opacity-50 ${p.className}`}
                          >
                            <Image
                              src={p.icon}
                              alt={p.label}
                              width={20}
                              height={20}
                              unoptimized
                            />
                            {socialLinkStarting === p.key
                              ? "연결 중..."
                              : p.label}
                          </button>
                        ))}
                      </div>
                    </div>
                  )}
                </div>

                <div className="mt-4 text-right">
                  <button
                    onClick={() => setShowWithdrawModal(true)}
                    className="px-4 py-2 bg-red-500 hover:bg-red-600 text-white text-sm font-semibold rounded-lg transition"
                  >
                    회원탈퇴
                  </button>
                </div>
              </>
            )}

            {activeTab === "tickets" && (
              <div className="bg-white rounded-2xl shadow-sm p-8">
                <div className="flex items-center justify-between mb-4">
                  <h2 className="text-lg font-bold text-gray-700">내 티켓</h2>
                  <span className="text-sm text-gray-400">
                    {ticketGroups.reduce((sum, g) => sum + g.tickets.length, 0)}
                    개의 티켓
                  </span>
                </div>

                <div className="flex gap-2 mb-6">
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
                      const allInvalid = group.tickets.every(
                        (t) => !isTicketValid(t),
                      );
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
                                    className="whitespace-nowrap text-xs text-gray-400 hover:text-red-500 border border-gray-200 hover:border-red-300 px-3 py-1 rounded-lg transition disabled:opacity-50"
                                  >
                                    {cancelingKey === group.tickets[0].ticketId
                                      ? "취소 중..."
                                      : "예매 취소"}
                                  </button>
                                )}
                                <span
                                  className={`whitespace-nowrap px-2 py-1 text-xs rounded-full font-semibold ${statusClass}`}
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
                                {group.tickets
                                  .map((t) => t.seatNumber)
                                  .join(", ")}
                                )
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
                    {Array.from({ length: totalPages }, (_, i) => i + 1).map(
                      (page) => (
                        <button
                          key={page}
                          onClick={() => setCurrentPage(page)}
                          className={`w-10 h-10 rounded-lg border text-sm font-semibold ${currentPage === page ? "bg-blue-600 border-blue-600 text-white" : "bg-white border-gray-200 text-gray-600 hover:bg-gray-50"}`}
                        >
                          {page}
                        </button>
                      ),
                    )}
                    <button
                      onClick={() =>
                        setCurrentPage((p) => Math.min(totalPages, p + 1))
                      }
                      disabled={currentPage === totalPages}
                      className="px-3 py-2 rounded-lg border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-default"
                    >
                      다음
                    </button>
                  </div>
                )}
              </div>
            )}

            {activeTab === "posts" && (
              <div className="bg-white rounded-2xl shadow-sm p-8">
                <div className="flex items-center justify-between mb-6">
                  <div className="flex gap-2">
                    {(
                      [
                        { key: "my", label: "내 게시글" },
                        { key: "bookmarks", label: "북마크" },
                        { key: "likes", label: "좋아요" },
                      ] as const
                    ).map((st) => (
                      <button
                        key={st.key}
                        onClick={() => setPostsSubTab(st.key)}
                        className={`px-4 py-2 rounded-lg text-sm font-semibold border transition ${
                          postsSubTab === st.key
                            ? "bg-blue-600 text-white border-blue-600"
                            : "bg-white text-gray-600 border-gray-200 hover:border-blue-400"
                        }`}
                      >
                        {st.label}
                      </button>
                    ))}
                  </div>
                  <Link
                    href="/mypage/follows"
                    className="px-3 py-1.5 bg-white border border-gray-200 hover:border-blue-300 text-gray-600 hover:text-blue-600 text-sm font-semibold rounded-lg transition"
                  >
                    팔로우 목록 보기
                  </Link>
                </div>

                {postsSubTab === "my" && (
                  <>
                    <div className="flex items-center justify-between mb-4">
                      <h2 className="text-lg font-bold text-gray-700">
                        내 게시글
                      </h2>
                      <span className="text-sm text-gray-400">
                        {myPostsTotalElements}개
                      </span>
                    </div>
                    {myPostsLoading ? (
                      <p className="text-sm text-gray-400 text-center py-10">
                        불러오는 중...
                      </p>
                    ) : myPosts.length === 0 ? (
                      <p className="text-sm text-gray-400 text-center py-10">
                        작성한 게시글이 없습니다.
                      </p>
                    ) : (
                      <div className="space-y-3">
                        {myPosts.map((post) => (
                          <div
                            key={post.postId}
                            onClick={() => router.push(`/board/${post.postId}`)}
                            role="button"
                            tabIndex={0}
                            className="p-4 border border-gray-100 rounded-xl hover:shadow-md hover:border-blue-200 transition cursor-pointer"
                          >
                            <div className="flex items-center gap-2 mb-1">
                              <h3 className="font-semibold text-gray-800 truncate">
                                {post.title}
                              </h3>
                              <span
                                className={`shrink-0 text-[10px] font-semibold rounded px-1.5 py-0.5 ${REVIEW_TYPE_BADGE[post.reviewType].className}`}
                              >
                                {REVIEW_TYPE_BADGE[post.reviewType].label}
                              </span>
                            </div>
                            {post.reviewType === "REVIEW" &&
                              post.rating !== null && (
                                <div className="mb-1">
                                  <RatingStars rating={post.rating} />
                                </div>
                              )}
                            {post.reviewType === "REVIEW" && post.summary && (
                              <p className="mb-1 text-xs text-indigo-600 bg-indigo-50 border border-indigo-100 rounded-lg px-2 py-1 leading-relaxed">
                                <span className="font-semibold">
                                  ✨ AI 요약:
                                </span>{" "}
                                {post.summary}
                              </p>
                            )}
                            <p className="text-xs text-blue-600 font-semibold">
                              {post.concertName}
                            </p>
                            <p className="text-xs text-gray-400 mt-1">
                              {formatDateTime(post.createdAt)} · 좋아요{" "}
                              {post.likeCount}
                            </p>
                          </div>
                        ))}
                      </div>
                    )}
                    {myPostsTotalPages > 1 && (
                      <div className="flex items-center justify-center gap-2 mt-8">
                        <button
                          onClick={() => fetchMyPosts(myPostsPage - 1)}
                          disabled={myPostsPage === 0}
                          className="px-3 py-2 rounded-lg border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-default"
                        >
                          이전
                        </button>
                        {Array.from(
                          { length: myPostsTotalPages },
                          (_, i) => i,
                        ).map((page) => (
                          <button
                            key={page}
                            onClick={() => fetchMyPosts(page)}
                            className={`w-10 h-10 rounded-lg border text-sm font-semibold ${myPostsPage === page ? "bg-blue-600 border-blue-600 text-white" : "bg-white border-gray-200 text-gray-600 hover:bg-gray-50"}`}
                          >
                            {page + 1}
                          </button>
                        ))}
                        <button
                          onClick={() => fetchMyPosts(myPostsPage + 1)}
                          disabled={myPostsPage >= myPostsTotalPages - 1}
                          className="px-3 py-2 rounded-lg border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-default"
                        >
                          다음
                        </button>
                      </div>
                    )}
                  </>
                )}

                {postsSubTab === "bookmarks" && (
                  <>
                    <div className="flex items-center justify-between mb-4">
                      <h2 className="text-lg font-bold text-gray-700">
                        북마크한 게시글
                      </h2>
                      <span className="text-sm text-gray-400">
                        {bookmarksTotalElements}개
                      </span>
                    </div>
                    {bookmarksLoading ? (
                      <p className="text-sm text-gray-400 text-center py-10">
                        불러오는 중...
                      </p>
                    ) : bookmarks.length === 0 ? (
                      <p className="text-sm text-gray-400 text-center py-10">
                        북마크한 게시글이 없습니다.
                      </p>
                    ) : (
                      <div className="space-y-3">
                        {bookmarks.map((b) => (
                          <div
                            key={b.postId}
                            onClick={() => router.push(`/board/${b.postId}`)}
                            role="button"
                            tabIndex={0}
                            className="flex gap-3 p-4 border border-gray-100 rounded-xl hover:shadow-md hover:border-blue-200 transition cursor-pointer"
                          >
                            <div className="relative shrink-0 w-12 h-16 rounded-lg overflow-hidden bg-gradient-to-br from-blue-100 to-indigo-200 flex items-center justify-center">
                              {b.posterUrl ? (
                                <Image
                                  fill
                                  unoptimized
                                  src={getLocalConcertPoster(b.posterUrl)}
                                  alt={b.concertName}
                                  sizes="48px"
                                  className="object-cover"
                                />
                              ) : (
                                <span className="text-[10px] text-gray-400">
                                  포스터
                                </span>
                              )}
                            </div>
                            <div className="min-w-0 flex-1">
                              <h3 className="font-semibold text-gray-800 truncate">
                                {b.title}
                              </h3>
                              <p className="text-xs text-blue-600 font-semibold mt-0.5">
                                {b.concertName}
                              </p>
                              <p className="text-xs text-gray-400 mt-1">
                                {b.userName} · {formatDateTime(b.bookmarkedAt)}{" "}
                                북마크
                              </p>
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                    {bookmarksTotalPages > 1 && (
                      <div className="flex items-center justify-center gap-2 mt-8">
                        <button
                          onClick={() => fetchBookmarks(bookmarksPage - 1)}
                          disabled={bookmarksPage === 0}
                          className="px-3 py-2 rounded-lg border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-default"
                        >
                          이전
                        </button>
                        {Array.from(
                          { length: bookmarksTotalPages },
                          (_, i) => i,
                        ).map((page) => (
                          <button
                            key={page}
                            onClick={() => fetchBookmarks(page)}
                            className={`w-10 h-10 rounded-lg border text-sm font-semibold ${bookmarksPage === page ? "bg-blue-600 border-blue-600 text-white" : "bg-white border-gray-200 text-gray-600 hover:bg-gray-50"}`}
                          >
                            {page + 1}
                          </button>
                        ))}
                        <button
                          onClick={() => fetchBookmarks(bookmarksPage + 1)}
                          disabled={bookmarksPage >= bookmarksTotalPages - 1}
                          className="px-3 py-2 rounded-lg border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-default"
                        >
                          다음
                        </button>
                      </div>
                    )}
                  </>
                )}

                {postsSubTab === "likes" && (
                  <>
                    <div className="flex items-center justify-between mb-4">
                      <h2 className="text-lg font-bold text-gray-700">
                        좋아요한 게시글
                      </h2>
                      <span className="text-sm text-gray-400">
                        {likesTotalElements}개
                      </span>
                    </div>
                    {likesLoading ? (
                      <p className="text-sm text-gray-400 text-center py-10">
                        불러오는 중...
                      </p>
                    ) : likes.length === 0 ? (
                      <p className="text-sm text-gray-400 text-center py-10">
                        좋아요한 게시글이 없습니다.
                      </p>
                    ) : (
                      <div className="space-y-3">
                        {likes.map((l) => (
                          <div
                            key={l.postId}
                            onClick={() => router.push(`/board/${l.postId}`)}
                            role="button"
                            tabIndex={0}
                            className="flex gap-3 p-4 border border-gray-100 rounded-xl hover:shadow-md hover:border-blue-200 transition cursor-pointer"
                          >
                            <div className="relative shrink-0 w-12 h-16 rounded-lg overflow-hidden bg-gradient-to-br from-blue-100 to-indigo-200 flex items-center justify-center">
                              {l.posterUrl ? (
                                <Image
                                  fill
                                  unoptimized
                                  src={getLocalConcertPoster(l.posterUrl)}
                                  alt={l.concertName}
                                  sizes="48px"
                                  className="object-cover"
                                />
                              ) : (
                                <span className="text-[10px] text-gray-400">
                                  포스터
                                </span>
                              )}
                            </div>
                            <div className="min-w-0 flex-1">
                              <h3 className="font-semibold text-gray-800 truncate">
                                {l.title}
                              </h3>
                              <p className="text-xs text-blue-600 font-semibold mt-0.5">
                                {l.concertName}
                              </p>
                              <p className="text-xs text-gray-400 mt-1">
                                {l.userName} · {formatDateTime(l.likedAt)}{" "}
                                좋아요
                              </p>
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                    {likesTotalPages > 1 && (
                      <div className="flex items-center justify-center gap-2 mt-8">
                        <button
                          onClick={() => fetchLikes(likesPage - 1)}
                          disabled={likesPage === 0}
                          className="px-3 py-2 rounded-lg border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-default"
                        >
                          이전
                        </button>
                        {Array.from(
                          { length: likesTotalPages },
                          (_, i) => i,
                        ).map((page) => (
                          <button
                            key={page}
                            onClick={() => fetchLikes(page)}
                            className={`w-10 h-10 rounded-lg border text-sm font-semibold ${likesPage === page ? "bg-blue-600 border-blue-600 text-white" : "bg-white border-gray-200 text-gray-600 hover:bg-gray-50"}`}
                          >
                            {page + 1}
                          </button>
                        ))}
                        <button
                          onClick={() => fetchLikes(likesPage + 1)}
                          disabled={likesPage >= likesTotalPages - 1}
                          className="px-3 py-2 rounded-lg border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-default"
                        >
                          다음
                        </button>
                      </div>
                    )}
                  </>
                )}
              </div>
            )}
          </div>
        </div>
      </div>

      {showWithdrawModal && (
        <div className="fixed inset-0 z-[100] bg-black/50 flex items-center justify-center p-4">
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

export default function MyPage() {
  return (
    <Suspense fallback={<div />}>
      <MyPageContent />
    </Suspense>
  );
}
