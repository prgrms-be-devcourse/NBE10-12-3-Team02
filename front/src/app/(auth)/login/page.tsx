"use client";

import { useState, useEffect, useRef, Suspense } from "react";
import Image from "next/image";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { Eye, EyeOff } from "lucide-react";
import { apiFetch } from "@/lib/api";
import { showAlert, showError } from "@/lib/alert";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL;

// 서버가 로그인 실패 시 붙여서 보내주는 에러 코드를, 사람이 읽을 문구로 바꿔주는 표
const OAUTH_ERROR_MESSAGES: Record<string, string> = {
  oauth2_user_id_missing: "소셜 로그인 정보를 가져오지 못했습니다.",
  oauth2_user_id_invalid: "소셜 로그인 정보가 올바르지 않습니다.",
  oauth2_user_not_found: "가입된 회원 정보를 찾을 수 없습니다.",
  oauth2_token_issue_failed: "로그인 처리 중 오류가 발생했습니다.",
  oauth2_provider_id_missing: "소셜 로그인 정보를 가져오지 못했습니다.",
  oauth2_email_missing: "소셜 계정에서 이메일 정보를 가져오지 못했습니다.",
  oauth2_email_already_exists:
    "이미 해당 이메일로 가입된 계정이 있습니다. 아이디/비밀번호로 로그인하거나, 기존 계정에서 소셜 연동을 이용해주세요.",
  oauth2_account_already_used: "이미 다른 계정에 연결된 소셜 계정입니다.",
  oauth2_provider_not_supported: "지원하지 않는 소셜 로그인입니다.",
};

const SOCIAL_PROVIDERS = [
  {
    key: "kakao",
    label: "카카오로 시작하기",
    className: "bg-[#FEE500] hover:bg-[#fada0a] text-[#191919]",
  },
  {
    key: "naver",
    label: "네이버로 시작하기",
    className: "bg-[#03C75A] hover:bg-[#02b350] text-white",
  },
  {
    key: "google",
    label: "Google로 시작하기",
    className: "bg-white hover:bg-gray-50 text-gray-700 border border-gray-200",
  },
] as const;

function LoginContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [loginId, setLoginId] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const hasHandledRef = useRef(false);

  // 소셜 로그인이 실패해서 서버가 /login?error=... 로 돌려보낸 경우, 그 이유를 안내한다.
  // hasHandledRef: Strict Mode 이중 실행에서 alert이 두 번 뜨는 걸 막는다.
  // router는 안정 참조라 deps에서 제외한다.
  useEffect(() => {
    const errorCode = searchParams.get("error");
    if (!errorCode) return;
    if (hasHandledRef.current) return;
    hasHandledRef.current = true;
    showAlert(
      OAUTH_ERROR_MESSAGES[errorCode] ?? "소셜 로그인 중 오류가 발생했습니다.",
    ).then(() => {
      router.replace("/login");
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams]);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();

    if (loginId.trim() === "") {
      showAlert("아이디를 입력해주세요.");
      return;
    }
    if (password.trim() === "") {
      showAlert("비밀번호를 입력해주세요.");
      return;
    }

    setIsSubmitting(true);
    try {
      await apiFetch("/auth/login", {
        method: "POST",
        body: JSON.stringify({ id: loginId, password }),
      });
      router.push("/");
    } catch (err) {
      showError(
        err instanceof Error ? err.message : "로그인 중 오류가 발생했습니다.",
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  // 소셜 로그인 버튼을 누르면, 우리 서버가 만들어둔 주소로 브라우저 전체를 이동시킨다.
  // (fetch가 아니라 페이지 이동! 카카오/네이버/구글 로그인 화면을 보여줘야 하기 때문)
  const handleSocialLogin = (provider: string) => {
    // window.location.href 대입은 브라우저 페이지 이동이지 리액트 상태 변경이 아니다.
    // eslint-disable-next-line react-hooks/immutability
    window.location.href = `${API_BASE_URL}/oauth2/authorization/${provider}`;
  };

  return (
    <div className="h-screen overflow-hidden flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100 px-4">
      <form
        onSubmit={handleLogin}
        className="w-full max-w-sm md:w-96 p-6 md:p-10 bg-white rounded-2xl shadow-xl"
      >
        <Link href="/" className="flex justify-center mb-8">
          <Image
            unoptimized
            priority
            src="/images/logo.svg"
            alt="티케팅고"
            width={112}
            height={112}
            className="h-28 w-28 object-contain"
          />
        </Link>

        <input
          type="text"
          placeholder="아이디"
          value={loginId}
          onChange={(e) => setLoginId(e.target.value)}
          className="w-full p-3 mb-3 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400"
        />

        <div className="relative mb-6">
          <input
            type={showPassword ? "text" : "password"}
            placeholder="비밀번호"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full p-3 pr-12 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400"
          />
          <button
            type="button"
            onClick={() => setShowPassword(!showPassword)}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
          >
            {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
          </button>
        </div>

        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full p-3 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-bold transition disabled:opacity-50"
        >
          {isSubmitting ? "로그인 중..." : "로그인"}
        </button>

        <div className="flex items-center gap-3 my-6">
          <div className="flex-1 h-px bg-gray-200" />
          <span className="text-xs text-gray-400">또는</span>
          <div className="flex-1 h-px bg-gray-200" />
        </div>

        <div className="space-y-2">
          {SOCIAL_PROVIDERS.map((provider) => (
            <button
              key={provider.key}
              type="button"
              onClick={() => handleSocialLogin(provider.key)}
              className={`w-full p-3 rounded-lg font-semibold transition ${provider.className}`}
            >
              {provider.label}
            </button>
          ))}
        </div>

        <p className="text-center text-sm text-gray-500 mt-6">
          아직 회원이 아니신가요?{" "}
          <Link
            href="/signup"
            className="text-blue-600 font-semibold hover:underline"
          >
            회원가입
          </Link>
        </p>
      </form>
    </div>
  );
}

export default function LoginPage() {
  return (
    <Suspense fallback={<div>로딩 중...</div>}>
      <LoginContent />
    </Suspense>
  );
}
