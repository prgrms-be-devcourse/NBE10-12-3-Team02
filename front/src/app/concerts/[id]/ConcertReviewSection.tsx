"use client";

import { useState, useEffect } from "react";
import { apiFetch, decodeToken, ApiError } from "@/lib/api";

interface ConcertReview {
  reviewId: number;
  concertId: number;
  userId: number;
  userName: string;
  title: string;
  content: string;
  isMine: boolean;
  createdAt: string;
  updatedAt: string;
}

const ELIGIBILITY_MESSAGES: Record<string, string> = {
  "403-4": "해당 콘서트에 대한 유효한 티켓이 없어 리뷰를 작성할 수 없습니다.",
  "403-5": "리뷰 작성 가능 기간이 지났습니다. (콘서트 종료 후 6개월 이내)",
  "409-4": "이미 해당 콘서트에 리뷰를 작성했습니다.",
};

export default function ConcertReviewSection({ concertId }: { concertId: number }) {
  const [reviews, setReviews] = useState<ConcertReview[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState({ title: "", content: "" });
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState("");
  const [refreshKey, setRefreshKey] = useState(0);
  const me = decodeToken();

  useEffect(() => {
    const doFetch = async () => {
      try {
        const res = await apiFetch<ConcertReview[]>(`/concerts/${concertId}/reviews`);
        setReviews(res.data);
      } catch {
        setReviews([]);
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
        await apiFetch(`/concerts/${concertId}/reviews/${editingId}`, {
          method: "PUT",
          body: JSON.stringify(form),
        });
      } else {
        await apiFetch(`/concerts/${concertId}/reviews`, {
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

  const handleEdit = (review: ConcertReview) => {
    setForm({ title: review.title, content: review.content });
    setEditingId(review.reviewId);
    setShowForm(true);
    setFormError("");
  };

  const handleDelete = async (reviewId: number) => {
    if (!confirm("리뷰를 삭제하시겠습니까?")) return;
    try {
      await apiFetch(`/concerts/${concertId}/reviews/${reviewId}`, { method: "DELETE" });
      setRefreshKey((k) => k + 1);
    } catch {
      alert("삭제에 실패했습니다.");
    }
  };

  return (
    <section id="reviews" className="mt-10 bg-white rounded-2xl shadow-sm overflow-hidden">
      <div className="p-8">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-xl font-bold text-gray-800">
            관람 후기
            <span className="ml-2 text-base font-normal text-gray-400">({reviews.length}개)</span>
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
              리뷰 작성
            </button>
          )}
        </div>

        {showForm && (
          <div className="mb-8 p-5 bg-slate-50 rounded-xl border border-slate-200">
            <h3 className="font-semibold text-gray-700 mb-4">
              {editingId !== null ? "리뷰 수정" : "리뷰 작성"}
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
        ) : reviews.length === 0 ? (
          <p className="text-gray-400 text-sm">아직 작성된 후기가 없습니다.</p>
        ) : (
          <ul className="space-y-5">
            {reviews.map((review) => (
              <li key={review.reviewId} className="border-b border-gray-100 pb-5 last:border-0">
                <div className="flex items-start justify-between">
                  <div>
                    <p className="font-semibold text-gray-800">{review.title}</p>
                    <p className="text-xs text-gray-400 mt-0.5">
                      {review.userName} · {review.createdAt?.slice(0, 10)}
                    </p>
                  </div>
                  {review.isMine && (
                    <div className="flex gap-2 shrink-0 ml-4">
                      <button
                        onClick={() => handleEdit(review)}
                        className="text-xs text-blue-500 hover:text-blue-700"
                      >
                        수정
                      </button>
                      <button
                        onClick={() => handleDelete(review.reviewId)}
                        className="text-xs text-red-400 hover:text-red-600"
                      >
                        삭제
                      </button>
                    </div>
                  )}
                </div>
                <p className="text-gray-600 text-sm mt-2 leading-relaxed whitespace-pre-wrap">
                  {review.content}
                </p>
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  );
}
