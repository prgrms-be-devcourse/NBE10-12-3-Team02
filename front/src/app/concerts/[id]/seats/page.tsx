"use client";

import { apiFetch, ApiError, decodeToken, restoreSession } from "@/lib/api";
import { showAlert, showError } from "@/lib/alert";
import { useRouter, useSearchParams } from "next/navigation";
import { Minus, Plus } from "lucide-react";
import { Suspense, use, useEffect, useState } from "react";

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
  VIP: {
    seat: "bg-yellow-300 hover:bg-yellow-400 text-yellow-900",
    dot: "bg-yellow-300",
  },
  R: {
    seat: "bg-blue-300 hover:bg-blue-400 text-blue-900",
    dot: "bg-blue-300",
  },
  S: {
    seat: "bg-green-300 hover:bg-green-400 text-green-900",
    dot: "bg-green-300",
  },
  A: {
    seat: "bg-orange-300 hover:bg-orange-400 text-orange-900",
    dot: "bg-orange-300",
  },
};
const DEFAULT_STYLE = {
  seat: "bg-gray-200 hover:bg-gray-300 text-gray-700",
  dot: "bg-gray-300",
};
const DIMMED_STYLE = "bg-gray-100 text-gray-300 cursor-not-allowed opacity-50";
const GRADE_ORDER = ["VIP", "R", "S", "A"];
const SELECTION_TIME_LIMIT = 300; // 5분
const MAX_HEADCOUNT = 3; // 1인당 최대 구매 가능 매수

function SeatSelectContent({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const searchParams = useSearchParams();
  const scheduleId = searchParams.get("scheduleId");
  const router = useRouter();

  const [seatData, setSeatData] = useState<SeatSelectionData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [isReserving, setIsReserving] = useState(false);
  const [timeLeft, setTimeLeft] = useState<number | null>(null);
  const [userName, setUserName] = useState<string | null>(null);

  // 인원수 선택 팝업 (좌석 페이지 진입 시 먼저 뜬다)
  const [showHeadcountModal, setShowHeadcountModal] = useState(true);
  const [adultCount, setAdultCount] = useState(1);
  const [teenCount, setTeenCount] = useState(0);
  const requiredSeatCount = adultCount + teenCount;

  // 2인 이상이면 "짝"(나란히 붙은 좌석 2개)을 먼저 채우고, 3인이면 그 뒤에 자유석 1개를 더 받는다.
  const [pairSeats, setPairSeats] = useState<[string, string] | null>(null);
  const [freeSeats, setFreeSeats] = useState<string[]>([]);
  const selectedSeats = [...(pairSeats ?? []), ...freeSeats];

  useEffect(() => {
    if (timeLeft === null) return;

    if (timeLeft <= 0) {
      // 카운트다운 타이머가 0이 됐을 때 선택 상태를 정리하는 로직이라 effect 안에서 setState가 맞다.
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setPairSeats(null);
      setFreeSeats([]);
      setTimeLeft(null);
      showAlert("좌석 선택 시간이 만료되어 선택이 취소되었습니다.");
      return;
    }

    const timer = setTimeout(() => {
      setTimeLeft((prev) => (prev !== null ? prev - 1 : null));
    }, 1000);

    return () => clearTimeout(timer);
  }, [timeLeft]);

  const formatTime = (seconds: number) => {
    const min = Math.floor(seconds / 60);
    const sec = seconds % 60;
    return `${min}:${sec.toString().padStart(2, "0")}`;
  };

  useEffect(() => {
    if (!scheduleId) {
      // 주소창에 회차 정보가 없을 때 에러 화면으로 전환하는 로직이라 effect 안에서 setState가 맞다.
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setError("회차 정보가 없습니다.");
      setLoading(false);
      return;
    }

    let active = true;
    let intervalId: NodeJS.Timeout;
    let stopPolling = false;

    const initAndFetchSeats = async () => {
      await restoreSession();

      const decoded = decodeToken();
      if (!decoded) {
        await showAlert("로그인이 필요합니다.");
        router.replace("/login");
        return;
      }
      setUserName(decoded.name);

      const fetchSeats = async () => {
        try {
          const res = await apiFetch<SeatSelectionData>(
            `/concerts/${id}/schedules/${scheduleId}/seats`,
          );
          if (active) {
            setSeatData(res.data);
            setError("");
          }
        } catch (e) {
          // 이 회차에서 이미 3매를 구매한 경우: 계속 재요청해봐야 결과가 안 바뀌니
          // 폴링을 멈추고, 안내 후 콘서트 상세 페이지로 돌려보낸다.
          if (e instanceof ApiError && e.resultCode === "400-2") {
            stopPolling = true;
            if (intervalId) clearInterval(intervalId);
            if (active) {
              await showAlert(e.message);
              router.replace(`/concerts/${id}`);
            }
            return;
          }

          if (active) {
            setError(
              e instanceof Error
                ? e.message
                : "좌석 정보를 불러오지 못했습니다.",
            );
          }
        } finally {
          if (active) setLoading(false);
        }
      };

      await fetchSeats();

      if (active && !stopPolling) {
        intervalId = setInterval(fetchSeats, 1000);
      }
    };

    initAndFetchSeats();

    return () => {
      active = false;
      if (intervalId) {
        clearInterval(intervalId);
      }
    };
  }, [id, scheduleId]);

  const seatStatusMap = new Map(
    seatData?.seats.map((s) => [s.seatNumber, s.seatStatus]) ?? [],
  );
  const seatGradeMap = new Map(
    seatData?.seats.map((s) => [s.seatNumber, s.gradeName]) ?? [],
  );

  const rows = Array.from(
    new Set(seatData?.seats.map((s) => s.seatNumber.split("-")[0]) ?? []),
  ).sort();

  const seatsByRow = (row: string) =>
    (seatData?.seats ?? [])
      .filter((s) => s.seatNumber.startsWith(`${row}-`))
      .sort(
        (a, b) =>
          parseInt(a.seatNumber.split("-")[1]) -
          parseInt(b.seatNumber.split("-")[1]),
      );

  // 같은 블록(가운데 통로 기준 왼쪽/오른쪽) 안에서만 "오른쪽 옆자리"를 찾는다.
  // 통로를 건너가야 하거나, 블록의 마지막 좌석이면 null을 돌려준다.
  const getRightNeighborSeat = (seatNumber: string): string | null => {
    const row = seatNumber.split("-")[0];
    const rowSeats = seatsByRow(row);
    const index = rowSeats.findIndex((s) => s.seatNumber === seatNumber);
    if (index === -1) return null;

    const mid = Math.ceil(rowSeats.length / 2);
    const isLeftBlock = index < mid;
    const isLastInBlock = isLeftBlock ? index === mid - 1 : index === rowSeats.length - 1;
    if (isLastInBlock) return null;

    return rowSeats[index + 1]?.seatNumber ?? null;
  };

  const isSeatPairable = (seatNumber: string): boolean => {
    if (seatStatusMap.get(seatNumber) !== "AVAILABLE") return false;
    const neighbor = getRightNeighborSeat(seatNumber);
    return !!neighbor && seatStatusMap.get(neighbor) === "AVAILABLE";
  };

  const needsPair = requiredSeatCount >= 2;
  const totalSelected = (pairSeats ? 2 : 0) + freeSeats.length;
  const isSelectionFull = totalSelected >= requiredSeatCount;

  const handleSeatClick = (seatNumber: string) => {
    // 이미 짝(페어)의 일부라면, 짝 전체를 같이 취소한다.
    if (pairSeats?.includes(seatNumber)) {
      setPairSeats(null);
      if (freeSeats.length === 0) setTimeLeft(null);
      return;
    }
    // 이미 자유석으로 선택된 좌석이라면, 그 한 자리만 취소한다.
    if (freeSeats.includes(seatNumber)) {
      const next = freeSeats.filter((s) => s !== seatNumber);
      setFreeSeats(next);
      if (next.length === 0 && !pairSeats) setTimeLeft(null);
      return;
    }

    if (seatStatusMap.get(seatNumber) !== "AVAILABLE") return;
    if (isSelectionFull) return;

    // 짝을 아직 못 채웠으면, 이 좌석 + 오른쪽 옆자리를 한 번에 선택한다.
    if (needsPair && !pairSeats) {
      const neighbor = getRightNeighborSeat(seatNumber);
      if (!neighbor || seatStatusMap.get(neighbor) !== "AVAILABLE") return;
      setPairSeats([seatNumber, neighbor]);
      setTimeLeft((prev) => prev ?? SELECTION_TIME_LIMIT);
      return;
    }

    // 짝을 다 채웠거나(혹은 1명이라 짝이 필요 없거나), 자유석 자리 — 아무 빈 좌석이나 선택.
    setFreeSeats((prev) => [...prev, seatNumber]);
    setTimeLeft((prev) => prev ?? SELECTION_TIME_LIMIT);
  };

  const totalPrice = selectedSeats.reduce((sum, seatNumber) => {
    const grade = seatGradeMap.get(seatNumber);
    const price = grade ? (seatData?.prices[grade] ?? 0) : 0;
    return sum + price;
  }, 0);

  const handleProceedToPayment = async () => {
    if (selectedSeats.length === 0 || !scheduleId) return;

    setIsReserving(true);
    const occupied: { seatNumber: string; occupyToken: string; price: number }[] = [];

    try {
      for (const seatNumber of selectedSeats) {
        const res = await apiFetch<{
          occupyToken: string;
          expireInSeconds: number;
        }>(`/concerts/${id}/schedules/${scheduleId}/seats/occupy`, {
          method: "POST",
          body: JSON.stringify({ seatNumber }),
        });
        const grade = seatGradeMap.get(seatNumber);
        const price = grade ? (seatData?.prices[grade] ?? 0) : 0;
        occupied.push({ seatNumber, occupyToken: res.data.occupyToken, price });
      }

      const seatsParam = encodeURIComponent(JSON.stringify(occupied));
      const params = new URLSearchParams({
        concertId: id,
        scheduleId,
        seats: seatsParam,
      });
      router.push(`/payment?${params.toString()}`);
    } catch (e) {
      await Promise.all(
        occupied.map(({ seatNumber }) =>
          apiFetch(`/concerts/${id}/schedules/${scheduleId}/seats/occupy`, {
            method: "DELETE",
            body: JSON.stringify({ seatNumber }),
          }).catch(() => {}),
        ),
      );
      showError(e instanceof Error ? e.message : "좌석 선점에 실패했습니다.");
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

  const gradeEntries = Object.entries(seatData?.prices ?? {}).sort(
    ([a], [b]) => GRADE_ORDER.indexOf(a) - GRADE_ORDER.indexOf(b),
  );

  const renderSeat = (seat: SeatDetail) => {
    const isPaired = pairSeats?.includes(seat.seatNumber) ?? false;
    const isFree = freeSeats.includes(seat.seatNumber);
    const isSelected = isPaired || isFree;
    const col = seat.seatNumber.split("-")[1];
    const style = GRADE_STYLES[seat.gradeName] ?? DEFAULT_STYLE;

    let seatClass = "";
    let disabled = false;

    if (seat.seatStatus === "SOLD_OUT") {
      seatClass = "bg-gray-400 text-gray-400 cursor-not-allowed";
      disabled = true;
    } else if (seat.seatStatus === "HOLD") {
      seatClass = "bg-red-400 text-red-400 cursor-not-allowed";
      disabled = true;
    } else if (isSelected) {
      seatClass = "bg-purple-500 hover:bg-purple-600 text-white cursor-pointer";
    } else if (isSelectionFull) {
      // 이미 필요한 인원수만큼 다 골랐으면, 나머지 빈 좌석은 더 못 고르게 흐리게 표시.
      seatClass = DIMMED_STYLE;
      disabled = true;
    } else if (needsPair && !pairSeats && !isSeatPairable(seat.seatNumber)) {
      // 짝을 아직 못 채웠는데, 이 좌석은 오른쪽에 붙일 자리가 없어서 짝을 만들 수 없음.
      seatClass = DIMMED_STYLE;
      disabled = true;
    } else {
      seatClass = `${style.seat} cursor-pointer`;
    }

    return (
      <button
        key={seat.seatNumber}
        onClick={() => handleSeatClick(seat.seatNumber)}
        disabled={disabled}
        className={`w-[18px] h-[18px] rounded-full flex items-center justify-center text-[7px] font-semibold transition ${seatClass}`}
      >
        {col}
      </button>
    );
  };

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div
        className={
          showHeadcountModal
            ? "max-w-[1600px] mx-auto bg-white rounded-2xl shadow-sm p-8 blur-sm pointer-events-none select-none"
            : "max-w-[1600px] mx-auto bg-white rounded-2xl shadow-sm p-8"
        }
      >
        <div className="mb-6 flex items-center justify-between">
          <h1 className="text-lg font-bold text-gray-800">좌석 선택</h1>
          {userName && (
            <span className="text-sm text-gray-400">{userName}님, 좌석을 선택해주세요</span>
          )}
        </div>

        <div className="flex flex-col lg:flex-row gap-8">
          {/* 좌석 배치도 */}
          <div className="flex-1 min-w-0">
            <div className="bg-gray-300 text-gray-600 text-center py-2 rounded-lg mb-6 font-bold tracking-widest text-sm">
              STAGE
            </div>

            <div className="space-y-1.5 overflow-x-auto pb-4">
              {rows.map((row) => {
                const seats = seatsByRow(row);
                const mid = Math.ceil(seats.length / 2);
                const leftBlock = seats.slice(0, mid);
                const rightBlock = seats.slice(mid);

                return (
                  <div
                    key={row}
                    className="flex items-center gap-2 justify-center min-w-max"
                  >
                    <span className="w-4 text-right font-bold text-gray-400 text-[10px]">
                      {row}
                    </span>
                    <div className="flex gap-1">
                      {leftBlock.map(renderSeat)}
                    </div>
                    <div className="w-4" /> {/* 중앙 통로 */}
                    <div className="flex gap-1">
                      {rightBlock.map(renderSeat)}
                    </div>
                    <span className="w-4" />{" "}
                    {/* 왼쪽 라벨과 대칭 맞추는 빈 공간 */}
                  </div>
                );
              })}
            </div>

            <div className="flex gap-6 justify-center mt-6 text-xs text-gray-500 flex-wrap">
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 rounded-full bg-purple-500"></div>{" "}
                선택됨
              </div>
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 rounded-full bg-red-400"></div> 점유중
              </div>
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 rounded-full bg-gray-400"></div>{" "}
                예매완료
              </div>
            </div>
          </div>

          {/* 사이드 패널 */}
          <div className="w-full lg:w-96 flex-shrink-0 space-y-6">
            <div>
              <div className="flex items-center justify-between mb-3">
                <h2 className="font-bold text-gray-700">
                  선택 좌석 {selectedSeats.length} / {requiredSeatCount}
                </h2>
                <div className="flex items-center gap-3">
                  {timeLeft !== null && (
                    <span className="text-red-500 text-sm font-bold">
                      {formatTime(timeLeft)}
                    </span>
                  )}
                  {selectedSeats.length > 0 && (
                    <button
                      onClick={() => {
                        setPairSeats(null);
                        setFreeSeats([]);
                        setTimeLeft(null);
                      }}
                      className="text-xs text-gray-400 hover:text-red-500"
                    >
                      전체삭제
                    </button>
                  )}
                </div>
              </div>

              {selectedSeats.length === 0 ? (
                <p className="text-gray-400 text-sm">
                  좌석을 선택해주세요. (총 {requiredSeatCount}매)
                </p>
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
                          <p className="text-xs text-gray-400">
                            {price.toLocaleString()}원
                          </p>
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
              <h3 className="font-bold text-gray-700 text-sm mb-3">
                등급별 가격
              </h3>
              <div className="space-y-2">
                {gradeEntries.map(([grade, price]) => {
                  const style = GRADE_STYLES[grade] ?? DEFAULT_STYLE;
                  return (
                    <div
                      key={grade}
                      className="flex items-center gap-2 text-sm text-gray-600"
                    >
                      <div
                        className={`w-3 h-3 rounded-full ${style.dot}`}
                      ></div>
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

      {/* 좌석 페이지 진입 시 먼저 뜨는 인원수 선택 팝업 */}
      {showHeadcountModal && (
        <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl p-8 max-w-sm w-full">
            <h2 className="text-xl font-bold text-center text-gray-800 mb-1">
              인원수를 선택해주세요
            </h2>
            <p className="text-center text-gray-400 text-sm mb-6">
              최대 {MAX_HEADCOUNT}명까지 예매할 수 있어요.
            </p>

            <div className="space-y-4 mb-6">
              <div className="flex items-center justify-between">
                <span className="font-semibold text-gray-700">성인</span>
                <div className="flex items-center gap-3">
                  <button
                    type="button"
                    onClick={() => setAdultCount((c) => Math.max(0, c - 1))}
                    disabled={adultCount <= 0}
                    className="w-8 h-8 flex items-center justify-center rounded-lg border border-gray-200 text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
                  >
                    <Minus size={16} />
                  </button>
                  <span className="w-5 text-center font-bold text-gray-800">{adultCount}</span>
                  <button
                    type="button"
                    onClick={() => setAdultCount((c) => Math.min(MAX_HEADCOUNT, c + 1))}
                    disabled={adultCount + teenCount >= MAX_HEADCOUNT}
                    className="w-8 h-8 flex items-center justify-center rounded-lg border border-gray-200 text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
                  >
                    <Plus size={16} />
                  </button>
                </div>
              </div>

              <div className="flex items-center justify-between">
                <span className="font-semibold text-gray-700">청소년</span>
                <div className="flex items-center gap-3">
                  <button
                    type="button"
                    onClick={() => setTeenCount((c) => Math.max(0, c - 1))}
                    disabled={teenCount <= 0}
                    className="w-8 h-8 flex items-center justify-center rounded-lg border border-gray-200 text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
                  >
                    <Minus size={16} />
                  </button>
                  <span className="w-5 text-center font-bold text-gray-800">{teenCount}</span>
                  <button
                    type="button"
                    onClick={() => setTeenCount((c) => Math.min(MAX_HEADCOUNT, c + 1))}
                    disabled={adultCount + teenCount >= MAX_HEADCOUNT}
                    className="w-8 h-8 flex items-center justify-center rounded-lg border border-gray-200 text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
                  >
                    <Plus size={16} />
                  </button>
                </div>
              </div>
            </div>

            <div className="flex justify-between items-center border-t pt-4 mb-6">
              <span className="text-gray-600 text-sm">총 인원</span>
              <span className="text-lg font-bold text-blue-600">{requiredSeatCount}명</span>
            </div>

            <button
              type="button"
              onClick={() => setShowHeadcountModal(false)}
              disabled={requiredSeatCount === 0}
              className="w-full p-3 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-bold transition disabled:opacity-40 disabled:cursor-not-allowed"
            >
              선택 완료
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export default function SeatSelectPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  return (
    <Suspense
      fallback={
        <p className="text-center text-gray-400 py-20">불러오는 중...</p>
      }
    >
      <SeatSelectContent params={params} />
    </Suspense>
  );
}