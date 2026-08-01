"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { apiFetch, decodeToken, ApiError } from "@/lib/api";
import { Star, Heart } from "lucide-react";

interface ConcertPost {
  postId: number;
  concertId: number;
  userId: number;
  userName: string;
  title: string;
  content: string;
  rating: number | null;
  reviewType: "EXPECTATION" | "REVIEW";
  isMine: boolean;
  likeCount: number;
  isLiked: boolean;
  createdAt: string;
  updatedAt: string;
}

const REVIEW_TYPE_BADGE: Record<ConcertPost["reviewType"], { label: string; className: string }> = {
  REVIEW: { label: "관람후기", className: "bg-emerald-50 text-emerald-600 border border-emerald-200" },
  EXPECTATION: { label: "기대평", className: "bg-amber-50 text-amber-600 border border-amber-200" },
};

function RatingStars({ rating }: { rating: number }) {
  return (
    <div className="flex items-center gap-0.5">
      {[1, 2, 3, 4, 5].map((n) => (
        <Star
          key={n}
          size={13}
          className={n <= rating ? "fill-yellow-400 text-yellow-400" : "text-gray-200"}
        />
      ))}
    </div>
  );
}

const ELIGIBILITY_MESSAGES: Record<string, string> = {
  "403-4": "해당 콘서트에 대한 유효한 티켓이 없어 게시글을 작성할 수 없습니다.",
  "403-5": "게시글 작성 가능 기간이 지났습니다. (콘서트 종료 후 6개월 이내)",
  "409-4": "이미 해당 콘서트에 게시글을 작성했습니다.",
};

export default function ConcertPostSection({ concertId }: { concertId: number }) {
  const [posts, setPosts] = useState<ConcertPost[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState({ title: "", content: "" });
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState("");
  const [refreshKey, setRefreshKey] = useState(0);
  const [me, setMe] = useState<{ id: number; name: string } | null>(null);

  useEffect(() => {
    const syncAuth = () => setMe(decodeToken());
    syncAuth();
    window.addEventListener("auth-changed", syncAuth);
    return () => window.removeEventListener("auth-changed", syncAuth);
  }, []);

  useEffect(() => {
    const doFetch = async () => {
      try {
        const res = await apiFetch<ConcertPost[]>(`/concerts/${concertId}/posts`);
        setPosts(res.data);
      } catch {
        setPosts([]);
      } finally {
        setLoading(false);
      }
    };
    doFetch();
  }, [concertId, refreshKey]);

  const resetForm = () => {
    setForm({ title: "", content: "" });
    setFormError("");
    setShowForm(false);
    setEditingId(null);
  };

  const handleSubmit = async () => {
    if (!form.title.trim() || !form.content.trim()) {
      setFormError("제목과 내용을 입력해주세요.");
      return;
    }
    setSubmitting(true);
    setFormError("");
    try {
      if (editingId !== null) {
        await apiFetch(`/concerts/${concertId}/posts/${editingId}`, {
          method: "PUT",
          body: JSON.stringify(form),
        });
      } else {
        await apiFetch(`/concerts/${concertId}/posts`, {
          method: "POST",
          body: JSON.stringify(form),
        });
      }
      resetForm();
      setRefreshKey((k) => k + 1);
    } catch (e) {
      if (e instanceof ApiError) {
        setFormError(ELIGIBILITY_MESSAGES[e.resultCode ?? ""] ?? e.message);
      } else {
        setFormError("오류가 발생했습니다. 다시 시도해주세요.");
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleEdit = (post: ConcertPost) => {
    setForm({ title: post.title, content: post.content });
    setEditingId(post.postId);
    setShowForm(true);
    setFormError("");
  };

  const handleDelete = async (postId: number) => {
    if (!confirm("게시글을 삭제하시겠습니까?")) return;
    try {
      await apiFetch(`/concerts/${concertId}/posts/${postId}`, { method: "DELETE" });
      setRefreshKey((k) => k + 1);
    } catch {
      alert("삭제에 실패했습니다.");
    }
  };

  return (
    <section id="posts" className="mt-10 bg-white rounded-2xl shadow-sm overflow-hidden">
      <div className="p-8">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-xl font-bold text-gray-800">
            관람 후기
            <span className="ml-2 text-base font-normal text-gray-400">({posts.length}개)</span>
          </h2>
          {me && !showForm && (
            <button
              onClick={() => {
                setShowForm(true);
                setEditingId(null);
                setForm({ title: "", content: "" });
                setFormError("");
              }}
              className="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition"
            >
              게시글 작성
            </button>
          )}
        </div>

        {showForm && (
          <div className="mb-8 p-5 bg-slate-50 rounded-xl border border-slate-200">
            <h3 className="font-semibold text-gray-700 mb-4">
              {editingId !== null ? "게시글 수정" : "게시글 작성"}
            </h3>
            <div className="mb-3">
              <input
                type="text"
                placeholder="제목 (최대 100자)"
                maxLength={100}
                value={form.title}
                onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-300"
              />
            </div>
            <div className="mb-3">
              <textarea
                placeholder="내용 (최대 2000자)"
                maxLength={2000}
                rows={4}
                value={form.content}
                onChange={(e) => setForm((f) => ({ ...f, content: e.target.value }))}
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-300 resize-none"
              />
            </div>
            {formError && <p className="text-red-500 text-sm mb-3">{formError}</p>}
            <div className="flex gap-2">
              <button
                onClick={handleSubmit}
                disabled={submitting}
                className="px-5 py-2 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 disabled:opacity-50 transition"
              >
                {submitting ? "저장 중..." : editingId !== null ? "수정하기" : "등록하기"}
              </button>
              <button
                onClick={resetForm}
                className="px-5 py-2 bg-gray-100 text-gray-600 text-sm rounded-lg hover:bg-gray-200 transition"
              >
                취소
              </button>
            </div>
          </div>
        )}

        {loading ? (
          <p className="text-gray-400 text-sm">불러오는 중...</p>
        ) : posts.length === 0 ? (
          <p className="text-gray-400 text-sm">아직 작성된 후기가 없습니다.</p>
        ) : (
          <ul className="space-y-5">
            {posts.map((post) => (
              <li key={post.postId} className="border-b border-gray-100 pb-5 last:border-0">
                <div className="flex items-start justify-between">
                  <div>
                    <div className="flex items-center gap-2">
                      <p className="font-semibold text-gray-800">{post.title}</p>
                      <span
                        className={`shrink-0 text-[10px] font-semibold rounded px-1.5 py-0.5 ${REVIEW_TYPE_BADGE[post.reviewType].className}`}
                      >
                        {REVIEW_TYPE_BADGE[post.reviewType].label}
                      </span>
                    </div>
                    <div className="flex items-center gap-2 mt-0.5">
                      <p className="text-xs text-gray-400">
                        {post.isMine ? (
                          post.userName
                        ) : (
                          <Link
                            href={`/users/${post.userId}`}
                            className="hover:text-blue-500 hover:underline"
                          >
                            {post.userName}
                          </Link>
                        )}{" "}
                        · {post.createdAt?.slice(0, 10)}
                      </p>
                      <span className="flex items-center gap-0.5 text-xs text-gray-400">
                        <Heart
                          size={12}
                          className={post.isLiked ? "fill-red-500 text-red-500" : ""}
                        />
                        {post.likeCount}
                      </span>
                    </div>
                    {post.reviewType === "REVIEW" && post.rating !== null && (
                      <div className="mt-1">
                        <RatingStars rating={post.rating} />
                      </div>
                    )}
                  </div>
                  {post.isMine && me && (
                    <div className="flex gap-2 shrink-0 ml-4">
                      <button
                        onClick={() => handleEdit(post)}
                        className="text-xs text-blue-500 hover:text-blue-700"
                      >
                        수정
                      </button>
                      <button
                        onClick={() => handleDelete(post.postId)}
                        className="text-xs text-red-400 hover:text-red-600"
                      >
                        삭제
                      </button>
                    </div>
                  )}
                </div>
                <p className="text-gray-600 text-sm mt-2 leading-relaxed whitespace-pre-wrap">
                  {post.content}
                </p>
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  );
}
