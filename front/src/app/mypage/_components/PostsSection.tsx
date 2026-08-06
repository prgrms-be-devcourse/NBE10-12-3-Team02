"use client";

import { useState, useEffect } from "react";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Star } from "lucide-react";
import { apiFetch } from "@/lib/api";
import { getLocalConcertPoster } from "@/lib/concertDetailImages";
import { formatDateTime } from "@/lib/date";

interface MyPostSummary {
  postId: number;
  title: string;
  rating: number | null;
  reviewType: "EXPECTATION" | "REVIEW";
  likeCount: number;
  createdAt: string;
  concertName: string;
}

interface PostBookmarkSummary {
  postId: number;
  concertId: number;
  concertName: string;
  userName: string;
  title: string;
  content: string;
  posterUrl: string | null;
  bookmarkedAt: string | null;
}

interface PostLikeSummary {
  postId: number;
  concertId: number;
  concertName: string;
  userName: string;
  title: string;
  content: string;
  posterUrl: string | null;
  likedAt: string | null;
}

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  last: boolean;
}

const REVIEW_TYPE_BADGE: Record<
  MyPostSummary["reviewType"],
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

const MY_POSTS_PAGE_SIZE = 5;
const MY_BOOKMARKS_PAGE_SIZE = 5;
const MY_LIKES_PAGE_SIZE = 5;

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

export function PostsSection() {
  const router = useRouter();
  const [postsSubTab, setPostsSubTab] = useState<"my" | "bookmarks" | "likes">(
    "my",
  );

  const [myPosts, setMyPosts] = useState<MyPostSummary[]>([]);
  const [myPostsLoading, setMyPostsLoading] = useState(true);
  const [myPostsPage, setMyPostsPage] = useState(0);
  const [myPostsTotalPages, setMyPostsTotalPages] = useState(0);
  const [myPostsTotalElements, setMyPostsTotalElements] = useState(0);

  const [bookmarks, setBookmarks] = useState<PostBookmarkSummary[]>([]);
  const [bookmarksLoading, setBookmarksLoading] = useState(true);
  const [bookmarksPage, setBookmarksPage] = useState(0);
  const [bookmarksTotalPages, setBookmarksTotalPages] = useState(0);
  const [bookmarksTotalElements, setBookmarksTotalElements] = useState(0);

  const [likes, setLikes] = useState<PostLikeSummary[]>([]);
  const [likesLoading, setLikesLoading] = useState(true);
  const [likesPage, setLikesPage] = useState(0);
  const [likesTotalPages, setLikesTotalPages] = useState(0);
  const [likesTotalElements, setLikesTotalElements] = useState(0);

  const fetchMyPosts = async (page: number) => {
    setMyPostsLoading(true);
    try {
      const res = await apiFetch<PageResponse<MyPostSummary>>(
        `/posts/me?page=${page}&size=${MY_POSTS_PAGE_SIZE}`,
      );
      setMyPosts(res.data.content);
      setMyPostsPage(res.data.number);
      setMyPostsTotalPages(res.data.totalPages);
      setMyPostsTotalElements(res.data.totalElements);
    } catch {
      setMyPosts([]);
      setMyPostsTotalPages(0);
      setMyPostsTotalElements(0);
    } finally {
      setMyPostsLoading(false);
    }
  };

  const fetchBookmarks = async (page: number) => {
    setBookmarksLoading(true);
    try {
      const res = await apiFetch<PageResponse<PostBookmarkSummary>>(
        `/users/me/post-bookmarks?page=${page}&size=${MY_BOOKMARKS_PAGE_SIZE}`,
      );
      setBookmarks(res.data.content);
      setBookmarksPage(res.data.number);
      setBookmarksTotalPages(res.data.totalPages);
      setBookmarksTotalElements(res.data.totalElements);
    } catch {
      setBookmarks([]);
      setBookmarksTotalPages(0);
      setBookmarksTotalElements(0);
    } finally {
      setBookmarksLoading(false);
    }
  };

  const fetchLikes = async (page: number) => {
    setLikesLoading(true);
    try {
      const res = await apiFetch<PageResponse<PostLikeSummary>>(
        `/users/me/post-likes?page=${page}&size=${MY_LIKES_PAGE_SIZE}`,
      );
      setLikes(res.data.content);
      setLikesPage(res.data.number);
      setLikesTotalPages(res.data.totalPages);
      setLikesTotalElements(res.data.totalElements);
    } catch {
      setLikes([]);
      setLikesTotalPages(0);
      setLikesTotalElements(0);
    } finally {
      setLikesLoading(false);
    }
  };

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchMyPosts(0);
    fetchBookmarks(0);
    fetchLikes(0);
  }, []);

  return (
    <div className="bg-white rounded-2xl shadow-sm p-8">
      <div className="flex items-center justify-between mb-6">
        <div className="flex gap-2">
          {(
            [
              { key: "my", label: "내 게시글" },
              { key: "bookmarks", label: "북마크" },
              { key: "likes", label: "좋아요" },
            ] as const
          ).map((st) => (
            <button
              key={st.key}
              onClick={() => setPostsSubTab(st.key)}
              className={`px-4 py-2 rounded-lg text-sm font-semibold border transition ${
                postsSubTab === st.key
                  ? "bg-blue-600 text-white border-blue-600"
                  : "bg-white text-gray-600 border-gray-200 hover:border-blue-400"
              }`}
            >
              {st.label}
            </button>
          ))}
        </div>
        <Link
          href="/mypage/follows"
          className="px-3 py-1.5 bg-white border border-gray-200 hover:border-blue-300 text-gray-600 hover:text-blue-600 text-sm font-semibold rounded-lg transition"
        >
          팔로우 목록 보기
        </Link>
      </div>

      {postsSubTab === "my" && (
        <>
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-bold text-gray-700">내 게시글</h2>
            <span className="text-sm text-gray-400">
              {myPostsTotalElements}개
            </span>
          </div>
          {myPostsLoading ? (
            <p className="text-sm text-gray-400 text-center py-10">
              불러오는 중...
            </p>
          ) : myPosts.length === 0 ? (
            <p className="text-sm text-gray-400 text-center py-10">
              작성한 게시글이 없습니다.
            </p>
          ) : (
            <div className="space-y-3">
              {myPosts.map((post) => (
                <div
                  key={post.postId}
                  onClick={() => router.push(`/board/${post.postId}`)}
                  role="button"
                  tabIndex={0}
                  className="p-4 border border-gray-100 rounded-xl hover:shadow-md hover:border-blue-200 transition cursor-pointer"
                >
                  <div className="flex items-center gap-2 mb-1">
                    <h3 className="font-semibold text-gray-800 truncate">
                      {post.title}
                    </h3>
                    <span
                      className={`shrink-0 text-[10px] font-semibold rounded px-1.5 py-0.5 ${REVIEW_TYPE_BADGE[post.reviewType].className}`}
                    >
                      {REVIEW_TYPE_BADGE[post.reviewType].label}
                    </span>
                  </div>
                  {post.reviewType === "REVIEW" && post.rating !== null && (
                    <div className="mb-1">
                      <RatingStars rating={post.rating} />
                    </div>
                  )}
                  <p className="text-xs text-blue-600 font-semibold">
                    {post.concertName}
                  </p>
                  <p className="text-xs text-gray-400 mt-1">
                    {formatDateTime(post.createdAt)} · 좋아요 {post.likeCount}
                  </p>
                </div>
              ))}
            </div>
          )}
          {myPostsTotalPages > 1 && (
            <div className="flex items-center justify-center gap-2 mt-8">
              <button
                onClick={() => fetchMyPosts(myPostsPage - 1)}
                disabled={myPostsPage === 0}
                className="px-3 py-2 rounded-lg border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-default"
              >
                이전
              </button>
              {Array.from({ length: myPostsTotalPages }, (_, i) => i).map(
                (page) => (
                  <button
                    key={page}
                    onClick={() => fetchMyPosts(page)}
                    className={`w-10 h-10 rounded-lg border text-sm font-semibold ${
                      myPostsPage === page
                        ? "bg-blue-600 border-blue-600 text-white"
                        : "bg-white border-gray-200 text-gray-600 hover:bg-gray-50"
                    }`}
                  >
                    {page + 1}
                  </button>
                ),
              )}
              <button
                onClick={() => fetchMyPosts(myPostsPage + 1)}
                disabled={myPostsPage >= myPostsTotalPages - 1}
                className="px-3 py-2 rounded-lg border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-default"
              >
                다음
              </button>
            </div>
          )}
        </>
      )}

      {postsSubTab === "bookmarks" && (
        <>
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-bold text-gray-700">북마크한 게시글</h2>
            <span className="text-sm text-gray-400">
              {bookmarksTotalElements}개
            </span>
          </div>
          {bookmarksLoading ? (
            <p className="text-sm text-gray-400 text-center py-10">
              불러오는 중...
            </p>
          ) : bookmarks.length === 0 ? (
            <p className="text-sm text-gray-400 text-center py-10">
              북마크한 게시글이 없습니다.
            </p>
          ) : (
            <div className="space-y-3">
              {bookmarks.map((b) => (
                <div
                  key={b.postId}
                  onClick={() => router.push(`/board/${b.postId}`)}
                  role="button"
                  tabIndex={0}
                  className="flex gap-3 p-4 border border-gray-100 rounded-xl hover:shadow-md hover:border-blue-200 transition cursor-pointer"
                >
                  <div className="relative shrink-0 w-12 h-16 rounded-lg overflow-hidden bg-gradient-to-br from-blue-100 to-indigo-200 flex items-center justify-center">
                    {b.posterUrl ? (
                      <Image
                        fill
                        unoptimized
                        src={getLocalConcertPoster(b.posterUrl)}
                        alt={b.concertName}
                        sizes="48px"
                        className="object-cover"
                      />
                    ) : (
                      <span className="text-[10px] text-gray-400">포스터</span>
                    )}
                  </div>
                  <div className="min-w-0 flex-1">
                    <h3 className="font-semibold text-gray-800 truncate">
                      {b.title}
                    </h3>
                    <p className="text-xs text-blue-600 font-semibold mt-0.5">
                      {b.concertName}
                    </p>
                    <p className="text-xs text-gray-400 mt-1">
                      {b.userName} · {formatDateTime(b.bookmarkedAt)} 북마크
                    </p>
                  </div>
                </div>
              ))}
            </div>
          )}
          {bookmarksTotalPages > 1 && (
            <div className="flex items-center justify-center gap-2 mt-8">
              <button
                onClick={() => fetchBookmarks(bookmarksPage - 1)}
                disabled={bookmarksPage === 0}
                className="px-3 py-2 rounded-lg border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-default"
              >
                이전
              </button>
              {Array.from({ length: bookmarksTotalPages }, (_, i) => i).map(
                (page) => (
                  <button
                    key={page}
                    onClick={() => fetchBookmarks(page)}
                    className={`w-10 h-10 rounded-lg border text-sm font-semibold ${
                      bookmarksPage === page
                        ? "bg-blue-600 border-blue-600 text-white"
                        : "bg-white border-gray-200 text-gray-600 hover:bg-gray-50"
                    }`}
                  >
                    {page + 1}
                  </button>
                ),
              )}
              <button
                onClick={() => fetchBookmarks(bookmarksPage + 1)}
                disabled={bookmarksPage >= bookmarksTotalPages - 1}
                className="px-3 py-2 rounded-lg border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-default"
              >
                다음
              </button>
            </div>
          )}
        </>
      )}

      {postsSubTab === "likes" && (
        <>
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-bold text-gray-700">좋아요한 게시글</h2>
            <span className="text-sm text-gray-400">
              {likesTotalElements}개
            </span>
          </div>
          {likesLoading ? (
            <p className="text-sm text-gray-400 text-center py-10">
              불러오는 중...
            </p>
          ) : likes.length === 0 ? (
            <p className="text-sm text-gray-400 text-center py-10">
              좋아요한 게시글이 없습니다.
            </p>
          ) : (
            <div className="space-y-3">
              {likes.map((l) => (
                <div
                  key={l.postId}
                  onClick={() => router.push(`/board/${l.postId}`)}
                  role="button"
                  tabIndex={0}
                  className="flex gap-3 p-4 border border-gray-100 rounded-xl hover:shadow-md hover:border-blue-200 transition cursor-pointer"
                >
                  <div className="relative shrink-0 w-12 h-16 rounded-lg overflow-hidden bg-gradient-to-br from-blue-100 to-indigo-200 flex items-center justify-center">
                    {l.posterUrl ? (
                      <Image
                        fill
                        unoptimized
                        src={getLocalConcertPoster(l.posterUrl)}
                        alt={l.concertName}
                        sizes="48px"
                        className="object-cover"
                      />
                    ) : (
                      <span className="text-[10px] text-gray-400">포스터</span>
                    )}
                  </div>
                  <div className="min-w-0 flex-1">
                    <h3 className="font-semibold text-gray-800 truncate">
                      {l.title}
                    </h3>
                    <p className="text-xs text-blue-600 font-semibold mt-0.5">
                      {l.concertName}
                    </p>
                    <p className="text-xs text-gray-400 mt-1">
                      {l.userName} · {formatDateTime(l.likedAt)} 좋아요
                    </p>
                  </div>
                </div>
              ))}
            </div>
          )}
          {likesTotalPages > 1 && (
            <div className="flex items-center justify-center gap-2 mt-8">
              <button
                onClick={() => fetchLikes(likesPage - 1)}
                disabled={likesPage === 0}
                className="px-3 py-2 rounded-lg border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-default"
              >
                이전
              </button>
              {Array.from({ length: likesTotalPages }, (_, i) => i).map(
                (page) => (
                  <button
                    key={page}
                    onClick={() => fetchLikes(page)}
                    className={`w-10 h-10 rounded-lg border text-sm font-semibold ${
                      likesPage === page
                        ? "bg-blue-600 border-blue-600 text-white"
                        : "bg-white border-gray-200 text-gray-600 hover:bg-gray-50"
                    }`}
                  >
                    {page + 1}
                  </button>
                ),
              )}
              <button
                onClick={() => fetchLikes(likesPage + 1)}
                disabled={likesPage >= likesTotalPages - 1}
                className="px-3 py-2 rounded-lg border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-default"
              >
                다음
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
