"use client";

import { useState, useEffect, useRef } from "react";
import Image from "next/image";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { Bell, ChevronDown } from "lucide-react";
import { fetchEventSource } from "@microsoft/fetch-event-source";
import {
  apiFetch,
  BASE_URL,
  decodeToken,
  getAccessToken,
  setAccessToken,
  restoreSession,
} from "@/lib/api";
import { showConfirm } from "@/lib/alert";

interface NotificationItem {
  notificationId: number;
  type: "LIKE" | "FOLLOW";
  actorId: number;
  actorName: string;
  actorProfileImgUrl: string;
  targetType: string | null;
  targetId: number | null;
  isRead: boolean;
  createdAt: string;
}

interface NotificationPushPayload {
  notificationId: number;
  type: "LIKE" | "FOLLOW";
  actorId: number;
  actorName: string;
  actorProfileImgUrl: string;
  targetType: string | null;
  targetId: number | null;
  createdAt: string | null;
}

interface PageContent<T> {
  content: T[];
}

interface UserProfile {
  profileImageUrl: string;
}

export default function Navbar() {
  const pathname = usePathname();
  const router = useRouter();
  const [userName, setUserName] = useState<string | null>(null);
  const [authChecked, setAuthChecked] = useState(false);
  const [profileImgUrl, setProfileImgUrl] = useState<string | null>(null);
  const [profileImgError, setProfileImgError] = useState(false);
  const [profileCacheKey, setProfileCacheKey] = useState(() => Date.now());

  // 알림 드롭다운
  const [unreadCount, setUnreadCount] = useState(0);
  const [isBellOpen, setIsBellOpen] = useState(false);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [notifLoading, setNotifLoading] = useState(false);

  // 유저 드롭다운
  const [isUserOpen, setIsUserOpen] = useState(false);

  const sseAbortRef = useRef<AbortController | null>(null);
  const bellRef = useRef<HTMLDivElement>(null);
  const isBellOpenRef = useRef(false);
  const userRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const syncAuth = () => {
      const decoded = decodeToken();
      setUserName(decoded?.name ?? null);
      setAuthChecked(true);
    };
    restoreSession().then(syncAuth);
    window.addEventListener("auth-changed", syncAuth);
    return () => window.removeEventListener("auth-changed", syncAuth);
  }, []);

  const fetchProfileImage = () => {
    apiFetch<UserProfile>("/users/me")
      .then((res) => {
        setProfileImgUrl(res.data.profileImageUrl || null);
        setProfileImgError(false);
        setProfileCacheKey(Date.now());
      })
      .catch(() => {});
  };

  // 마이페이지에서 프로필 사진을 변경하면 발행되는 이벤트를 받아 새로고침 없이 갱신한다.
  // redirectToProfileImg URL은 유저ID 기준 고정 URL이라 캐시버스팅(profileCacheKey)이 없으면
  // 브라우저가 20분간 캐싱된 이전 이미지를 계속 보여준다.
  useEffect(() => {
    if (!userName) return;
    window.addEventListener("profile-image-changed", fetchProfileImage);
    return () =>
      window.removeEventListener("profile-image-changed", fetchProfileImage);
  }, [userName]);

  useEffect(() => {
    if (!userName) {
      sseAbortRef.current?.abort();
      sseAbortRef.current = null;
      // 로그아웃 시 알림·프로필 상태를 초기화하는 로직이라 effect 안에서 setState가 맞다.
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setUnreadCount(0);
      setNotifications([]);
      setProfileImgUrl(null);
      setProfileImgError(false);
      return;
    }

    apiFetch<number>("/notifications/unread-count")
      .then((res) => setUnreadCount(res.data))
      .catch(() => {});

    fetchProfileImage();

    const controller = new AbortController();
    sseAbortRef.current = controller;

    fetchEventSource(`${BASE_URL}/api/v1/notifications/subscribe`, {
      signal: controller.signal,
      headers: { Authorization: `Bearer ${getAccessToken() ?? ""}` },
      openWhenHidden: true,
      async onopen(response) {
        if (!response.ok) throw new Error("알림 SSE 연결 실패");
      },
      onmessage(msg) {
        if (msg.event !== "notification" || !msg.data) return;
        const payload = JSON.parse(msg.data) as NotificationPushPayload;
        setUnreadCount((prev) => prev + 1);

        // 브라우저 알림: 권한 허용 + 현재 탭이 숨겨진 경우에만 표시
        if (
          typeof Notification !== "undefined" &&
          Notification.permission === "granted" &&
          document.hidden
        ) {
          const title = payload.type === "LIKE" ? "새 좋아요" : "새 팔로워";
          const body =
            payload.type === "LIKE"
              ? `${payload.actorName}님이 회원님의 게시글을 좋아합니다`
              : `${payload.actorName}님이 회원님을 팔로우합니다`;
          const iconUrl = payload.actorProfileImgUrl
            ? payload.actorProfileImgUrl
            : `${window.location.origin}/default-avatar.svg`;
          const browserNotif = new Notification(title, { body, icon: iconUrl });
          browserNotif.onclick = () => {
            window.focus();
            router.push(
              payload.targetType === "POST" && payload.targetId != null
                ? `/board/${payload.targetId}`
                : `/users/${payload.actorId}`,
            );
          };
        }

        if (isBellOpenRef.current) {
          const newItem: NotificationItem = {
            ...payload,
            isRead: false,
            // 서버가 DB 저장 시각을 내려주므로 그대로 쓴다. GET /notifications로 다시
            // 조회했을 때와 같은 값이어야 하며, 클라이언트에서 임의로 지어내지 않는다.
            createdAt: payload.createdAt ?? new Date().toISOString(),
          };
          setNotifications((prev) => [newItem, ...prev]);
        }
      },
      onerror(err) {
        if ((err as Error)?.name === "AbortError") throw err;
      },
    });

    return () => controller.abort();
  }, [userName, router]);

  // 알림 드롭다운 외부 클릭
  useEffect(() => {
    if (!isBellOpen) return;
    const handler = (e: MouseEvent) => {
      if (bellRef.current && !bellRef.current.contains(e.target as Node)) {
        setIsBellOpen(false);
        isBellOpenRef.current = false;
      }
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, [isBellOpen]);

  // 유저 드롭다운 외부 클릭
  useEffect(() => {
    if (!isUserOpen) return;
    const handler = (e: MouseEvent) => {
      if (userRef.current && !userRef.current.contains(e.target as Node)) {
        setIsUserOpen(false);
      }
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, [isUserOpen]);

  const handleBellClick = async () => {
    // 종 아이콘 첫 클릭 시 브라우저 알림 권한 요청 (아직 물어본 적 없을 때만)
    if (typeof Notification !== "undefined" && Notification.permission === "default") {
      await Notification.requestPermission();
    }

    const next = !isBellOpen;
    setIsBellOpen(next);
    isBellOpenRef.current = next;
    if (!next) return;

    setNotifLoading(true);
    try {
      const res = await apiFetch<PageContent<NotificationItem>>(
        "/notifications?size=20",
      );
      setNotifications(res.data.content);
    } catch {
      setNotifications([]);
    } finally {
      setNotifLoading(false);
    }
  };

  const handleNotifClick = async (notif: NotificationItem) => {
    if (!notif.isRead) {
      try {
        await apiFetch(`/notifications/${notif.notificationId}/read`, {
          method: "PATCH",
        });
        setNotifications((prev) =>
          prev.map((n) =>
            n.notificationId === notif.notificationId
              ? { ...n, isRead: true }
              : n,
          ),
        );
        setUnreadCount((prev) => Math.max(0, prev - 1));
      } catch {}
    }
    setIsBellOpen(false);
    isBellOpenRef.current = false;
    router.push(
      notif.targetType === "POST" && notif.targetId != null
        ? `/board/${notif.targetId}`
        : `/users/${notif.actorId}`,
    );
  };

  const handleReadAll = async () => {
    try {
      await apiFetch("/notifications/read-all", { method: "PATCH" });
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
      setUnreadCount(0);
    } catch {}
  };

  const handleUserMenuClick = (path: string) => {
    setIsUserOpen(false);
    router.push(path);
  };

  const handleLogout = async () => {
    setIsUserOpen(false);
    const confirmed = await showConfirm("로그아웃 하시겠습니까?", {
      confirmText: "로그아웃",
      danger: true,
    });
    if (!confirmed) return;
    try {
      await apiFetch("/auth/logout", { method: "POST" });
    } catch {
    } finally {
      setAccessToken(null);
      window.location.href = "/";
    }
  };

  const badgeLabel =
    unreadCount > 99 ? "99+" : unreadCount > 0 ? String(unreadCount) : null;

  if (pathname === "/login" || pathname === "/signup") return null;

  const handleLogoClick = (e: React.MouseEvent<HTMLAnchorElement>) => {
    if (typeof window !== "undefined" && window.location.pathname === "/") {
      e.preventDefault();
      window.location.reload();
    }
  };

  return (
    <nav className="print:hidden sticky top-0 z-50 bg-white shadow-sm">
      <div className="max-w-5xl mx-auto px-6 h-16 flex items-center justify-between">
        <Link href="/" onClick={handleLogoClick} className="flex items-center">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img
            src="/images/logo-horizontal.svg"
            alt="티케팅고"
            className="h-12 w-auto object-contain block"
          />
        </Link>

        <div className="flex items-center gap-4 text-sm font-semibold text-gray-600">
          {!authChecked ? (
            <div className="w-24 h-9" />
          ) : userName ? (
            <>
              {/* 프로필 사진 → /mypage */}
              <Link href="/mypage" className="flex-shrink-0">
                <div className="w-8 h-8 rounded-full overflow-hidden bg-gray-100 ring-2 ring-gray-200 hover:ring-blue-400 transition">
                  <Image
                    unoptimized
                    src={
                      !profileImgUrl || profileImgError
                        ? "/default-avatar.svg"
                        : `${profileImgUrl}?t=${profileCacheKey}`
                    }
                    alt={userName}
                    width={32}
                    height={32}
                    className="object-cover w-full h-full"
                    onError={() => setProfileImgError(true)}
                  />
                </div>
              </Link>

              {/* 닉네임 드롭다운 */}
              <div className="relative" ref={userRef}>
                <button
                  onClick={() => setIsUserOpen((v) => !v)}
                  className="flex items-center gap-1 text-gray-700 hover:text-blue-600 transition"
                >
                  <span>{userName}</span>
                  <ChevronDown size={14} />
                </button>
                {isUserOpen && (
                  <div className="absolute right-0 top-9 w-36 bg-white rounded-xl shadow-lg border border-gray-100 z-50 py-1 overflow-hidden">
                    <button
                      onClick={() => handleUserMenuClick("/mypage?tab=info")}
                      className="w-full px-4 py-2 text-left text-sm text-gray-700 hover:bg-gray-50 transition"
                    >
                      내 정보
                    </button>
                    <button
                      onClick={() => handleUserMenuClick("/mypage?tab=tickets")}
                      className="w-full px-4 py-2 text-left text-sm text-gray-700 hover:bg-gray-50 transition"
                    >
                      내 티켓
                    </button>
                    <button
                      onClick={() => handleUserMenuClick("/mypage?tab=posts")}
                      className="w-full px-4 py-2 text-left text-sm text-gray-700 hover:bg-gray-50 transition"
                    >
                      내 게시글
                    </button>
                    <div className="border-t border-gray-100 my-1" />
                    <button
                      onClick={handleLogout}
                      className="w-full px-4 py-2 text-left text-sm text-red-500 hover:bg-gray-50 transition"
                    >
                      로그아웃
                    </button>
                  </div>
                )}
              </div>

              {/* 알림 종 아이콘 */}
              <div className="relative" ref={bellRef}>
                <button
                  onClick={handleBellClick}
                  className="relative p-1 text-gray-600 hover:text-blue-600 transition"
                  aria-label="알림"
                >
                  <Bell size={20} />
                  {badgeLabel && (
                    <span className="absolute -top-1 -right-1 min-w-[18px] h-[18px] px-1 flex items-center justify-center bg-red-500 text-white text-[10px] font-bold rounded-full leading-none">
                      {badgeLabel}
                    </span>
                  )}
                </button>

                {isBellOpen && (
                  <div className="absolute right-0 top-10 w-80 bg-white rounded-xl shadow-lg border border-gray-100 z-50 overflow-hidden">
                    <div className="flex items-center justify-between px-4 py-3 border-b border-gray-100">
                      <span className="font-bold text-gray-800 text-sm">
                        알림
                      </span>
                      <button
                        onClick={handleReadAll}
                        className="text-xs text-blue-500 hover:text-blue-700 transition font-medium"
                      >
                        모두 읽음
                      </button>
                    </div>
                    <div className="max-h-96 overflow-y-auto">
                      {notifLoading ? (
                        <p className="text-center text-sm text-gray-400 py-8">
                          불러오는 중...
                        </p>
                      ) : notifications.length === 0 ? (
                        <p className="text-center text-sm text-gray-400 py-8">
                          알림이 없습니다
                        </p>
                      ) : (
                        notifications.map((notif) => (
                          <button
                            key={notif.notificationId}
                            onClick={() => handleNotifClick(notif)}
                            className={`w-full flex items-center gap-3 px-4 py-3 text-left hover:bg-gray-50 transition ${
                              !notif.isRead ? "bg-blue-50" : ""
                            }`}
                          >
                            <div className="flex-shrink-0 w-9 h-9 rounded-full overflow-hidden bg-gray-100">
                              <Image
                                unoptimized
                                src={
                                  notif.actorProfileImgUrl ||
                                  "/default-avatar.svg"
                                }
                                alt={notif.actorName}
                                width={36}
                                height={36}
                                className="object-cover w-full h-full"
                                onError={(e) => {
                                  (e.currentTarget as HTMLImageElement).src =
                                    "/default-avatar.svg";
                                }}
                              />
                            </div>
                            <div className="flex-1 min-w-0">
                              <p className="text-sm text-gray-800 leading-snug">
                                {notif.type === "LIKE"
                                  ? `${notif.actorName}님이 회원님의 게시글을 좋아합니다`
                                  : `${notif.actorName}님이 회원님을 팔로우합니다`}
                              </p>
                              {notif.createdAt && (
                                <p className="text-xs text-gray-400 mt-0.5">
                                  {new Date(notif.createdAt).toLocaleString(
                                    "ko-KR",
                                    {
                                      month: "numeric",
                                      day: "numeric",
                                      hour: "2-digit",
                                      minute: "2-digit",
                                    },
                                  )}
                                </p>
                              )}
                            </div>
                            {!notif.isRead && (
                              <div className="flex-shrink-0 w-2 h-2 bg-blue-500 rounded-full" />
                            )}
                          </button>
                        ))
                      )}
                    </div>
                  </div>
                )}
              </div>
            </>
          ) : (
            <Link
              href="/login"
              className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition"
            >
              로그인
            </Link>
          )}
        </div>
      </div>
    </nav>
  );
}
