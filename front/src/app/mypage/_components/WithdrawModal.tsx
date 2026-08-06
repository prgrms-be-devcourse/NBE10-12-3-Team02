"use client";

import { useEffect } from "react";

interface WithdrawModalProps {
  show: boolean;
  onClose: () => void;
  onWithdraw: () => Promise<void>;
}

export function WithdrawModal({ show, onClose, onWithdraw }: WithdrawModalProps) {
  useEffect(() => {
    if (show) {
      document.body.style.overflow = "hidden";
      return () => {
        document.body.style.overflow = "";
      };
    }
  }, [show]);

  if (!show) return null;

  return (
    <div className="fixed inset-0 z-[100] bg-black/50 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl p-8 max-w-sm w-full">
        <h2 className="text-xl font-bold text-center text-gray-800 mb-3">
          정말 탈퇴하시겠어요?
        </h2>
        <p className="text-center text-gray-500 text-sm mb-6">
          탈퇴 시 모든 예매 내역이 사라지며,
          <br />
          되돌릴 수 없습니다.
        </p>
        <div className="flex gap-3">
          <button
            onClick={onClose}
            className="flex-1 p-3 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-lg font-bold transition"
          >
            취소
          </button>
          <button
            onClick={onWithdraw}
            className="flex-1 p-3 bg-red-500 hover:bg-red-600 text-white rounded-lg font-bold transition"
          >
            탈퇴하기
          </button>
        </div>
      </div>
    </div>
  );
}
