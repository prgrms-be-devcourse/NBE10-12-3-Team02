"use client";

import { useState, useEffect, type ChangeEvent } from "react";
import Link from "next/link";
import { Search, ChevronLeft, ChevronRight } from "lucide-react";
import { apiFetch } from "@/lib/api";

interface ConcertListItem {
  concertId: number;
  concertName: string;
  venueName: string;
  startDate: string;
  endDate: string;
  imageUrl: string;
  status: string;
}

export default function Home() {
  const [concerts, setConcerts] = useState<ConcertListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [topConcerts, setTopConcerts] = useState<ConcertListItem[]>([]);

  const [keyword, setKeyword] = useState("");
  const [sort, setSort] = useState("closingSoon");
  const [slideIndex, setSlideIndex] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);

  const visibleCount = 3;
  const itemsPerPage = 12;

  // 검색/정렬에 반응하는 전체 목록
  useEffect(() => {
    const fetchConcerts = async () => {
      try {
        setLoading(true);
        setError("");
        const params = new URLSearchParams();
        if (keyword.trim() !== "") params.append("keyword", keyword);
        params.append("sort", sort);

        const res = await apiFetch<ConcertListItem[]>(`/concerts?${params.toString()}`);
        setConcerts(res.data);
      } catch (e) {
        setError(e instanceof Error ? e.message : "콘서트를 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    };

    fetchConcerts();
  }, [keyword, sort]);

  // 마감임박 슬라이드는 검색어/정렬과 무관하게 최초 1회만
  useEffect(() => {
    apiFetch<ConcertListItem[]>(`/concerts?sort=closingSoon`)
      .then((res) => setTopConcerts(res.data.slice(0, 5)))
      .catch(() => setTopConcerts([]));
  }, []);

  const maxIndex = Math.max(0, topConcerts.length - visibleCount);

  const prevSlide = () => setSlideIndex((i) => Math.max(0, i - 1));
  const nextSlide = () => setSlideIndex((i) => Math.min(maxIndex, i + 1));

  const totalPages = Math.ceil(concerts.length / itemsPerPage);
  const pagedConcerts = concerts.slice(
    (currentPage - 1) * itemsPerPage,
    currentPage * itemsPerPage
  );

  const handleKeywordChange = (e: ChangeEvent<HTMLInputElement>) => {
    setKeyword(e.target.value);
    setCurrentPage(1);
  };

  const handleSortChange = (e: ChangeEvent<HTMLSelectElement>) => {
    setSort(e.target.value);
    setCurrentPage(1);
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="bg-gradient-to-r from-blue-600 to-indigo-600 text-white">
        <div className="max-w-5xl mx-auto px-6 py-14">
          <h1 className="text-4xl font-bold mb-3">
            원하는 공연,
            <br />
            가장 빠르게 예매하세요
          </h1>
          <p className="text-blue-100 text-lg">
            콘서트부터 페스티벌까지, <span className="font-bold">티케팅고</span>에서 만나보세요.
          </p>
        </div>
      </div>

      <div className="max-w-5xl mx-auto px-6 py-10">
        <div className="mb-12 relative">
          <button
            onClick={prevSlide}
            disabled={slideIndex === 0}
            className="absolute left-0 top-1/2 -translate-y-1/2 -translate-x-1/2 z-10 p-2 rounded-full bg-white border border-gray-200 shadow-md hover:bg-gray-50 disabled:opacity-0 disabled:cursor-default"
          >
            <ChevronLeft size={24} />
          </button>

          <button
            onClick={nextSlide}
            disabled={slideIndex === maxIndex}
            className="absolute right-0 top-1/2 -translate-y-1/2 translate-x-1/2 z-10 p-2 rounded-full bg-white border border-gray-200 shadow-md hover:bg-gray-50 disabled:opacity-0 disabled:cursor-default"
          >
            <ChevronRight size={24} />
          </button>

          <div className="overflow-hidden">
            <div
              className="flex gap-4 transition-transform duration-300"
              style={{
                transform: `translateX(calc(-${slideIndex} * ((100% - 32px) / 3 + 16px)))`,
              }}
            >
              {topConcerts.map((concert) => (
                <Link
                  href={`/concerts/${concert.concertId}`}
                  key={concert.concertId}
                  className="shrink-0 bg-white rounded-2xl shadow-sm overflow-hidden hover:shadow-lg transition"
                  style={{ width: `calc((100% - 32px) / 3)` }}
                >
                  <div className="h-56 bg-gradient-to-br from-blue-200 to-indigo-300 flex items-center justify-center text-white font-bold relative overflow-hidden">
                    {concert.imageUrl ? (
                      <img src={concert.imageUrl} alt={concert.concertName} className="w-full h-full object-cover" />
                    ) : (
                      "포스터"
                    )}
                    {concert.status === "CLOSED" && (
                      <span className="absolute top-3 left-3 bg-gray-600 text-white text-xs px-2 py-1 rounded-full">
                        마감
                      </span>
                    )}
                  </div>
                  <div className="p-4">
                    <h3 className="font-bold text-gray-800 truncate">
                      {concert.concertName}
                    </h3>
                    <p className="text-sm text-gray-500 mt-1">{concert.venueName}</p>
                    <p className="text-sm text-gray-400 mt-1">
                      {concert.startDate?.slice(0, 10)}
                    </p>
                  </div>
                </Link>
              ))}
            </div>
          </div>
        </div>

        <div className="flex items-center justify-between mb-6">
          <h2 className="text-2xl font-bold text-gray-800">전체 공연</h2>
          <span className="text-sm text-gray-400">{concerts.length}개의 공연</span>
        </div>

        <div className="flex flex-col md:flex-row gap-3 mb-6">
          <div className="relative flex-1">
            <Search size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input
              type="text"
              placeholder="콘서트 이름으로 검색"
              value={keyword}
              onChange={handleKeywordChange}
              className="w-full pl-10 p-3 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400"
            />
          </div>
          <select
            value={sort}
            onChange={handleSortChange}
            className="p-3 border border-gray-200 rounded-lg bg-white text-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-400"
          >
            <option value="closingSoon">마감 임박순</option>
            <option value="latest">최신순</option>
          </select>
        </div>

        {loading ? (
          <p className="text-center text-gray-400 py-20">불러오는 중...</p>
        ) : error ? (
          <p className="text-center text-red-400 py-20">{error}</p>
        ) : concerts.length === 0 ? (
          <p className="text-center text-gray-400 py-20">검색 결과가 없습니다.</p>
        ) : (
          <>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
              {pagedConcerts.map((concert) => (
                <Link
                  href={`/concerts/${concert.concertId}`}
                  key={concert.concertId}
                  className="bg-white rounded-2xl shadow-sm overflow-hidden hover:shadow-lg hover:-translate-y-1 transition-all duration-200 cursor-pointer"
                >
                  <div className="h-48 bg-gradient-to-br from-blue-200 to-indigo-300 flex items-center justify-center text-white font-bold relative overflow-hidden">
                    {concert.imageUrl ? (
                      <img src={concert.imageUrl} alt={concert.concertName} className="w-full h-full object-cover" />
                    ) : (
                      "포스터"
                    )}
                    {concert.status === "CLOSED" && (
                      <span className="absolute top-2 left-2 bg-gray-600 text-white text-xs px-2 py-1 rounded-full">
                        마감
                      </span>
                    )}
                  </div>
                  <div className="p-4">
                    <h3 className="font-bold text-gray-800 truncate">{concert.concertName}</h3>
                    <p className="text-sm text-gray-500 mt-1">{concert.venueName}</p>
                    <p className="text-sm text-gray-400 mt-1">
                      {concert.startDate?.slice(0, 10)}
                    </p>
                  </div>
                </Link>
              ))}
            </div>

            {totalPages > 1 && (
              <div className="flex items-center justify-center gap-2 mt-10">
                <button
                  type="button"
                  onClick={() => setCurrentPage((page) => Math.max(1, page - 1))}
                  disabled={currentPage === 1}
                  className="px-3 py-2 rounded-lg border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-default"
                >
                  이전
                </button>
                {Array.from({ length: totalPages }, (_, index) => index + 1).map((page) => (
                  <button
                    type="button"
                    key={page}
                    onClick={() => setCurrentPage(page)}
                    className={`w-10 h-10 rounded-lg border text-sm font-semibold ${
                      currentPage === page
                        ? "bg-blue-600 border-blue-600 text-white"
                        : "bg-white border-gray-200 text-gray-600 hover:bg-gray-50"
                    }`}
                  >
                    {page}
                  </button>
                ))}
                <button
                  type="button"
                  onClick={() => setCurrentPage((page) => Math.min(totalPages, page + 1))}
                  disabled={currentPage === totalPages}
                  className="px-3 py-2 rounded-lg border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-default"
                >
                  다음
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}