import Link from "next/link";
import { Ticket } from "lucide-react";

export default function Footer() {
  return (
    <footer className="bg-white border-t border-gray-100 mt-auto">
      <div className="max-w-5xl mx-auto px-6 py-10">
        <div className="flex flex-col md:flex-row justify-between gap-8">
          {/* 로고 + 소개 */}
          <div>
            <Link href="/" className="flex items-center gap-2 font-bold text-lg text-blue-600 mb-3">
              <Ticket size={20} />
              티케팅고
            </Link>
            <p className="text-sm text-gray-400 leading-relaxed">
              콘서트부터 페스티벌까지,<br />
              원하는 공연을 가장 빠르게 예매하세요.
            </p>
          </div>

          {/* 링크 */}
          <div className="flex gap-16">
            <div>
              <h3 className="font-bold text-gray-700 mb-3 text-sm">서비스</h3>
              <ul className="space-y-2 text-sm text-gray-400">
                <li><Link href="/" className="hover:text-blue-600 transition">공연 목록</Link></li>
                <li><Link href="/login" className="hover:text-blue-600 transition">로그인</Link></li>
                <li><Link href="/signup" className="hover:text-blue-600 transition">회원가입</Link></li>
                <li><Link href="/mypage" className="hover:text-blue-600 transition">마이페이지</Link></li>
              </ul>
            </div>

            <div>
              <h3 className="font-bold text-gray-700 mb-3 text-sm">정보</h3>
              <ul className="space-y-2 text-sm text-gray-400">
                <li><span className="cursor-default">이용약관</span></li>
                <li><span className="cursor-default">개인정보처리방침</span></li>
                <li><span className="cursor-default">고객센터</span></li>
              </ul>
            </div>
          </div>
        </div>

        {/* 하단 카피라이트 */}
        <div className="border-t border-gray-100 mt-8 pt-6 text-center text-xs text-gray-300">
          © 2026 티케팅고. All rights reserved.
        </div>
      </div>
    </footer>
  );
}