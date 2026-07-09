"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { User } from "lucide-react";
import { apiFetch, decodeToken, setAccessToken, restoreSession } from "@/lib/api";
import { showConfirm } from "@/lib/alert";

export default function Navbar() {
  const [userName, setUserName] = useState<string | null>(null);
  const [authChecked, setAuthChecked] = useState(false);

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

  const handleLogoClick = (e: React.MouseEvent<HTMLAnchorElement>) => {
    if (typeof window !== "undefined" && window.location.pathname === "/") {
      e.preventDefault();
      window.location.href = "/";
    }
  };

  const handleLogout = async () => {
    const confirmed = await showConfirm("로그아웃 하시겠습니까?", { danger: true });
    if (!confirmed) return;

    try {
      await apiFetch("/auth/logout", { method: "POST" });
    } catch {
    } finally {
      setAccessToken(null);
      // router.push만 하면 화면에 남아있는 상태(변수, 메모리)가 안 씻겨나갈 수 있어서
      // 아예 브라우저를 새로고침시켜서 완전히 초기 상태로 되돌린다.
      window.location.href = "/";
    }
  };

  return (
    <nav className="print:hidden sticky top-0 z-50 bg-white border-b border-gray-100 shadow-sm">
      <div className="max-w-5xl mx-auto px-6 h-16 flex items-center justify-between">
        <Link href="/" onClick={handleLogoClick} className="flex items-center">
          <img src="/images/logo-horizontal.svg" alt="티케팅고" className="h-12 w-auto object-contain block" />
        </Link>
        <div className="flex items-center gap-6 text-sm font-semibold text-gray-600">
          <Link href="/mypage" className="flex items-center gap-1 hover:text-blue-600 transition">
            <User size={18} />
            마이페이지
          </Link>
          {!authChecked ? (
            // 로그인 여부 확인 중 — "로그인 안 한 상태"로 잘못 깜빡이지 않도록, 확인될 때까지는
            // 아무 것도(로그인 버튼도, 로그인 정보도) 보여주지 않는다. 자리만 비슷하게 잡아둔다.
            <div className="w-24 h-9" />
          ) : userName ? (
            <>
              <span className="text-gray-500">{userName}님</span>
              <button
                onClick={handleLogout}
                className="px-4 py-2 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-lg transition"
              >
                로그아웃
              </button>
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