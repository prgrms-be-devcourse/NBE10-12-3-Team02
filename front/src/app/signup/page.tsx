"use client";

import { useState } from "react";
import Link from "next/link";
import { Eye, EyeOff } from "lucide-react";

export default function SignupPage() {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [loginId, setLoginId] = useState("");
  const [password, setPassword] = useState("");
  const [passwordCheck, setPasswordCheck] = useState("");
  const [showPassword, setShowPassword] = useState(false);

  const handleSignup = (e: React.FormEvent) => {
    e.preventDefault();

    if (name.trim() === "") {
      alert("이름을 입력해주세요.");
      return;
    }
    if (email.trim() === "") {
      alert("이메일을 입력해주세요.");
      return;
    }
    if (loginId.trim() === "") {
      alert("아이디를 입력해주세요.");
      return;
    }
    if (password.trim() === "") {
      alert("비밀번호를 입력해주세요.");
      return;
    }
    if (password !== passwordCheck) {
      alert("비밀번호가 일치하지 않습니다.");
      return;
    }

    console.log("회원가입 시도:", { name, email, loginId, password });
    alert("회원가입 시도! (나중에 여기서 API 호출)");
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100">
      <form onSubmit={handleSignup} className="w-96 p-10 bg-white rounded-2xl shadow-xl">
        <h1 className="text-3xl font-bold text-center mb-8 text-gray-800">
          회원가입
        </h1>

        <input
          type="text"
          placeholder="사용자 이름"
          value={name}
          onChange={(e) => setName(e.target.value)}
          className="w-full p-3 mb-3 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400"
        />
        <input
          type="email"
          placeholder="이메일"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className="w-full p-3 mb-3 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400"
        />

        <div className="flex gap-2 mb-3">
          <input
            type="text"
            placeholder="아이디"
            value={loginId}
            onChange={(e) => setLoginId(e.target.value)}
            className="flex-1 p-3 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400"
          />
          <button
            type="button"
            className="px-4 bg-gray-100 hover:bg-gray-200 rounded-lg text-sm font-semibold whitespace-nowrap"
          >
            중복확인
          </button>
        </div>

        {/* 비밀번호 + 보기 토글 */}
        <div className="relative mb-3">
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

        <input
          type={showPassword ? "text" : "password"}
          placeholder="비밀번호 확인"
          value={passwordCheck}
          onChange={(e) => setPasswordCheck(e.target.value)}
          className="w-full p-3 mb-6 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400"
        />

        <button
          type="submit"
          className="w-full p-3 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-bold transition"
        >
          회원가입 하기
        </button>

        <p className="text-center text-sm text-gray-500 mt-6">
          이미 계정이 있으신가요?{" "}
          <Link href="/login" className="text-blue-600 font-semibold cursor-pointer hover:underline">
            로그인
          </Link>
        </p>
      </form>
    </div>
  );
}