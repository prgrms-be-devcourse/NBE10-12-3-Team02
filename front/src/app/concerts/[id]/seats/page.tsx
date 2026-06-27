"use client";

import { useState } from "react";
import Link from "next/link";

export default function SeatSelectPage() {
  const rows = ["A", "B", "C", "D", "E"];
  const cols = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
  const bookedSeats = ["A-3", "B-5", "C-7", "A-8", "D-2"];

  // 등급별 가격 (행으로 등급 구분: A행=VIP, B행=R, C~D행=S, E행=A)
  const getGrade = (row: string) => {
    if (row === "A") return { name: "VIP", price: 150000 };
    if (row === "B") return { name: "R", price: 120000 };
    if (row === "C" || row === "D") return { name: "S", price: 90000 };
    return { name: "A", price: 70000 };
  };

  const [selectedSeats, setSelectedSeats] = useState<string[]>([]);

  const handleSeatClick = (seatNumber: string) => {
    if (selectedSeats.includes(seatNumber)) {
      setSelectedSeats(selectedSeats.filter((seat) => seat !== seatNumber));
    } else {
      setSelectedSeats([...selectedSeats, seatNumber]);
    }
  };

  // 선택한 좌석들의 총 금액 계산
  const totalPrice = selectedSeats.reduce((sum, seat) => {
    const row = seat.split("-")[0]; // "A-3" → "A"
    return sum + getGrade(row).price;
  }, 0);

  return (
    <div className="min-h-screen bg-gray-50 p-10">
      <div className="max-w-3xl mx-auto">
        <h1 className="text-2xl font-bold text-gray-800 mb-6">좌석 선택</h1>

        <div className="bg-gray-700 text-white text-center py-3 rounded-lg mb-8 font-bold">
          STAGE
        </div>

        <div className="bg-white rounded-2xl shadow-sm p-8">
          <div className="space-y-3">
            {rows.map((row) => (
              <div key={row} className="flex items-center gap-3 justify-center">
                <span className="w-6 font-bold text-gray-400">{row}</span>
                <div className="flex gap-2">
                  {cols.map((col) => {
                    const seatNumber = `${row}-${col}`;
                    const isBooked = bookedSeats.includes(seatNumber);
                    const isSelected = selectedSeats.includes(seatNumber);

                    return (
                      <div
                        key={seatNumber}
                        onClick={() => !isBooked && handleSeatClick(seatNumber)}
                        className={`w-8 h-8 rounded flex items-center justify-center text-xs font-semibold
                          ${
                            isBooked
                              ? "bg-gray-300 text-gray-400 cursor-not-allowed"
                              : isSelected
                              ? "bg-yellow-400 text-white cursor-pointer"
                              : "bg-blue-100 text-blue-700 hover:bg-blue-300 cursor-pointer"
                          }`}
                      >
                        {col}
                      </div>
                    );
                  })}
                </div>
              </div>
            ))}
          </div>

          {/* 범례 */}
          <div className="flex gap-6 justify-center mt-8 text-sm text-gray-500">
            <div className="flex items-center gap-2">
              <div className="w-5 h-5 rounded bg-blue-100"></div> 선택 가능
            </div>
            <div className="flex items-center gap-2">
              <div className="w-5 h-5 rounded bg-yellow-400"></div> 선택됨
            </div>
            <div className="flex items-center gap-2">
              <div className="w-5 h-5 rounded bg-gray-300"></div> 예매 완료
            </div>
          </div>
        </div>

        {/* 선택한 좌석 + 총 금액 */}
        <div className="bg-white rounded-2xl shadow-sm p-6 mt-6">
          <h2 className="font-bold text-gray-700 mb-2">선택한 좌석</h2>
          {selectedSeats.length === 0 ? (
            <p className="text-gray-400 text-sm">좌석을 선택해주세요.</p>
          ) : (
            <>
              <p className="text-blue-600 font-semibold mb-2">
                {selectedSeats.join(", ")} ({selectedSeats.length}석)
              </p>
              <div className="flex justify-between items-center border-t pt-3 mb-4">
                <span className="text-gray-600">총 결제 금액</span>
                <span className="text-xl font-bold text-blue-600">
                  {totalPrice.toLocaleString()}원
                </span>
              </div>
              <Link
                href="/payment"
                className="block w-full p-3 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-bold text-center transition"
              >
                선택 완료 ({selectedSeats.length}석)
              </Link>
            </>
          )}
        </div>
      </div>
    </div>
  );
}