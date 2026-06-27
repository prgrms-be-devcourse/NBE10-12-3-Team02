"use client";

import { useState, use } from "react";
import Link from "next/link";

export default function ConcertDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);

  // 선택한 회차를 기억하는 상태
  const [selectedSchedule, setSelectedSchedule] = useState<number | null>(null);

  const concert = {
    concertId: id,
    concertName: "2026 SUMMER LIVE",
    description: "올여름 최고의 페스티벌! 뜨거운 여름밤을 함께해요.",
    venueName: "올림픽 체조경기장",
    location: "서울시 송파구",
    prices: { VIP: 150000, R: 120000, S: 90000, A: 70000 },
  };

  // 가짜 회차 데이터
  const schedules = [
    { scheduleId: 101, dateTime: "2026-08-10 19:00", round: 1 },
    { scheduleId: 102, dateTime: "2026-08-11 19:00", round: 2 },
    { scheduleId: 103, dateTime: "2026-08-12 18:00", round: 3 },
  ];

  return (
    <div className="min-h-screen bg-gray-50 p-10">
      <div className="max-w-4xl mx-auto">
        <div className="bg-white rounded-2xl shadow-sm overflow-hidden">
          <div className="flex flex-col md:flex-row">
            <div className="md:w-1/3 h-80 bg-gradient-to-br from-blue-200 to-indigo-300 flex items-center justify-center text-white font-bold text-xl">
              포스터
            </div>

            <div className="p-8 flex-1">
              <h1 className="text-2xl font-bold text-gray-800 mb-2">
                {concert.concertName}
              </h1>
              <p className="text-gray-500 mb-1">📍 {concert.venueName}</p>
              <p className="text-gray-400 text-sm mb-4">{concert.location}</p>
              <p className="text-gray-600 mb-6">{concert.description}</p>

              <div className="border-t pt-4 mb-6">
                <h2 className="font-bold text-gray-700 mb-2">좌석 등급별 가격</h2>
                <div className="space-y-1 text-sm text-gray-600">
                  <p>VIP석 — {concert.prices.VIP.toLocaleString()}원</p>
                  <p>R석 — {concert.prices.R.toLocaleString()}원</p>
                  <p>S석 — {concert.prices.S.toLocaleString()}원</p>
                  <p>A석 — {concert.prices.A.toLocaleString()}원</p>
                </div>
              </div>

              {/* 회차 선택 */}
              <div className="border-t pt-4 mb-6">
                <h2 className="font-bold text-gray-700 mb-3">회차 선택</h2>
                <div className="flex flex-wrap gap-2">
                  {schedules.map((schedule) => (
                    <button
                      key={schedule.scheduleId}
                      onClick={() => setSelectedSchedule(schedule.scheduleId)}
                      className={`px-4 py-2 rounded-lg text-sm font-semibold border transition
                        ${
                          selectedSchedule === schedule.scheduleId
                            ? "bg-blue-600 text-white border-blue-600"
                            : "bg-white text-gray-600 border-gray-200 hover:border-blue-400"
                        }`}
                    >
                      {schedule.round}회차
                      <br />
                      <span className="text-xs font-normal">
                        {schedule.dateTime}
                      </span>
                    </button>
                  ))}
                </div>
              </div>

              {/* 예매하기 버튼 */}
              {selectedSchedule ? (
                <Link
                  href={`/concerts/${id}/seats`}
                  className="block w-full p-3 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-bold text-center transition"
                >
                  예매하기
                </Link>
              ) : (
                <button
                  disabled
                  className="w-full p-3 bg-gray-300 text-gray-500 rounded-lg font-bold cursor-not-allowed"
                >
                  회차를 선택해주세요
                </button>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}