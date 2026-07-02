"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Ticket, User } from "lucide-react";
import { apiFetch, decodeToken, setAccessToken, restoreSession } from "@/lib/api";

export default function Navbar() {
  const router = useRouter();
  const [userName, setUserName] = useState<string | null>(null);

  useEffect(() => {
    const syncAuth = () => {
      const decoded = decodeToken();
      setUserName(decoded?.name ?? null);
    };

    // 새로고침으로 메모리 토큰이 날아갔어도, refreshToken 쿠키로 세션 복구 시도
    restoreSession().then(syncAuth);

    window.addEventListener("auth-changed", syncAuth);
    return () => window.removeEventListener("auth-changed", syncAuth);
  }, []);

  const handleLogout = async () => {
    try {
      await apiFetch("/auth/logout", { method: "POST" });
    } catch {
      // 로그아웃 API가 실패해도 클라이언트 토큰은 지움
    } finally {
      setAccessToken(null);
      router.push("/");
    }
  };

  return (
    <nav className="sticky top-0 z-50 bg-white border-b border-gray-100 shadow-sm">
      <div className="max-w-5xl mx-auto px-6 h-16 flex items-center justify-between">
        <Link href="/" className="flex items-center gap-2 font-bold text-xl text-blue-600">
          <Ticket size={24} />
          티케팅고
        </Link>

        <div className="flex items-center gap-6 text-sm font-semibold text-gray-600">
          <Link href="/mypage" className="flex items-center gap-1 hover:text-blue-600 transition">
            <User size={18} />
            마이페이지
          </Link>
          {userName ? (
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