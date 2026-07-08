"use client";

import { Suspense, useState, useEffect, useRef, type ChangeEvent } from "react";
import Link from "next/link";
import { useRouter, usePathname, useSearchParams } from "next/navigation";
import { Search, ChevronLeft, ChevronRight } from "lucide-react";
import { apiFetch, setAccessToken } from "@/lib/api";
import { Swiper, SwiperSlide } from "swiper/react";
import { Autoplay } from "swiper/modules";
import type { Swiper as SwiperInstance } from "swiper";
import "swiper/css";

interface ConcertListItem {
  concertId: number;
  concertName: string;
  venueName: string;
  startDate: string;
  endDate: string;
  imageUrl: string;
  status: string;
}

// 상세 조회 API에만 있는 "공연 소개"/"장소 주소"까지 합쳐서 홈 화면 상단 배너에 쓸 타입
interface TopConcertItem extends ConcertListItem {
  description: string;
  location: string;
}

interface ConcertDetailResponse {
  concertId: number;
  concertName: string;
  description: string;
  venueName: string;
  location: string;
  urlPoster: string;
}

function HomeContent() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const listSectionRef = useRef<HTMLDivElement>(null);

  const [concerts, setConcerts] = useState<ConcertListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [topConcerts, setTopConcerts] = useState<TopConcertItem[]>([]);
  const [topConcertsLoading, setTopConcertsLoading] = useState(true);
  const swiperRef = useRef<SwiperInstance | null>(null);
  const [activeTopIndex, setActiveTopIndex] = useState(0);
  const [autoplayProgress, setAutoplayProgress] = useState(0);
  // 지금 배너에 있는 포스터들 중 "가장 작은(세로로 좁은) 비율"을 담아둔다.
  // 이 비율을 기준으로 왼쪽 포스터 박스 폭을 정해서, 그 포스터가 여백 없이 꽉 차게 만든다.
  const [posterAspectRatio, setPosterAspectRatio] = useState(3 / 4);

  const [keyword, setKeyword] = useState("");
  const [sort, setSort] = useState("latest");

  const [currentPage, setCurrentPage] = useState(() => {
    const page = Number(searchParams.get("page"));
    return page > 0 ? page : 1;
  });

  useEffect(() => {
    const page = Number(searchParams.get("page"));
    // 주소창의 ?page= 값이 바뀔 때마다(뒤로가기 포함) 화면 페이지 번호를 맞춰주는 로직이라
    // effect 안에서 setState를 쓰는 게 맞는 경우다.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setCurrentPage(page > 0 ? page : 1);
  }, [searchParams]);

  // 소셜 로그인 성공 시 서버가 "/#accessToken=..." 형태로 우리를 돌려보낸다.
  // 주소창의 # 뒤에 실려온 토큰을 꺼내서 저장하고, 주소를 원래 모습으로 되돌린다.
  useEffect(() => {
    if (typeof window === "undefined") return;

    const hash = window.location.hash; // 예: "#accessToken=eyJhbGciOi..."
    if (!hash.startsWith("#accessToken=")) return;

    const token = decodeURIComponent(hash.slice("#accessToken=".length));
    setAccessToken(token);

    // 주소창에서 토큰 흔적을 지운다 (새로고침해도 다시 로그인되도록 두면 안 되니까)
    window.history.replaceState(null, "", window.location.pathname + window.location.search);
  }, []);

  const itemsPerPage = 12;

  const goToPage = (page: number) => {
    setCurrentPage(page);
    const params = new URLSearchParams(searchParams.toString());
    params.set("page", String(page));
    router.push(`${pathname}?${params.toString()}`, { scroll: false });
    requestAnimationFrame(() => {
      listSectionRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
    });
  };

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

  useEffect(() => {
    const fetchTopConcerts = async () => {
      try {
        const res = await apiFetch<ConcertListItem[]>(`/concerts?sort=closingSoon`);
        // 이미 마감된 콘서트는 배너에서 제외하고, 그다음으로 마감이 임박한 콘서트로 채운다.
        const top5 = res.data.filter((c) => c.status !== "CLOSED").slice(0, 5);

        // 목록 조회 API엔 "공연 소개"/"장소 주소"가 없어서, 콘서트 5개에 한해서만
        // 상세 조회 API를 추가로 호출해 필요한 정보를 채워 넣는다.
        const detailed = await Promise.all(
          top5.map(async (concert) => {
            try {
              const detailRes = await apiFetch<ConcertDetailResponse>(`/concerts/${concert.concertId}`);
              return {
                ...concert,
                description: detailRes.data.description,
                location: detailRes.data.location,
              };
            } catch {
              return { ...concert, description: "", location: "" };
            }
          }),
        );

        setTopConcerts(detailed);

        // 포스터 5개의 실제 가로/세로 비율을 브라우저에서 직접 읽어서,
        // 그중 가장 작은(세로로 좁은) 비율을 찾아 왼쪽 포스터 박스 폭 계산에 쓴다.
        const ratios = await Promise.all(
          detailed.map(
            (concert) =>
              new Promise<number | null>((resolve) => {
                if (!concert.imageUrl) {
                  resolve(null);
                  return;
                }
                const img = new window.Image();
                img.onload = () => resolve(img.naturalWidth / img.naturalHeight);
                img.onerror = () => resolve(null);
                img.src = concert.imageUrl;
              }),
          ),
        );
        const validRatios = ratios.filter((r): r is number => r !== null && r > 0);
        if (validRatios.length > 0) {
          setPosterAspectRatio(Math.min(...validRatios));
        }
      } catch {
        setTopConcerts([]);
      } finally {
        setTopConcertsLoading(false);
      }
    };

    fetchTopConcerts();
  }, []);

  const totalPages = Math.ceil(concerts.length / itemsPerPage);
  const pagedConcerts = concerts.slice(
    (currentPage - 1) * itemsPerPage,
    currentPage * itemsPerPage
  );

  const handleKeywordChange = (e: ChangeEvent<HTMLInputElement>) => {
    setKeyword(e.target.value);
    goToPage(1);
  };

  const handleSortChange = (e: ChangeEvent<HTMLSelectElement>) => {
    setSort(e.target.value);
    goToPage(1);
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-5xl mx-auto px-6 pt-8">
        <div className="group relative bg-gray-900 rounded-2xl overflow-hidden">
          <button
            onClick={() => swiperRef.current?.slidePrev()}
            className="absolute left-0 top-0 bottom-0 z-20 w-16 flex items-center justify-center bg-gradient-to-r from-black/50 to-transparent text-white opacity-0 group-hover:opacity-100 transition-opacity"
          >
            <ChevronLeft size={28} />
          </button>

          <button
            onClick={() => swiperRef.current?.slideNext()}
            className="absolute right-0 top-0 bottom-0 z-20 w-16 flex items-center justify-center bg-gradient-to-l from-black/50 to-transparent text-white opacity-0 group-hover:opacity-100 transition-opacity"
          >
            <ChevronRight size={28} />
          </button>

          {topConcertsLoading ? (
            <div className="h-[28rem] flex items-center justify-center text-gray-400">
              불러오는 중...
            </div>
          ) : (
            <Swiper
              modules={[Autoplay]}
              onSwiper={(swiper) => {
                swiperRef.current = swiper;
              }}
              onSlideChange={(swiper) => setActiveTopIndex(swiper.realIndex)}
              onAutoplayTimeLeft={(_swiper, _timeLeft, percentage) => setAutoplayProgress(1 - percentage)}
              slidesPerView={1}
              loop={topConcerts.length > 1}
              autoplay={{
                delay: 3000,
                disableOnInteraction: false,
                pauseOnMouseEnter: true,
              }}
            >
              {topConcerts.map((concert) => (
                <SwiperSlide key={concert.concertId}>
                  <Link href={`/concerts/${concert.concertId}`} className="relative flex h-[28rem] overflow-hidden bg-gray-900">
                    {/* 배너 전체에 깔리는 흐린 포스터 배경 (왼쪽/오른쪽이 하나로 이어져 보이도록) */}
                    {concert.imageUrl && (
                      <img
                        src={concert.imageUrl}
                        alt=""
                        aria-hidden="true"
                        className="absolute inset-0 w-full h-full object-cover scale-110 blur-3xl opacity-60"
                      />
                    )}
                    {/* 글자 가독성을 위해 어둡게 한 겹 덮는다 */}
                    <div className="absolute inset-0 bg-gray-900/50" />

                    {/* 왼쪽: 포스터 (크게, 잘리지 않도록) */}
                    <div
                      className="relative flex-shrink-0 overflow-hidden"
                      style={{ width: `${448 * posterAspectRatio}px`, minWidth: 220 }}
                    >
                      {concert.imageUrl ? (
                        <img
                          src={concert.imageUrl}
                          alt={concert.concertName}
                          className="absolute inset-0 w-full h-full object-contain"
                        />
                      ) : (
                        <div className="absolute inset-0 bg-gradient-to-br from-blue-500 to-indigo-600" />
                      )}
                    </div>

                    {/* 오른쪽: 제목/장소/소개/버튼 */}
                    <div className="relative flex-1 min-w-0 text-white p-10 flex flex-col justify-center">
                      {concert.status === "CLOSED" && (
                        <span className="self-start mb-3 bg-red-500 text-white text-xs px-2 py-1 rounded-full font-semibold">
                          마감
                        </span>
                      )}
                      <h3 className="text-3xl md:text-4xl font-bold drop-shadow-md">{concert.concertName}</h3>

                      <p className="text-sm text-white font-medium mt-4 drop-shadow-md">
                        📍 {concert.venueName}
                        {concert.location && <span className="text-gray-100"> · {concert.location}</span>}
                      </p>
                      <p className="text-sm text-gray-100 font-medium mt-1 drop-shadow-md">
                        {concert.startDate?.slice(0, 10)} ~ {concert.endDate?.slice(0, 10)}
                      </p>

                      {concert.description && (
                        <p className="text-sm text-white mt-5 leading-6 line-clamp-3 max-w-xl drop-shadow-md">
                          {concert.description}
                        </p>
                      )}

                      <span className="inline-flex items-center gap-1 mt-6 text-sm font-semibold bg-white text-gray-900 w-fit px-4 py-2 rounded-full hover:bg-gray-100 transition">
                        자세히 보기 →
                      </span>
                    </div>
                  </Link>
                </SwiperSlide>
              ))}
            </Swiper>
          )}

          {/* 캐러셀 틀에 고정된 진행 표시 (슬라이드와 같이 움직이지 않도록 Swiper 바깥에 둔다) */}
          {!topConcertsLoading && topConcerts.length > 0 && (
            <div className="absolute bottom-4 left-8 right-8 flex gap-2 z-20 pointer-events-none">
              {topConcerts.map((_, segmentIndex) => (
                <div key={segmentIndex} className="flex-1 h-1 rounded-full bg-white/25 overflow-hidden">
                  <div
                    className="h-full bg-white rounded-full"
                    style={{
                      width:
                        segmentIndex < activeTopIndex
                          ? "100%"
                          : segmentIndex === activeTopIndex
                            ? `${autoplayProgress * 100}%`
                            : "0%",
                    }}
                  />
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      <div className="max-w-5xl mx-auto px-6 py-10">
        <div ref={listSectionRef} className="flex items-center justify-between mb-6 scroll-mt-6">
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
                  className="bg-white rounded-2xl shadow-sm overflow-hidden hover:shadow-lg hover:-translate-y-1 transition-all duration-200 cursor-pointer flex flex-col"
                >
                  <div className="h-48 bg-gradient-to-br from-blue-200 to-indigo-300 flex items-center justify-center text-white font-bold relative overflow-hidden">
                    {concert.imageUrl ? (
                      <img src={concert.imageUrl} alt={concert.concertName} className="w-full h-full object-cover" />
                    ) : (
                      "포스터"
                    )}
                    {concert.status === "CLOSED" && (
                      <span className="absolute top-2 left-2 bg-red-500 text-white text-xs px-2 py-1 rounded-full font-semibold">
                        마감
                      </span>
                    )}
                  </div>
                  <div className="p-4 flex flex-col flex-1">
                    <h3 className="font-bold text-gray-800 truncate">{concert.concertName}</h3>
                    <p className="text-sm text-gray-500 mt-1 line-clamp-1">{concert.venueName}</p>
                    <p className="text-sm text-gray-400 mt-auto pt-1">
                      {concert.startDate?.slice(0, 10)} ~ {concert.endDate?.slice(0, 10)}
                    </p>
                  </div>
                </Link>
              ))}
            </div>

            {totalPages > 1 && (
              <div className="flex items-center justify-center gap-2 mt-10">
                <button
                  type="button"
                  onClick={() => goToPage(Math.max(1, currentPage - 1))}
                  disabled={currentPage === 1}
                  className="px-3 py-2 rounded-lg border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-default"
                >
                  이전
                </button>
                {Array.from({ length: totalPages }, (_, index) => index + 1).map((page) => (
                  <button
                    type="button"
                    key={page}
                    onClick={() => goToPage(page)}
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
                  onClick={() => goToPage(Math.min(totalPages, currentPage + 1))}
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

export default function Home() {
  return (
    <Suspense fallback={<div>로딩 중...</div>}>
      <HomeContent />
    </Suspense>
  );
}