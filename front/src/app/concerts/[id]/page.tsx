"use client";

import { useState, useEffect, use } from "react";
import { useRouter } from "next/navigation";
import { apiFetch, decodeToken } from "@/lib/api";
import { showAlert } from "@/lib/alert";
import { getConcertDetailImages, getLocalConcertPoster } from "@/lib/concertDetailImages";

interface ConcertDetail {
  concertId: number;
  concertName: string;
  description: string;
  venueName: string;
  location: string;
  urlPoster: string;
  detailUrlList: string[];
  prices: Record<string, number>;
  bookable: boolean;
}

interface ScheduleItem {
  scheduleId: number;
  round: number;
  scheduleDate: string;
  remainingSeats: number;
}

const GRADE_ORDER = ["VIP", "R", "S", "A"];

const stripVenuePrefix = (name: string) => name.replace(/^\(.*?\)\s*/, "").trim();

export default function ConcertDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const router = useRouter();

  const [concert, setConcert] = useState<ConcertDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [schedules, setSchedules] = useState<ScheduleItem[]>([]);
  const [selectedSchedule, setSelectedSchedule] = useState<number | null>(null);

  useEffect(() => {
    const fetchConcert = async () => {
      try {
        setLoading(true);
        setError("");
        const res = await apiFetch<ConcertDetail>(`/concerts/${id}`);
        setConcert(res.data);
      } catch (e) {
        setError(e instanceof Error ? e.message : "콘서트 정보를 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    };
    fetchConcert();
  }, [id]);

  useEffect(() => {
    apiFetch<ScheduleItem[]>(`/schedules?concertId=${id}`)
      .then((res) => setSchedules(res.data))
      .catch(() => setSchedules([]));
  }, [id]);

  const handleBookingClick = async () => {
    if (!decodeToken()) {
      await showAlert("로그인이 필요합니다.");
      router.replace("/login");
      return;
    }
    router.push(`/concerts/${id}/seats?scheduleId=${selectedSchedule}`);
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <p className="text-gray-400">불러오는 중...</p>
      </div>
    );
  }

  if (error || !concert) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <p className="text-red-400">{error || "콘서트를 찾을 수 없습니다."}</p>
      </div>
    );
  }

  const posterUrl = getLocalConcertPoster(concert.urlPoster);
  const detailImages = getConcertDetailImages(concert.urlPoster);
  const mapQuery = encodeURIComponent(`${stripVenuePrefix(concert.venueName)} ${concert.location}`);
  const mapUrl = `https://map.kakao.com/link/search/${mapQuery}`;

  return (
    <div className="min-h-screen bg-gray-50 p-10">
      <div className="max-w-4xl mx-auto">
        <div className="bg-white rounded-2xl shadow-sm overflow-hidden">
          <div className="flex flex-col md:flex-row">
            <div className="md:w-1/3 flex flex-col">
              <div className="self-start bg-gradient-to-br from-blue-200 to-indigo-300 flex items-center justify-center text-white font-bold text-xl overflow-hidden">
                {posterUrl ? (
                  <img
                    src={posterUrl}
                    alt={concert.concertName}
                    className="w-full h-auto object-cover"
                  />
                ) : (
                  <div className="w-full aspect-[3/4] flex items-center justify-center">
                    포스터
                  </div>
                )}
              </div>
              <p className="text-sm text-gray-500 mt-3 text-left">예매 가능 시간: 관람일 전일 17시까지</p>
              <p className="text-sm text-gray-500 mt-1 text-left">회차당 최대 3매까지 예매 가능합니다.</p>
            </div>

            <div className="p-8 flex-1">
              <h1 className="text-2xl font-bold text-gray-800 mb-4">{concert.concertName}</h1>

              <div className="mb-4">
                <h2 className="font-bold text-gray-700 mb-2">공연 장소</h2>

                <a href={mapUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="block group"
                >
                  <p className="text-gray-600 text-sm group-hover:text-blue-600 transition">
                    📍 {concert.venueName}
                    <span className="ml-1 text-xs text-blue-500 underline">지도 보기</span>
                  </p>
                  <p className="text-gray-400 text-sm mt-1">{concert.location}</p>
                </a>
              </div>

              <div className="mb-6">
                <h2 className="font-bold text-gray-700 mb-2">공연 소개</h2>
                <p className="text-gray-600 text-sm leading-6">{concert.description}</p>
              </div>

              <div className="mb-6">
                <h2 className="font-bold text-gray-700 mb-2">좌석 등급별 가격</h2>
                <div className="space-y-1 text-sm text-gray-600">
                  {Object.entries(concert.prices)
                    .sort(([a], [b]) => GRADE_ORDER.indexOf(a) - GRADE_ORDER.indexOf(b))
                    .map(([grade, price]) => (
                      <p key={grade}>
                        {grade}석 — {price.toLocaleString()}원
                      </p>
                    ))}
                </div>
              </div>

              <div className="mb-6">
                <h2 className="font-bold text-gray-700 mb-3">회차 선택</h2>
                {schedules.length === 0 ? (
                  <p className="text-sm text-gray-400">등록된 회차가 없습니다.</p>
                ) : (
                  <div className="flex flex-wrap gap-2">
                    {schedules.map((schedule) => (
                      <button
                        key={schedule.scheduleId}
                        onClick={() => setSelectedSchedule(schedule.scheduleId)}
                        className={`px-4 py-2 rounded-lg text-sm font-semibold border transition ${selectedSchedule === schedule.scheduleId
                            ? "bg-blue-600 text-white border-blue-600"
                            : "bg-white text-gray-600 border-gray-200 hover:border-blue-400"
                          }`}
                      >
                        {schedule.round}회차
                        <br />
                        <span className="text-xs font-normal">
                          {schedule.scheduleDate?.slice(0, 16).replace("T", " ")}
                        </span>
                        <br />
                        <span
                          className={`text-xs font-normal ${schedule.remainingSeats === 0 ? "text-red-500" : "text-gray-400"
                            }`}
                        >
                          {schedule.remainingSeats === 0 ? "매진" : `잔여 ${schedule.remainingSeats}석`}
                        </span>
                      </button>
                    ))}
                  </div>
                )}
              </div>

              {(() => {
                const selectedScheduleData = schedules.find(
                  (s) => s.scheduleId === selectedSchedule
                );
                // 회차 자체는 살아있어도(콘서트 전체 기간 안이어도), 그 회차의 공연 날짜가
                // 이미 지났으면 예매할 수 없다 — 콘서트 전체 bookable 여부와는 별개로 확인한다.
                const isSelectedSchedulePast =
                  !!selectedScheduleData &&
                  new Date(selectedScheduleData.scheduleDate) < new Date();

                if (selectedSchedule && concert.bookable && !isSelectedSchedulePast) {
                  return (
                    <button
                      onClick={handleBookingClick}
                      className="block w-full p-3 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-bold text-center transition"
                    >
                      예매하기
                    </button>
                  );
                }

                const message = !selectedSchedule
                  ? "회차를 선택해주세요"
                  : "예매 불가능한 공연입니다";

                return (
                  <button
                    disabled
                    className="w-full p-3 bg-gray-300 text-gray-500 rounded-lg font-bold cursor-not-allowed"
                  >
                    {message}
                  </button>
                );
              })()}
            </div>
          </div>
        </div>

        <section className="mt-10 bg-white rounded-2xl shadow-sm overflow-hidden">
          <div className="p-8">
            <h2 className="text-xl font-bold text-gray-800 mb-6">상세 설명</h2>

            {detailImages.length > 0 ? (
              detailImages.map((url) => (
                <div key={url} className="w-full max-w-3xl mx-auto mb-4">
                  <img src={url} alt="공연 상세 설명" className="w-full h-auto rounded-xl border border-gray-200" />
                </div>
              ))
            ) : (
              <p className="text-sm text-gray-400">등록된 상세 이미지가 없습니다.</p>
            )}
          </div>
        </section>
      </div>
    </div>
  );
}