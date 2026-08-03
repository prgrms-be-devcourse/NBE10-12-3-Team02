"use client";

import { useState, useEffect, use } from "react";
import { useRouter } from "next/navigation";
import Image from "next/image";
import Link from "next/link";
import { apiFetch, decodeToken, ApiError, restoreSession } from "@/lib/api";
import { getLocalConcertPoster } from "@/lib/concertDetailImages";
import { formatDateTime } from "@/lib/date";
import { Star, Bookmark, Heart } from "lucide-react";

interface PostDetail {
  postId: number;
  concertId: number;
  userId: number;
  userName: string;
  title: string;
  content: string;
  rating: number | null;
  reviewType: "EXPECTATION" | "REVIEW";
  isMine: boolean;
  isBookmarked: boolean;
  likeCount: number;
  isLiked: boolean;
  createdAt: string;
  updatedAt: string;
  concertName: string;
  posterUrl: string | null;
}

const REVIEW_TYPE_BADGE: Record<
  PostDetail["reviewType"],
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
          size={16}
          className={
            n <= rating ? "fill-yellow-400 text-yellow-400" : "text-gray-200"
          }
        />
      ))}
    </div>
  );
}

interface Comment {
  commentId: number;
  authorId: number;
  authorName: string;
  content: string;
  createdAt: string;
  isMine: boolean;
}

export default function PostDetailPage({
  params,
}: {
  params: Promise<{ postId: string }>;
}) {
  const { postId } = use(params);
  const router = useRouter();

  const [post, setPost] = useState<PostDetail | null>(null);
  const [postLoading, setPostLoading] = useState(true);
  const [editMode, setEditMode] = useState(false);
  const [editForm, setEditForm] = useState<{
    title: string;
    content: string;
    reviewType: PostDetail["reviewType"];
    rating: number | null;
  }>({ title: "", content: "", reviewType: "REVIEW", rating: null });
  const [editError, setEditError] = useState("");
  const [editSubmitting, setEditSubmitting] = useState(false);

  const [comments, setComments] = useState<Comment[]>([]);
  const [commentInput, setCommentInput] = useState("");
  const [commentError, setCommentError] = useState("");
  const [commentSubmitting, setCommentSubmitting] = useState(false);
  const [commentRefreshKey, setCommentRefreshKey] = useState(0);

  const [me, setMe] = useState<{ id: number; name: string } | null>(null);

  const [bookmarked, setBookmarked] = useState(false);
  const [bookmarkPending, setBookmarkPending] = useState(false);

  const [liked, setLiked] = useState(false);
  const [likeCount, setLikeCount] = useState(0);
  const [likePending, setLikePending] = useState(false);

  useEffect(() => {
    const syncAuth = () => setMe(decodeToken());
    syncAuth();
    window.addEventListener("auth-changed", syncAuth);
    return () => window.removeEventListener("auth-changed", syncAuth);
  }, []);

  useEffect(() => {
    restoreSession().then(() => {
      setPostLoading(true);
      apiFetch<PostDetail>(`/posts/${postId}`)
        .then((res) => {
          setPost(res.data);
          setBookmarked(res.data.isBookmarked);
          setLiked(res.data.isLiked);
          setLikeCount(res.data.likeCount);
          setEditForm({
            title: res.data.title,
            content: res.data.content,
            reviewType: res.data.reviewType,
            rating: res.data.rating,
          });
        })
        .catch(() => setPost(null))
        .finally(() => setPostLoading(false));
    });
  }, [postId]);

  useEffect(() => {
    apiFetch<Comment[]>(`/posts/${postId}/comments`)
      .then((res) => setComments(res.data))
      .catch(() => setComments([]));
  }, [postId, commentRefreshKey]);

  const handleUpdate = async () => {
    if (!editForm.title.trim() || !editForm.content.trim()) {
      setEditError("제목과 내용을 입력해주세요.");
      return;
    }
    setEditSubmitting(true);
    setEditError("");
    try {
      const updated = await apiFetch<PostDetail>(
        `/concerts/${post!.concertId}/posts/${postId}`,
        { method: "PUT", body: JSON.stringify(editForm) },
      );
      setPost(updated.data);
      setEditMode(false);
    } catch (e) {
      setEditError(
        e instanceof ApiError ? e.message : "수정 중 오류가 발생했습니다.",
      );
    } finally {
      setEditSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!confirm("게시글을 삭제하시겠습니까?")) return;
    try {
      await apiFetch(`/concerts/${post!.concertId}/posts/${postId}`, {
        method: "DELETE",
      });
      router.push("/board");
    } catch {
      alert("삭제에 실패했습니다.");
    }
  };

  const handleBookmarkToggle = async () => {
    if (!me) {
      router.push("/login");
      return;
    }
    setBookmarkPending(true);
    try {
      if (bookmarked) {
        await apiFetch(`/posts/${postId}/bookmarks`, { method: "DELETE" });
        setBookmarked(false);
      } else {
        await apiFetch(`/posts/${postId}/bookmarks`, { method: "PUT" });
        setBookmarked(true);
      }
    } catch {
      alert("북마크 처리에 실패했습니다.");
    } finally {
      setBookmarkPending(false);
    }
  };

  const handleLikeToggle = async () => {
    if (!me) {
      router.push("/login");
      return;
    }
    const prevLiked = liked;
    const prevCount = likeCount;
    const nextLiked = !liked;
    setLiked(nextLiked);
    setLikeCount((c) => (nextLiked ? c + 1 : c - 1));
    setLikePending(true);
    try {
      const res = await apiFetch<{ isLiked: boolean; likeCount: number }>(
        `/posts/${postId}/likes`,
        { method: nextLiked ? "PUT" : "DELETE" },
      );
      setLiked(res.data.isLiked);
      setLikeCount(res.data.likeCount);
    } catch {
      setLiked(prevLiked);
      setLikeCount(prevCount);
      alert("좋아요 처리에 실패했습니다.");
    } finally {
      setLikePending(false);
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
      await apiFetch(`/posts/${postId}/comments`, {
        method: "POST",
        body: JSON.stringify({ content: commentInput }),
      });
      setCommentInput("");
      setCommentRefreshKey((k) => k + 1);
    } catch (e) {
      setCommentError(
        e instanceof ApiError ? e.message : "댓글 등록 중 오류가 발생했습니다.",
      );
    } finally {
      setCommentSubmitting(false);
    }
  };

  const handleCommentDelete = async (commentId: number) => {
    if (!confirm("댓글을 삭제하시겠습니까?")) return;
    try {
      await apiFetch(`/posts/${postId}/comments/${commentId}`, {
        method: "DELETE",
      });
      setCommentRefreshKey((k) => k + 1);
    } catch {
      alert("댓글 삭제에 실패했습니다.");
    }
  };

  if (postLoading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <p className="text-gray-400">불러오는 중...</p>
      </div>
    );
  }

  if (!post) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <p className="text-red-400">게시글을 찾을 수 없습니다.</p>
      </div>
    );
  }

  const posterUrl = getLocalConcertPoster(post.posterUrl);

  return (
    <div className="min-h-screen bg-gray-50 py-10">
      <div className="max-w-2xl mx-auto px-4 space-y-6">
        {/* 게시글 본문 */}
        <div className="bg-white rounded-2xl shadow-sm p-6">
          <button
            onClick={() => router.push("/board")}
            className="text-sm text-gray-400 hover:text-gray-600 mb-4 inline-block"
          >
            ← 게시판으로
          </button>

          <div className="flex gap-4 mb-5">
            <div className="relative shrink-0 w-14 h-18 rounded-lg overflow-hidden bg-gradient-to-br from-blue-100 to-indigo-200 flex items-center justify-center">
              {posterUrl ? (
                <Image
                  fill
                  unoptimized
                  src={posterUrl}
                  alt={post.concertName}
                  sizes="56px"
                  className="object-cover"
                />
              ) : (
                <span className="text-xs text-gray-400">포스터</span>
              )}
            </div>
            <div>
              <div className="flex items-center gap-2 mb-0.5">
                <p className="text-xs text-blue-600 font-semibold">
                  {post.concertName}
                </p>
                <span
                  className={`text-[10px] font-semibold rounded px-1.5 py-0.5 ${REVIEW_TYPE_BADGE[post.reviewType].className}`}
                >
                  {REVIEW_TYPE_BADGE[post.reviewType].label}
                </span>
              </div>
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
                · {formatDateTime(post.createdAt)}
              </p>
              {post.reviewType === "REVIEW" && post.rating !== null && (
                <div className="mt-1.5">
                  <RatingStars rating={post.rating} />
                </div>
              )}
            </div>
          </div>

          {editMode ? (
            <div className="space-y-3">
              <input
                type="text"
                maxLength={100}
                value={editForm.title}
                onChange={(e) =>
                  setEditForm((f) => ({ ...f, title: e.target.value }))
                }
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-300"
              />
              <textarea
                maxLength={2000}
                rows={6}
                value={editForm.content}
                onChange={(e) =>
                  setEditForm((f) => ({ ...f, content: e.target.value }))
                }
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-300 resize-none"
              />
              {editForm.reviewType === "REVIEW" && (
                <div>
                  <label className="block text-xs text-gray-400 mb-1">
                    별점
                  </label>
                  <div className="flex gap-1">
                    {[1, 2, 3, 4, 5].map((n) => (
                      <button
                        key={n}
                        type="button"
                        onClick={() =>
                          setEditForm((f) => ({ ...f, rating: n }))
                        }
                        aria-label={`${n}점`}
                        className="p-0.5"
                      >
                        <Star
                          size={24}
                          className={
                            editForm.rating !== null && n <= editForm.rating
                              ? "fill-yellow-400 text-yellow-400"
                              : "text-gray-300"
                          }
                        />
                      </button>
                    ))}
                  </div>
                </div>
              )}
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
                  onClick={() => {
                    setEditMode(false);
                    setEditError("");
                  }}
                  className="px-4 py-2 bg-gray-100 text-gray-600 text-sm rounded-lg hover:bg-gray-200 transition"
                >
                  취소
                </button>
              </div>
            </div>
          ) : (
            <>
              <div className="flex items-start justify-between">
                <h1 className="text-xl font-bold text-gray-800">
                  {post.title}
                </h1>
                <div className="flex items-center gap-3 ml-4 shrink-0">
                  {me && (
                    <button
                      onClick={handleLikeToggle}
                      disabled={likePending}
                      aria-label={liked ? "좋아요 취소" : "좋아요"}
                      className="flex items-center gap-1 text-gray-400 hover:text-red-500 disabled:opacity-50 transition"
                    >
                      <Heart
                        size={20}
                        className={liked ? "fill-red-500 text-red-500" : ""}
                      />
                      <span className="text-xs">{likeCount}</span>
                    </button>
                  )}
                  {me && (
                    <button
                      onClick={handleBookmarkToggle}
                      disabled={bookmarkPending}
                      aria-label={bookmarked ? "북마크 취소" : "북마크"}
                      className="text-gray-400 hover:text-blue-500 disabled:opacity-50 transition"
                    >
                      <Bookmark
                        size={20}
                        className={
                          bookmarked ? "fill-blue-500 text-blue-500" : ""
                        }
                      />
                    </button>
                  )}
                  {post.isMine && me && (
                    <div className="flex gap-2">
                      <button
                        onClick={() => {
                          setEditForm({
                            title: post.title,
                            content: post.content,
                            reviewType: post.reviewType,
                            rating: post.rating,
                          });
                          setEditMode(true);
                        }}
                        className="text-xs text-blue-500 hover:text-blue-700"
                      >
                        수정
                      </button>
                      <button
                        onClick={handleDelete}
                        className="text-xs text-red-400 hover:text-red-600"
                      >
                        삭제
                      </button>
                    </div>
                  )}
                </div>
              </div>
              <p className="text-gray-600 text-sm mt-4 leading-relaxed whitespace-pre-wrap">
                {post.content}
              </p>
            </>
          )}
        </div>

        {/* 댓글 섹션 */}
        <div className="bg-white rounded-2xl shadow-sm p-6">
          <h2 className="font-bold text-gray-800 mb-4">
            댓글{" "}
            <span className="text-sm font-normal text-gray-400">
              ({comments.length}개)
            </span>
          </h2>

          {comments.length === 0 ? (
            <p className="text-sm text-gray-400 mb-4">아직 댓글이 없습니다.</p>
          ) : (
            <ul className="space-y-4 mb-5">
              {comments.map((c) => (
                <li
                  key={c.commentId}
                  className="border-b border-gray-100 pb-4 last:border-0"
                >
                  <div className="flex items-start justify-between">
                    <div>
                      <p className="text-xs text-gray-400">
                        {c.isMine ? (
                          c.authorName
                        ) : (
                          <Link
                            href={`/users/${c.authorId}`}
                            className="hover:text-blue-500 hover:underline"
                          >
                            {c.authorName}
                          </Link>
                        )}{" "}
                        · {formatDateTime(c.createdAt)}
                      </p>
                      <p className="text-sm text-gray-700 mt-1">{c.content}</p>
                    </div>
                    {c.isMine && me && (
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
              {commentError && (
                <p className="text-red-500 text-sm">{commentError}</p>
              )}
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
              <button
                onClick={() => router.push("/login")}
                className="text-blue-500 underline"
              >
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
