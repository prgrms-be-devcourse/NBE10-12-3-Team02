"use client";

import { useState, useEffect, useRef } from "react";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { apiFetch, decodeToken, restoreSession } from "@/lib/api";
import { showAlert } from "@/lib/alert";
import { formatDateTime } from "@/lib/date";
import Pagination from "@/app/components/Pagination";

interface FollowUser {
  userId: number;
  name: string;
  profileImgUrl: string;
  followedAt: string | null;
}

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  last: boolean;
}

type FollowTab = "followings" | "followers";

const PAGE_SIZE = 10;

function FollowUserAvatar({ src, alt }: { src: string; alt: string }) {
  const [error, setError] = useState(false);
  return (
    <div className="relative w-11 h-11 rounded-full overflow-hidden border border-gray-100 shrink-0">
      <Image
        src={error ? "/default-avatar.svg" : src}
        alt={alt}
        fill
        unoptimized
        onError={() => setError(true)}
        className="object-cover"
      />
    </div>
  );
}

export default function MyFollowsPage() {
  const router = useRouter();
  const hasCheckedAuth = useRef(false);

  const [tab, setTab] = useState<FollowTab>("followings");
  const [users, setUsers] = useState<FollowUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const fetchList = async (targetTab: FollowTab, targetPage: number) => {
    setLoading(true);
    try {
      const path =
        targetTab === "followings" ? "/follows/me" : "/follows/me/followers";
      const res = await apiFetch<PageResponse<FollowUser>>(
        `${path}?page=${targetPage}&size=${PAGE_SIZE}`,
      );
      setUsers(res.data.content);
      setPage(res.data.number);
      setTotalPages(res.data.totalPages);
      setTotalElements(res.data.totalElements);
    } catch {
      setUsers([]);
      setTotalPages(0);
      setTotalElements(0);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (hasCheckedAuth.current) return;
    hasCheckedAuth.current = true;

    const init = async () => {
      await restoreSession();
      if (!decodeToken()) {
        await showAlert("로그인이 필요합니다.");
        router.push("/login");
        return;
      }
      fetchList("followings", 0);
    };

    init();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleTabChange = (nextTab: FollowTab) => {
    if (nextTab === tab) return;
    setTab(nextTab);
    fetchList(nextTab, 0);
  };

  const emptyMessage =
    tab === "followings"
      ? "아직 팔로우한 사람이 없습니다."
      : "아직 팔로워가 없습니다.";

  return (
    <div className="min-h-screen bg-gray-50 p-10">
      <div className="max-w-3xl mx-auto">
        <Link
          href="/mypage"
          className="text-sm text-gray-400 hover:text-gray-600 mb-4 inline-block"
        >
          ← 마이페이지로
        </Link>

        <h1 className="text-2xl font-bold text-gray-800 mb-6">팔로우 목록</h1>

        <div className="bg-white rounded-2xl shadow-sm p-8">
          <div className="flex items-center justify-between mb-4">
            <div className="flex gap-2">
              {(
                [
                  { key: "followings", label: "팔로잉" },
                  { key: "followers", label: "팔로워" },
                ] as const
              ).map((t) => (
                <button
                  key={t.key}
                  onClick={() => handleTabChange(t.key)}
                  className={`px-3 py-1.5 rounded-lg text-sm font-semibold border transition ${
                    tab === t.key
                      ? "bg-blue-600 text-white border-blue-600"
                      : "bg-white text-gray-600 border-gray-200 hover:border-blue-400"
                  }`}
                >
                  {t.label}
                </button>
              ))}
            </div>
            <span className="text-sm text-gray-400">{totalElements}명</span>
          </div>

          {loading ? (
            <p className="text-sm text-gray-400 text-center py-10">
              불러오는 중...
            </p>
          ) : users.length === 0 ? (
            <p className="text-sm text-gray-400 text-center py-10">
              {emptyMessage}
            </p>
          ) : (
            <div className="space-y-3">
              {users.map((u) => (
                <Link
                  key={u.userId}
                  href={`/users/${u.userId}`}
                  className="flex items-center gap-3 p-4 border border-gray-100 rounded-xl hover:shadow-md hover:border-blue-200 transition"
                >
                  <FollowUserAvatar src={u.profileImgUrl} alt={u.name} />
                  <div className="min-w-0">
                    <p className="font-semibold text-gray-800 truncate">
                      {u.name}
                    </p>
                    {u.followedAt && (
                      <p className="text-xs text-gray-400 mt-0.5">
                        {formatDateTime(u.followedAt)}부터 팔로우
                      </p>
                    )}
                  </div>
                </Link>
              ))}
            </div>
          )}

          <Pagination
            currentPage={page}
            totalPages={totalPages}
            onPageChange={(p) => fetchList(tab, p)}
            basePage={0}
          />
        </div>
      </div>
    </div>
  );
}
