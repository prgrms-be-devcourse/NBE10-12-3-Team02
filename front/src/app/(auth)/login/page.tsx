"use client";

import { useState } from "react";
import Link from "next/link";
import { Eye, EyeOff } from "lucide-react";

export default function LoginPage() {
  const [loginId, setLoginId] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();

    if (loginId.trim() === "") {
      alert("아이디를 입력해주세요.");
      return;
    }
    if (password.trim() === "") {
      alert("비밀번호를 입력해주세요.");
      return;
    }

    console.log("로그인 시도:", loginId, password);
    alert("로그인 시도! (나중에 여기서 API 호출)");
  };

  return (
    <div className="h-screen overflow-hidden flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100">
      <form onSubmit={handleLogin} className="w-96 p-10 bg-white rounded-2xl shadow-xl">
        <Link href="/" className="block text-3xl font-bold text-center mb-8 text-gray-800">
          티케팅고 🎫
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
          className="w-full p-3 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-bold transition"
        >
          로그인
        </button>

        <p className="text-center text-sm text-gray-500 mt-6">
          아직 회원이 아니신가요?{" "}
          <Link href="/signup" className="text-blue-600 font-semibold hover:underline">
            회원가입
          </Link>
        </p>
      </form>
    </div>
  );
}