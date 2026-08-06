"use client";

import { useState, useEffect, useRef, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import {
  apiFetch,
  decodeToken,
  restoreSession,
  setAccessToken,
} from "@/lib/api";
import { showAlert, showSuccess, showError } from "@/lib/alert";
import { ProfileCard } from "./_components/ProfileCard";
import { SocialLinkSection } from "./_components/SocialLinkSection";
import { TicketsSection } from "./_components/TicketsSection";
import { PostsSection } from "./_components/PostsSection";
import { WithdrawModal } from "./_components/WithdrawModal";
import type { MyPageData, TicketGroupInfo } from "./_components/types";

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

function MyPageContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const hasCheckedAuth = useRef(false);
  const hasSocialLinkHandledRef = useRef(false);

  const [data, setData] = useState<MyPageData | null>(null);
  const [loading, setLoading] = useState(true);
  const [showWithdrawModal, setShowWithdrawModal] = useState(false);
  const [socialRefreshKey, setSocialRefreshKey] = useState(0);

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

  // 소셜 연동 콜백 URL 파라미터 처리 (?socialLink=success / ?socialLinkError=...)
  // router.replace는 page 레벨 관심사이므로 여기서 처리하고,
  // 실제 상태 갱신은 socialRefreshKey를 올려 SocialLinkSection에 위임한다.
  useEffect(() => {
    if (hasSocialLinkHandledRef.current) return;
    hasSocialLinkHandledRef.current = true;
    const params = new URLSearchParams(window.location.search);
    const linked = params.get("socialLink");
    const linkError = params.get("socialLinkError");
    if (!linked && !linkError) return;
    if (linked === "success") {
      showSuccess("소셜 계정 연동이 완료되었습니다.").then(() => {
        setSocialRefreshKey((k) => k + 1);
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

  const handleDataUpdate = (name: string, email: string) => {
    setData((prev) => (prev ? { ...prev, name, email } : prev));
  };

  const handleCancelSuccess = (updated: TicketGroupInfo[]) => {
    setData((prev) => (prev ? { ...prev, ticketGroups: updated } : prev));
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <p className="text-gray-400">불러오는 중...</p>
      </div>
    );
  }

  if (!data) return null;

  return (
    <div className="min-h-screen bg-gray-50 p-4 md:p-10">
      <div className="max-w-4xl mx-auto">
        <div className="flex justify-between items-center mb-8">
          <div>
            <p className="text-gray-400 text-sm">안녕하세요</p>
            <h1 className="text-2xl font-bold text-gray-800">
              {data.name}님 👋
            </h1>
          </div>
        </div>

        <div className="flex flex-col md:flex-row md:items-start gap-6">
          <nav className="w-full md:w-48 shrink-0">
            <div className="bg-white rounded-2xl shadow-sm overflow-hidden flex md:block">
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
                  className={`flex-1 md:w-full text-center md:text-left px-4 py-3.5 text-sm font-semibold transition border-b-2 md:border-b-0 md:border-l-4 ${
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
                <ProfileCard data={data} onDataUpdate={handleDataUpdate} />
                <SocialLinkSection refreshKey={socialRefreshKey} />
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
              <TicketsSection
                ticketGroups={data.ticketGroups}
                onCancelSuccess={handleCancelSuccess}
              />
            )}

            {activeTab === "posts" && <PostsSection />}
          </div>
        </div>
      </div>

      <WithdrawModal
        show={showWithdrawModal}
        onClose={() => setShowWithdrawModal(false)}
        onWithdraw={handleWithdraw}
      />
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
