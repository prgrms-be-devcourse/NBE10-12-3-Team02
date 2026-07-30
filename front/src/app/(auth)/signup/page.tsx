"use client";

import PasswordStrengthMeter from "@/app/components/PasswordStrengthMeter";
import { showAlert, showError, showSuccess } from "@/lib/alert";
import { apiFetch } from "@/lib/api";
import { isValidEmail } from "@/lib/validators";
import { Eye, EyeOff } from "lucide-react";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";

interface ConfirmResponseData {
  verificationToken: string;
}

export default function SignupPage() {
  const router = useRouter();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [loginId, setLoginId] = useState("");
  const [password, setPassword] = useState("");
  const [passwordCheck, setPasswordCheck] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isIdChecked, setIsIdChecked] = useState(false);
  const [isChecking, setIsChecking] = useState(false);

  // 이메일 인증 관련 state
  const [verificationCode, setVerificationCode] = useState("");
  const [verificationToken, setVerificationToken] = useState("");
  const [isEmailSent, setIsEmailSent] = useState(false);
  const [isEmailVerified, setIsEmailVerified] = useState(false);
  const [isSendingEmail, setIsSendingEmail] = useState(false);
  const [isVerifyingCode, setIsVerifyingCode] = useState(false);

  const handleLoginIdChange = (value: string) => {
    setLoginId(value);
    setIsIdChecked(false);
  };

  const handleEmailChange = (value: string) => {
    setEmail(value);
    setIsEmailSent(false);
    setIsEmailVerified(false);
    setVerificationCode("");
    setVerificationToken("");
  };

  const handleCheckId = async () => {
    if (loginId.trim() === "") {
      showAlert("아이디를 입력해주세요.");
      return;
    }
    setIsChecking(true);
    try {
      await apiFetch(`/users/check-id?id=${encodeURIComponent(loginId)}`);
      showSuccess("사용 가능한 아이디입니다.");
      setIsIdChecked(true);
    } catch (e) {
      showError(e instanceof Error ? e.message : "중복확인에 실패했습니다.");
      setIsIdChecked(false);
    } finally {
      setIsChecking(false);
    }
  };

  const handleSendEmailVerification = async () => {
    if (email.trim() === "") {
      showAlert("이메일을 입력해주세요.");
      return;
    }
    if (!isValidEmail(email)) {
      showAlert("올바른 이메일 형식을 입력해주세요.");
      return;
    }

    setIsSendingEmail(true);
    try {
      await apiFetch("/auth/email-verifications", {
        method: "POST",
        body: JSON.stringify({ email }),
      });
      showSuccess("이메일로 인증번호가 발송되었습니다.");
      setIsEmailSent(true);
      setIsEmailVerified(false);
    } catch (e) {
      showError(
        e instanceof Error ? e.message : "인증번호 발송에 실패했습니다.",
      );
    } finally {
      setIsSendingEmail(false);
    }
  };

  const handleConfirmEmailVerification = async () => {
    if (verificationCode.trim().length !== 6) {
      showAlert("6자리 인증번호를 정확히 입력해주세요.");
      return;
    }

    setIsVerifyingCode(true);
    try {
      const res = await apiFetch<ConfirmResponseData>(
        "/auth/email-verifications/confirm",
        {
          method: "POST",
          body: JSON.stringify({ email, code: verificationCode }),
        },
      );

      if (res.data?.verificationToken) {
        setVerificationToken(res.data.verificationToken);
        setIsEmailVerified(true);
        showSuccess("이메일 인증이 완료되었습니다.");
      } else {
        throw new Error("인증 토큰을 전달받지 못했습니다.");
      }
    } catch (e) {
      showError(
        e instanceof Error
          ? e.message
          : "인증번호가 일치하지 않거나 만료되었습니다.",
      );
    } finally {
      setIsVerifyingCode(false);
    }
  };

  const handleSignup = async (e: React.FormEvent) => {
    e.preventDefault();

    if (name.trim() === "") {
      showAlert("이름을 입력해주세요.");
      return;
    }
    if (email.trim() === "") {
      showAlert("이메일을 입력해주세요.");
      return;
    }
    if (!isEmailVerified || !verificationToken) {
      showAlert("이메일 인증을 진행해주세요.");
      return;
    }
    if (loginId.trim() === "") {
      showAlert("아이디를 입력해주세요.");
      return;
    }
    if (!isIdChecked) {
      showAlert("아이디 중복확인을 먼저 진행해주세요.");
      return;
    }
    if (password.trim() === "") {
      showAlert("비밀번호를 입력해주세요.");
      return;
    }
    if (password.length < 8) {
      showAlert("비밀번호는 8자 이상이어야 합니다.");
      return;
    }
    if (password !== passwordCheck) {
      showAlert("비밀번호가 일치하지 않습니다.");
      return;
    }

    setIsSubmitting(true);
    try {
      await apiFetch("/users/signup", {
        method: "POST",
        body: JSON.stringify({
          id: loginId,
          email,
          password,
          name,
          verificationToken,
        }),
      });
      await showSuccess("회원가입이 완료되었습니다. 로그인해주세요.");
      router.push("/login");
    } catch (err) {
      showError(
        err instanceof Error ? err.message : "회원가입 중 오류가 발생했습니다.",
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="h-screen overflow-hidden flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100">
      <form
        onSubmit={handleSignup}
        className="w-96 p-10 bg-white rounded-2xl shadow-xl"
      >
        <div className="text-center">
          <Link href="/" className="flex justify-center">
            <Image
              unoptimized
              priority
              loading="eager"
              src="/images/logo.svg"
              alt="티케팅고"
              width={96}
              height={96}
              style={{ width: "96px", height: "96px" }}
              className="h-24 w-24 object-contain"
            />
          </Link>
          <p className="my-4 text-2xl font-bold text-gray-800">회원가입</p>
        </div>

        <input
          type="text"
          placeholder="사용자 이름"
          value={name}
          onChange={(e) => setName(e.target.value)}
          className="w-full p-3 mb-3 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400"
        />

        {/* 이메일 입력 + 인증번호 발송 버튼 */}
        <div className="flex gap-2 mb-3">
          <input
            type="email"
            placeholder="이메일"
            value={email}
            onChange={(e) => handleEmailChange(e.target.value)}
            disabled={isEmailVerified}
            className="flex-1 p-3 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400 disabled:bg-gray-50 disabled:text-gray-500"
          />
          <button
            type="button"
            onClick={handleSendEmailVerification}
            disabled={isSendingEmail || isEmailVerified}
            className={`px-4 rounded-lg text-sm font-semibold whitespace-nowrap transition ${
              isEmailVerified
                ? "bg-green-100 text-green-700"
                : "bg-gray-100 hover:bg-gray-200 text-gray-700"
            } disabled:opacity-50`}
          >
            {isSendingEmail
              ? "발송 중..."
              : isEmailVerified
                ? "인증완료"
                : isEmailSent
                  ? "재발송"
                  : "인증발송"}
          </button>
        </div>

        {/* 인증번호 6자리 입력 + 인증확인 버튼 */}
        {isEmailSent && !isEmailVerified && (
          <div className="flex gap-2 mb-3">
            <input
              type="text"
              placeholder="인증번호 6자리"
              maxLength={6}
              value={verificationCode}
              onChange={(e) => setVerificationCode(e.target.value)}
              className="flex-1 p-3 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400"
            />
            <button
              type="button"
              onClick={handleConfirmEmailVerification}
              disabled={isVerifyingCode}
              className="px-4 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-semibold whitespace-nowrap transition disabled:opacity-50"
            >
              {isVerifyingCode ? "확인 중..." : "인증확인"}
            </button>
          </div>
        )}

        <div className="flex gap-2 mb-3">
          <input
            type="text"
            placeholder="아이디"
            value={loginId}
            onChange={(e) => handleLoginIdChange(e.target.value)}
            className="flex-1 p-3 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400"
          />
          <button
            type="button"
            onClick={handleCheckId}
            disabled={isChecking}
            className={`px-4 rounded-lg text-sm font-semibold whitespace-nowrap transition ${
              isIdChecked
                ? "bg-green-100 text-green-700"
                : "bg-gray-100 hover:bg-gray-200 text-gray-700"
            } disabled:opacity-50`}
          >
            {isChecking ? "확인 중..." : isIdChecked ? "확인완료" : "중복확인"}
          </button>
        </div>

        <div className="relative mb-1">
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

        <PasswordStrengthMeter password={password} />

        <input
          type={showPassword ? "text" : "password"}
          placeholder="비밀번호 확인"
          value={passwordCheck}
          onChange={(e) => setPasswordCheck(e.target.value)}
          className="w-full p-3 mb-6 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400"
        />

        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full p-3 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-bold transition disabled:opacity-50"
        >
          {isSubmitting ? "가입 중..." : "회원가입 하기"}
        </button>

        <p className="text-center text-sm text-gray-500 mt-6">
          이미 계정이 있으신가요?{" "}
          <Link
            href="/login"
            className="text-blue-600 font-semibold hover:underline"
          >
            로그인
          </Link>
        </p>
      </form>
    </div>
  );
}
