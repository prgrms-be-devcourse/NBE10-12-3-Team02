"use client";

import { useState } from "react";
import Link from "next/link";

export default function Home() {
  const concerts = [
    { concertId: 1, concertName: "2026 SUMMER LIVE", venueName: "올림픽 체조경기장", startDate: "2026-08-10", closeDate: "2026-08-09", lowestPrice: 77000 },
    { concertId: 2, concertName: "흠뻑쇼 - 대전", venueName: "대전월드컵경기장", startDate: "2026-07-20", closeDate: "2026-07-19", lowestPrice: 99000 },
    { concertId: 3, concertName: "재즈 페스티벌", venueName: "올림픽공원", startDate: "2026-09-05", closeDate: "2026-09-04", lowestPrice: 55000 },
    { concertId: 4, concertName: "클래식 갈라쇼", venueName: "예술의전당", startDate: "2026-08-25", closeDate: "2026-08-24", lowestPrice: 120000 },
  ];

  const [keyword, setKeyword] = useState("");
  const [sort, setSort] = useState("closingSoon");

  const filtered = concerts.filter((concert) =>
    concert.concertName.toLowerCase().includes(keyword.toLowerCase())
  );

  const sorted = [...filtered].sort((a, b) => {
    if (sort === "priceLow") return a.lowestPrice - b.lowestPrice;
    if (sort === "priceHigh") return b.lowestPrice - a.lowestPrice;
    return a.closeDate.localeCompare(b.closeDate); // closingSoon = 마감 가까운 순
  });

  return (
    <div className="min-h-screen bg-gray-50 p-10">
      <div className="max-w-5xl mx-auto">
        <h1 className="text-3xl font-bold text-gray-800 mb-8">🎫 TicketingGo</h1>

        {/* 검색 + 정렬 */}
        <div className="flex flex-col md:flex-row gap-3 mb-8">
          <input
            type="text"
            placeholder="콘서트 이름으로 검색"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            className="flex-1 p-3 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400"
          />
          <select
            value={sort}
            onChange={(e) => setSort(e.target.value)}
            className="p-3 border border-gray-200 rounded-lg bg-white text-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-400"
          >
            <option value="closingSoon">마감 임박순</option>
            <option value="priceLow">가격 낮은순</option>
            <option value="priceHigh">가격 높은순</option>
          </select>
        </div>

        {/* 콘서트 목록 */}
        {sorted.length === 0 ? (
          <p className="text-center text-gray-400 py-20">검색 결과가 없습니다.</p>
        ) : (
          <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
            {sorted.map((concert) => (
              <Link
                href={`/concerts/${concert.concertId}`}
                key={concert.concertId}
                className="bg-white rounded-2xl shadow-sm overflow-hidden hover:shadow-lg transition cursor-pointer"
              >
                <div className="h-48 bg-gradient-to-br from-blue-200 to-indigo-300 flex items-center justify-center text-white font-bold">
                  포스터
                </div>
                <div className="p-4">
                  <h2 className="font-bold text-gray-800 truncate">
                    {concert.concertName}
                  </h2>
                  <p className="text-sm text-gray-500 mt-1">{concert.venueName}</p>
                  <p className="text-sm text-gray-400 mt-1">{concert.startDate}</p>
                  <p className="text-blue-600 font-bold mt-2">
                    {concert.lowestPrice.toLocaleString()}원~
                  </p>
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}