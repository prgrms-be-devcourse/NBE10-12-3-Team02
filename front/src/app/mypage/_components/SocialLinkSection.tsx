"use client";

import { useState, useEffect } from "react";
import Image from "next/image";
import { apiFetch, BASE_URL } from "@/lib/api";
import { showError } from "@/lib/alert";
import { SocialBadge } from "./SocialBadge";

interface SocialLinkStatus {
  linked: boolean;
  provider: string | null;
}

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

interface SocialLinkSectionProps {
  // page.tsx가 ?socialLink=success 감지 후 이 값을 올려 재조회를 트리거한다.
  refreshKey: number;
}

export function SocialLinkSection({ refreshKey }: SocialLinkSectionProps) {
  const [socialLinkStatus, setSocialLinkStatus] =
    useState<SocialLinkStatus | null>(null);
  const [socialLinkLoading, setSocialLinkLoading] = useState(true);
  const [socialLinkStarting, setSocialLinkStarting] = useState<string | null>(
    null,
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

  // mount 시 + refreshKey 변경(연동 성공 콜백) 시 재조회
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchSocialLinkStatus();
  }, [refreshKey]);

  // bfcache(뒤로가기)로 돌아왔을 때 연동 상태 갱신
  useEffect(() => {
    const handlePageShow = (e: PageTransitionEvent) => {
      if (e.persisted) {
        setSocialLinkStarting(null);
        fetchSocialLinkStatus();
      }
    };
    window.addEventListener("pageshow", handlePageShow);
    return () => window.removeEventListener("pageshow", handlePageShow);
  }, []);

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

  return (
    <div className="bg-white rounded-2xl shadow-sm p-8">
      <h2 className="text-lg font-bold text-gray-700 mb-4">소셜 계정 연동</h2>
      {socialLinkLoading ? (
        <p className="text-sm text-gray-400">불러오는 중...</p>
      ) : socialLinkStatus?.linked ? (
        <div className="flex items-center gap-3 p-4 bg-gray-50 rounded-xl border border-gray-100">
          <SocialBadge provider={socialLinkStatus.provider!} size={24} />
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
            소셜 계정을 연동하면 해당 소셜 계정으로도 로그인할 수 있습니다.
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
                {socialLinkStarting === p.key ? "연결 중..." : p.label}
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
