"use client";

import { useState, useEffect, use } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";

interface SeatDetail {
  seatNumber: string;
  seatStatus: "AVAILABLE" | "HOLD" | "SOLD_OUT";
  gradeName: string;
}

interface SeatSelectionData {
  concertId: number;
  scheduleId: number;
  prices: Record<string, number>;
  seats: SeatDetail[];
}

const GRADE_STYLES: Record<string, { seat: string; dot: string }> = {
  VIP: { seat: "bg-yellow-300 hover:bg-yellow-400 text-yellow-900", dot: "bg-yellow-300" },
  R: { seat: "bg-blue-300 hover:bg-blue-400 text-blue-900", dot: "bg-blue-300" },
  S: { seat: "bg-green-300 hover:bg-green-400 text-green-900", dot: "bg-green-300" },
};
const DEFAULT_STYLE = { seat: "bg-gray-200 hover:bg-gray-300 text-gray-700", dot: "bg-gray-300" };

export default function SeatSelectPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const searchParams = useSearchParams();
  const scheduleId = searchParams.get("scheduleId");
  const router = useRouter();

  const [seatData, setSeatData] = useState<SeatSelectionData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedSeats, setSelectedSeats] = useState<string[]>([]);
  const [isReserving, setIsReserving] = useState(false);
  const [timeLeft, setTimeLeft] = useState(300);

  useEffect(() => {
    const timer = setInterval(() => {
      setTimeLeft((prev) => (prev <= 1 ? 0 : prev - 1));
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  const formatTime = (seconds: number) => {
    const min = Math.floor(seconds / 60);
    const sec = seconds % 60;
    return `${min}:${sec.toString().padStart(2, "0")}`;
  };

  useEffect(() => {
    if (!scheduleId) {
      setError("회차 정보가 없습니다.");
      setLoading(false);
      return;
    }

    let active = true;

    const fetchSeats = async () => {
      try {
        const res = await apiFetch<SeatSelectionData>(
          `/concerts/${id}/schedules/${scheduleId}/seats`
        );
        if (active) {
          setSeatData(res.data);
          setError("");
        }
      } catch (e) {
        if (active) {
          setError(e instanceof Error ? e.message : "좌석 정보를 불러오지 못했습니다.");
        }
      } finally {
        if (active) setLoading(false);
      }
    };

    fetchSeats();
    const intervalId = setInterval(fetchSeats, 3000);

    return () => {
      active = false;
      clearInterval(intervalId);
    };
  }, [id, scheduleId]);

  const seatStatusMap = new Map(seatData?.seats.map((s) => [s.seatNumber, s.seatStatus]) ?? []);
  const seatGradeMap = new Map(seatData?.seats.map((s) => [s.seatNumber, s.gradeName]) ?? []);

  const rows = Array.from(
    new Set(seatData?.seats.map((s) => s.seatNumber.split("-")[0]) ?? [])
  ).sort();

  const seatsByRow = (row: string) =>
    (seatData?.seats ?? [])
      .filter((s) => s.seatNumber.startsWith(`${row}-`))
      .sort((a, b) => parseInt(a.seatNumber.split("-")[1]) - parseInt(b.seatNumber.split("-")[1]));

  const handleSeatClick = (seatNumber: string) => {
    const status = seatStatusMap.get(seatNumber);
    if (status !== "AVAILABLE") return;

    if (selectedSeats.includes(seatNumber)) {
      setSelectedSeats([]); // 이미 선택된 좌석 다시 누르면 선택 해제
    } else {
      setSelectedSeats([seatNumber]); // 무조건 이 좌석 하나로 교체
    }
  };

  const totalPrice = selectedSeats.reduce((sum, seatNumber) => {
    const grade = seatGradeMap.get(seatNumber);
    const price = grade ? seatData?.prices[grade] ?? 0 : 0;
    return sum + price;
  }, 0);

  const handleProceedToPayment = async () => {
    const seatNumber = selectedSeats[0];
    setIsReserving(true);
    try {
      const res = await apiFetch<{ occupyToken: string; expireInSeconds: number }>(
        `/concerts/${id}/schedules/${scheduleId}/seats/occupy`,
        { method: "POST", body: JSON.stringify({ seatNumber }) }
      );

      const grade = seatGradeMap.get(seatNumber);
      const price = grade ? seatData?.prices[grade] ?? 0 : 0;

      const params = new URLSearchParams({
        concertId: id,
        scheduleId: scheduleId ?? "",
        seatNumber,
        occupyToken: res.data.occupyToken,
        price: String(price),
      });
      router.push(`/payment?${params.toString()}`);
    } catch (e) {
      alert(e instanceof Error ? e.message : "좌석 선점에 실패했습니다.");
    } finally {
      setIsReserving(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <p className="text-gray-400">불러오는 중...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <p className="text-red-400">{error}</p>
      </div>
    );
  }

  const gradeEntries = Object.entries(seatData?.prices ?? {});

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="max-w-6xl mx-auto bg-white rounded-2xl shadow-sm p-8">
        <div className="flex items-center justify-between mb-8">
          <h1 className="text-lg font-bold text-gray-800">좌석 선택</h1>
          <div className="text-red-500 font-bold">{formatTime(timeLeft)}</div>
        </div>

        <div className="flex flex-col lg:flex-row gap-8">
          {/* 좌석 배치도 */}
          <div className="flex-1">
            <div className="bg-gray-300 text-gray-600 text-center py-3 rounded-lg mb-8 font-bold tracking-widest">
              STAGE
            </div>

            <div className="space-y-3 overflow-x-auto pb-4">
              {rows.map((row) => (
                <div key={row} className="flex items-center gap-3 justify-center min-w-max">
                  <span className="w-6 font-bold text-gray-400 text-sm">{row}</span>
                  <div className="flex gap-2">
                    {seatsByRow(row).map((seat) => {
                      const isSelected = selectedSeats.includes(seat.seatNumber);
                      const isTaken = seat.seatStatus !== "AVAILABLE";
                      const col = seat.seatNumber.split("-")[1];
                      const style = GRADE_STYLES[seat.gradeName] ?? DEFAULT_STYLE;

                      let seatClass = "";
                      if (isTaken) {
                        seatClass = "bg-white border-2 border-gray-200 text-gray-300 cursor-not-allowed";
                      } else if (isSelected) {
                        seatClass = "bg-purple-500 hover:bg-purple-600 text-white cursor-pointer";
                      } else {
                        seatClass = `${style.seat} cursor-pointer`;
                      }

                      return (
                        <button
                          key={seat.seatNumber}
                          onClick={() => handleSeatClick(seat.seatNumber)}
                          disabled={isTaken}
                          className={`w-8 h-8 rounded-full flex items-center justify-center text-[10px] font-semibold transition ${seatClass}`}
                        >
                          {col}
                        </button>
                      );
                    })}
                  </div>
                </div>
              ))}
            </div>

            <div className="flex gap-6 justify-center mt-8 text-sm text-gray-500 flex-wrap">
              <div className="flex items-center gap-2">
                <div className="w-5 h-5 rounded-full bg-purple-500"></div> 선택됨
              </div>
              <div className="flex items-center gap-2">
                <div className="w-5 h-5 rounded-full bg-white border-2 border-gray-200"></div> 예매완료
              </div>
            </div>
          </div>

          {/* 사이드 패널 */}
          <div className="w-full lg:w-80 flex-shrink-0 space-y-6">
            <div>
              <div className="flex items-center justify-between mb-3">
                <h2 className="font-bold text-gray-700">선택 좌석 {selectedSeats.length}</h2>
                {selectedSeats.length > 0 && (
                  <button
                    onClick={() => setSelectedSeats([])}
                    className="text-xs text-gray-400 hover:text-red-500"
                  >
                    전체삭제
                  </button>
                )}
              </div>

              {selectedSeats.length === 0 ? (
                <p className="text-gray-400 text-sm">좌석을 선택해주세요.</p>
              ) : (
                <div className="space-y-2">
                  {selectedSeats.map((seatNumber) => {
                    const grade = seatGradeMap.get(seatNumber) ?? "";
                    const price = seatData?.prices[grade] ?? 0;
                    return (
                      <div
                        key={seatNumber}
                        className="flex items-center justify-between bg-gray-50 rounded-lg px-4 py-3"
                      >
                        <div>
                          <p className="font-semibold text-gray-700 text-sm">
                            {grade} · {seatNumber}
                          </p>
                          <p className="text-xs text-gray-400">{price.toLocaleString()}원</p>
                        </div>
                        <button
                          onClick={() => handleSeatClick(seatNumber)}
                          className="text-gray-400 hover:text-red-500 text-lg leading-none"
                        >
                          ×
                        </button>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            <div className="border border-gray-200 rounded-xl p-4">
              <h3 className="font-bold text-gray-700 text-sm mb-3">등급별 가격</h3>
              <div className="space-y-2">
                {gradeEntries.map(([grade, price]) => {
                  const style = GRADE_STYLES[grade] ?? DEFAULT_STYLE;
                  return (
                    <div key={grade} className="flex items-center gap-2 text-sm text-gray-600">
                      <div className={`w-3 h-3 rounded-full ${style.dot}`}></div>
                      {grade} : {price.toLocaleString()}
                    </div>
                  );
                })}
              </div>
            </div>

            <div className="flex justify-between items-center border-t pt-4">
              <span className="text-gray-600 text-sm">총 결제 금액</span>
              <span className="text-xl font-bold text-blue-600">
                {totalPrice.toLocaleString()}원
              </span>
            </div>

            <button
              onClick={handleProceedToPayment}
              disabled={selectedSeats.length === 0 || isReserving}
              className="w-full p-3 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-bold transition disabled:opacity-40 disabled:cursor-not-allowed"
            >
              {isReserving ? "선점 중..." : "선택 완료"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}