"use client";

import { Check, Share2 } from "lucide-react";
import { useState } from "react";

export default function ShareButton({
  url,
  className = "",
}: {
  url?: string;
  className?: string;
}) {
  const [copied, setCopied] = useState(false);

  const handleClick = async (e: React.MouseEvent) => {
    e.stopPropagation();
    const target = url
      ? url.startsWith("http")
        ? url
        : window.location.origin + url
      : window.location.href;
    try {
      await navigator.clipboard.writeText(target);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      /* 클립보드 API 미지원 환경에서 조용히 무시 */
    }
  };

  return (
    <button
      onClick={handleClick}
      aria-label="링크 복사"
      className={`flex items-center gap-1 text-xs text-gray-400 hover:text-blue-500 transition ${className}`}
    >
      {copied ? (
        <>
          <Check size={15} className="text-green-500" />
          <span className="text-green-500">복사됨</span>
        </>
      ) : (
        <Share2 size={15} />
      )}
    </button>
  );
}
