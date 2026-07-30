"use client";

import { useState, useEffect, use } from "react";
import { useRouter } from "next/navigation";
import { apiFetch, decodeToken, ApiError, restoreSession } from "@/lib/api";
import { getLocalConcertPoster } from "@/lib/concertDetailImages";
import { formatDateTime } from "@/lib/date";

interface ReviewDetail {
  reviewId: number;
  concertId: number;
  userId: number;
  userName: string;
  title: string;
  content: string;
  isMine: boolean;
  createdAt: string;
  updatedAt: string;
  concertName: string;
  posterUrl: string | null;
}

interface Comment {
  commentId: number;
  authorName: string;
  content: string;
  createdAt: string;
  isMine: boolean;
}

export default function ReviewDetailPage({
  params,
}: {
  params: Promise<{ reviewId: string }>;
}) {
  const { reviewId } = use(params);
  const router = useRouter();

  const [review, setReview] = useState<ReviewDetail | null>(null);
  const [reviewLoading, setReviewLoading] = useState(true);
  const [editMode, setEditMode] = useState(false);
  const [editForm, setEditForm] = useState({ title: "", content: "" });
  const [editError, setEditError] = useState("");
  const [editSubmitting, setEditSubmitting] = useState(false);

  const [comments, setComments] = useState<Comment[]>([]);
  const [commentInput, setCommentInput] = useState("");
  const [commentError, setCommentError] = useState("");
  const [commentSubmitting, setCommentSubmitting] = useState(false);
  const [commentRefreshKey, setCommentRefreshKey] = useState(0);

  const me = decodeToken();

  useEffect(() => {
    restoreSession().then(() => {
      setReviewLoading(true);
      apiFetch<ReviewDetail>(`/reviews/${reviewId}`)
        .then((res) => {
          setReview(res.data);
          setEditForm({ title: res.data.title, content: res.data.content });
        })
        .catch(() => setReview(null))
        .finally(() => setReviewLoading(false));
    });
  }, [reviewId]);

  useEffect(() => {
    apiFetch<Comment[]>(`/reviews/${reviewId}/comments`)
      .then((res) => setComments(res.data))
      .catch(() => setComments([]));
  }, [reviewId, commentRefreshKey]);

  const handleUpdate = async () => {
    if (!editForm.title.trim() || !editForm.content.trim()) {
      setEditError("제목과 내용을 입력해주세요.");
      return;
    }
    setEditSubmitting(true);
    setEditError("");
    try {
      const updated = await apiFetch<ReviewDetail>(
        `/concerts/${review!.concertId}/reviews/${reviewId}`,
        { method: "PUT", body: JSON.stringify(editForm) }
      );
      setReview(updated.data);
      setEditMode(false);
    } catch (e) {
      setEditError(e instanceof ApiError ? e.message : "수정 중 오류가 발생했습니다.");
    } finally {
      setEditSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!confirm("리뷰를 삭제하시겠습니까?")) return;
    try {
      await apiFetch(`/concerts/${review!.concertId}/reviews/${reviewId}`, { method: "DELETE" });
      router.push("/board");
    } catch {
      alert("삭제에 실패했습니다.");
    }
  };

  const handleCommentSubmit = async () => {
    if (!commentInput.trim()) {
      setCommentError("댓글 내용을 입력해주세요.");
      return;
    }
    setCommentSubmitting(true);
    setCommentError("");
    try {
      await apiFetch(`/reviews/${reviewId}/comments`, {
        method: "POST",
        body: JSON.stringify({ content: commentInput }),
      });
      setCommentInput("");
      setCommentRefreshKey((k) => k + 1);
    } catch (e) {
      setCommentError(e instanceof ApiError ? e.message : "댓글 등록 중 오류가 발생했습니다.");
    } finally {
      setCommentSubmitting(false);
    }
  };

  const handleCommentDelete = async (commentId: number) => {
    if (!confirm("댓글을 삭제하시겠습니까?")) return;
    try {
      await apiFetch(`/reviews/${reviewId}/comments/${commentId}`, { method: "DELETE" });
      setCommentRefreshKey((k) => k + 1);
    } catch {
      alert("댓글 삭제에 실패했습니다.");
    }
  };

  if (reviewLoading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <p className="text-gray-400">불러오는 중...</p>
      </div>
    );
  }

  if (!review) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <p className="text-red-400">리뷰를 찾을 수 없습니다.</p>
      </div>
    );
  }

  const posterUrl = getLocalConcertPoster(review.posterUrl);

  return (
    <div className="min-h-screen bg-gray-50 py-10">
      <div className="max-w-2xl mx-auto px-4 space-y-6">

        {/* 리뷰 본문 */}
        <div className="bg-white rounded-2xl shadow-sm p-6">
          <button
            onClick={() => router.push("/board")}
            className="text-sm text-gray-400 hover:text-gray-600 mb-4 inline-block"
          >
            ← 게시판으로
          </button>

          <div className="flex gap-4 mb-5">
            <div className="shrink-0 w-14 h-18 rounded-lg overflow-hidden bg-gradient-to-br from-blue-100 to-indigo-200 flex items-center justify-center">
              {posterUrl ? (
                <img src={posterUrl} alt={review.concertName} className="w-full h-full object-cover" />
              ) : (
                <span className="text-xs text-gray-400">포스터</span>
              )}
            </div>
            <div>
              <p className="text-xs text-blue-600 font-semibold mb-0.5">{review.concertName}</p>
              <p className="text-xs text-gray-400">{review.userName} · {formatDateTime(review.createdAt)}</p>
            </div>
          </div>

          {editMode ? (
            <div className="space-y-3">
              <input
                type="text"
                maxLength={100}
                value={editForm.title}
                onChange={(e) => setEditForm((f) => ({ ...f, title: e.target.value }))}
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-300"
              />
              <textarea
                maxLength={2000}
                rows={6}
                value={editForm.content}
                onChange={(e) => setEditForm((f) => ({ ...f, content: e.target.value }))}
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-300 resize-none"
              />
              {editError && <p className="text-red-500 text-sm">{editError}</p>}
              <div className="flex gap-2">
                <button
                  onClick={handleUpdate}
                  disabled={editSubmitting}
                  className="px-4 py-2 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 disabled:opacity-50 transition"
                >
                  {editSubmitting ? "저장 중..." : "수정 완료"}
                </button>
                <button
                  onClick={() => { setEditMode(false); setEditError(""); }}
                  className="px-4 py-2 bg-gray-100 text-gray-600 text-sm rounded-lg hover:bg-gray-200 transition"
                >
                  취소
                </button>
              </div>
            </div>
          ) : (
            <>
              <div className="flex items-start justify-between">
                <h1 className="text-xl font-bold text-gray-800">{review.title}</h1>
                {review.isMine && (
                  <div className="flex gap-2 ml-4 shrink-0">
                    <button
                      onClick={() => { setEditForm({ title: review.title, content: review.content }); setEditMode(true); }}
                      className="text-xs text-blue-500 hover:text-blue-700"
                    >
                      수정
                    </button>
                    <button onClick={handleDelete} className="text-xs text-red-400 hover:text-red-600">
                      삭제
                    </button>
                  </div>
                )}
              </div>
              <p className="text-gray-600 text-sm mt-4 leading-relaxed whitespace-pre-wrap">{review.content}</p>
            </>
          )}
        </div>

        {/* 댓글 섹션 */}
        <div className="bg-white rounded-2xl shadow-sm p-6">
          <h2 className="font-bold text-gray-800 mb-4">
            댓글 <span className="text-sm font-normal text-gray-400">({comments.length}개)</span>
          </h2>

          {comments.length === 0 ? (
            <p className="text-sm text-gray-400 mb-4">아직 댓글이 없습니다.</p>
          ) : (
            <ul className="space-y-4 mb-5">
              {comments.map((c) => (
                <li key={c.commentId} className="border-b border-gray-100 pb-4 last:border-0">
                  <div className="flex items-start justify-between">
                    <div>
                      <p className="text-xs text-gray-400">{c.authorName} · {formatDateTime(c.createdAt)}</p>
                      <p className="text-sm text-gray-700 mt-1">{c.content}</p>
                    </div>
                    {c.isMine && (
                      <button
                        onClick={() => handleCommentDelete(c.commentId)}
                        className="text-xs text-red-400 hover:text-red-600 shrink-0 ml-3"
                      >
                        삭제
                      </button>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          )}

          {me ? (
            <div className="space-y-2">
              <textarea
                placeholder="댓글을 입력하세요"
                rows={3}
                value={commentInput}
                onChange={(e) => setCommentInput(e.target.value)}
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-300 resize-none"
              />
              {commentError && <p className="text-red-500 text-sm">{commentError}</p>}
              <button
                onClick={handleCommentSubmit}
                disabled={commentSubmitting}
                className="px-4 py-2 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 disabled:opacity-50 transition"
              >
                {commentSubmitting ? "등록 중..." : "댓글 등록"}
              </button>
            </div>
          ) : (
            <p className="text-sm text-gray-400">
              댓글을 작성하려면{" "}
              <button onClick={() => router.push("/login")} className="text-blue-500 underline">
                로그인
              </button>
              하세요.
            </p>
          )}
        </div>

      </div>
    </div>
  );
}
