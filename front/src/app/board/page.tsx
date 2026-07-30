"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { apiFetch, decodeToken } from "@/lib/api";
import { getLocalConcertPoster } from "@/lib/concertDetailImages";

interface ReviewCard {
  reviewId: number;
  concertId: number;
  userId: number;
  userName: string;
  title: string;
  content: string;
  isMine: boolean;
  createdAt: string;
  concertName: string;
  posterUrl: string | null;
}

export default function BoardPage() {
  const router = useRouter();
  const [reviews, setReviews] = useState<ReviewCard[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    apiFetch<ReviewCard[]>("/reviews")
      .then((res) => setReviews(res.data))
      .catch(() => setReviews([]))
      .finally(() => setLoading(false));
  }, []);

  const handleWriteClick = () => {
    if (!decodeToken()) {
      router.push("/login");
      return;
    }
    router.push("/board/write");
  };

  return (
    <div className="min-h-screen bg-gray-50 py-10">
      <div className="max-w-3xl mx-auto px-4">
        <h1 className="text-2xl font-bold text-gray-800 mb-6">관람 후기 게시판</h1>

        {loading ? (
          <p className="text-gray-400 text-sm">불러오는 중...</p>
        ) : reviews.length === 0 ? (
          <p className="text-gray-400 text-sm">아직 작성된 후기가 없습니다.</p>
        ) : (
          <ul className="space-y-4">
            {reviews.map((review) => (
              <li
                key={review.reviewId}
                onClick={() =>
                  router.push(
                    `/concerts/${review.concertId}${review.isMine ? "#reviews" : ""}`
                  )
                }
                className="bg-white rounded-2xl shadow-sm border border-gray-100 flex gap-4 p-4 cursor-pointer hover:shadow-md transition"
              >
                <div className="shrink-0 w-16 h-20 rounded-lg overflow-hidden bg-gradient-to-br from-blue-100 to-indigo-200 flex items-center justify-center">
                  {review.posterUrl ? (
                    <img
                      src={getLocalConcertPoster(review.posterUrl)}
                      alt={review.concertName}
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <span className="text-xs text-gray-400 text-center px-1">포스터</span>
                  )}
                </div>

                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-0.5">
                    <p className="text-xs text-blue-600 font-semibold truncate">
                      {review.concertName}
                    </p>
                    {review.isMine && (
                      <span className="shrink-0 text-[10px] font-semibold text-blue-600 bg-blue-50 border border-blue-200 rounded px-1.5 py-0.5">
                        내 글
                      </span>
                    )}
                  </div>
                  <p className="font-semibold text-gray-800 truncate">{review.title}</p>
                  <p className="text-sm text-gray-500 mt-1 line-clamp-2 leading-relaxed">
                    {review.content}
                  </p>
                  <p className="text-xs text-gray-400 mt-2">
                    {review.userName} · {review.createdAt?.slice(0, 10)}
                  </p>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>

      <button
        onClick={handleWriteClick}
        className="fixed bottom-8 right-8 w-14 h-14 bg-blue-600 hover:bg-blue-700 text-white rounded-full shadow-lg flex items-center justify-center text-2xl transition"
        aria-label="글쓰기"
      >
        ✏️
      </button>
    </div>
  );
}
