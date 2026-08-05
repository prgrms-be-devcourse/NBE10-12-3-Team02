"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import Image from "next/image";
import Link from "next/link";
import { apiFetch, decodeToken, restoreSession } from "@/lib/api";
import { getLocalConcertPoster } from "@/lib/concertDetailImages";
import { formatDateTime } from "@/lib/date";
import { Star, Heart } from "lucide-react";

interface PostCard {
  postId: number;
  concertId: number;
  userId: number;
  userName: string;
  title: string;
  content: string;
  rating: number | null;
  reviewType: "EXPECTATION" | "REVIEW";
  summary: string | null;
  isMine: boolean;
  likeCount: number;
  isLiked: boolean;
  createdAt: string;
  concertName: string;
  posterUrl: string | null;
}

type ReviewTypeFilter = "all" | "REVIEW" | "EXPECTATION";

const TYPE_FILTER_OPTIONS: { key: ReviewTypeFilter; label: string }[] = [
  { key: "all", label: "전체" },
  { key: "REVIEW", label: "관람후기" },
  { key: "EXPECTATION", label: "기대평" },
];

const REVIEW_TYPE_BADGE: Record<
  PostCard["reviewType"],
  { label: string; className: string }
> = {
  REVIEW: {
    label: "관람후기",
    className: "bg-emerald-50 text-emerald-600 border border-emerald-200",
  },
  EXPECTATION: {
    label: "기대평",
    className: "bg-amber-50 text-amber-600 border border-amber-200",
  },
};

function RatingStars({ rating }: { rating: number }) {
  return (
    <div className="flex items-center gap-0.5">
      {[1, 2, 3, 4, 5].map((n) => (
        <Star
          key={n}
          size={13}
          className={
            n <= rating ? "fill-yellow-400 text-yellow-400" : "text-gray-200"
          }
        />
      ))}
    </div>
  );
}

export default function BoardPage() {
  const router = useRouter();
  const [posts, setPosts] = useState<PostCard[]>([]);
  const [loading, setLoading] = useState(true);
  const [typeFilter, setTypeFilter] = useState<ReviewTypeFilter>("all");

  useEffect(() => {
    // accessToken이 준비된 이후 fetch해야 isMine이 정확히 내려옴
    restoreSession().then(() => {
      apiFetch<PostCard[]>("/posts")
        .then((res) => setPosts(res.data))
        .catch(() => setPosts([]))
        .finally(() => setLoading(false));
    });
  }, []);

  const handleWriteClick = () => {
    if (!decodeToken()) {
      router.push("/login");
      return;
    }
    router.push("/board/write");
  };

  const filteredPosts = posts.filter(
    (post) => typeFilter === "all" || post.reviewType === typeFilter,
  );

  return (
    <div className="min-h-screen bg-gray-50 py-10">
      <div className="max-w-3xl mx-auto px-4">
        <h1 className="text-2xl font-bold text-gray-800 mb-6">
          관람 후기 게시판
        </h1>

        <div className="flex gap-2 mb-6">
          {TYPE_FILTER_OPTIONS.map((f) => (
            <button
              key={f.key}
              onClick={() => setTypeFilter(f.key)}
              className={`px-3 py-1.5 rounded-lg text-sm font-semibold border transition ${
                typeFilter === f.key
                  ? "bg-blue-600 text-white border-blue-600"
                  : "bg-white text-gray-600 border-gray-200 hover:border-blue-400"
              }`}
            >
              {f.label}
            </button>
          ))}
        </div>

        {loading ? (
          <p className="text-gray-400 text-sm">불러오는 중...</p>
        ) : filteredPosts.length === 0 ? (
          <p className="text-gray-400 text-sm">아직 작성된 후기가 없습니다.</p>
        ) : (
          <ul className="space-y-4">
            {filteredPosts.map((post) => (
              <li
                key={post.postId}
                onClick={() => router.push(`/board/${post.postId}`)}
                className="bg-white rounded-2xl shadow-sm border border-gray-100 flex gap-4 p-4 cursor-pointer hover:shadow-md transition"
              >
                <div className="relative shrink-0 w-16 h-20 rounded-lg overflow-hidden bg-gradient-to-br from-blue-100 to-indigo-200 flex items-center justify-center">
                  {post.posterUrl ? (
                    <Image
                      fill
                      unoptimized
                      src={getLocalConcertPoster(post.posterUrl)}
                      alt={post.concertName}
                      sizes="64px"
                      className="object-cover"
                    />
                  ) : (
                    <span className="text-xs text-gray-400 text-center px-1">
                      포스터
                    </span>
                  )}
                </div>

                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-0.5">
                    <p className="text-xs text-blue-600 font-semibold truncate">
                      {post.concertName}
                    </p>
                    <span
                      className={`shrink-0 text-[10px] font-semibold rounded px-1.5 py-0.5 ${REVIEW_TYPE_BADGE[post.reviewType].className}`}
                    >
                      {REVIEW_TYPE_BADGE[post.reviewType].label}
                    </span>
                    {post.isMine && (
                      <span className="shrink-0 text-[10px] font-semibold text-blue-600 bg-blue-50 border border-blue-200 rounded px-1.5 py-0.5">
                        내 글
                      </span>
                    )}
                  </div>
                  <p className="font-semibold text-gray-800 truncate">
                    {post.title}
                  </p>
                  {post.reviewType === "REVIEW" && post.rating !== null && (
                    <div className="mt-1">
                      <RatingStars rating={post.rating} />
                    </div>
                  )}
                  {post.reviewType === "REVIEW" && post.summary && (
                    <p className="mt-1.5 text-xs text-indigo-600 bg-indigo-50 border border-indigo-100 rounded-lg px-2 py-1 leading-relaxed">
                      <span className="font-semibold">✨ AI 요약:</span>{" "}
                      {post.summary}
                    </p>
                  )}
                  <p className="text-sm text-gray-500 mt-1 line-clamp-2 leading-relaxed">
                    {post.content}
                  </p>
                  <div className="flex items-center gap-2 mt-2">
                    <p className="text-xs text-gray-400">
                      {post.isMine ? (
                        post.userName
                      ) : (
                        <Link
                          href={`/users/${post.userId}`}
                          onClick={(e) => e.stopPropagation()}
                          className="hover:text-blue-500 hover:underline"
                        >
                          {post.userName}
                        </Link>
                      )}{" "}
                      · {formatDateTime(post.createdAt)}
                    </p>
                    <span className="flex items-center gap-0.5 text-xs text-gray-400">
                      <Heart
                        size={12}
                        className={
                          post.isLiked ? "fill-red-500 text-red-500" : ""
                        }
                      />
                      {post.likeCount}
                    </span>
                  </div>
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
