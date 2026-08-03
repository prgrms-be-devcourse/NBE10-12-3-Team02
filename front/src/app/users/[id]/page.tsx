"use client";

import { useState, useEffect, use } from "react";
import { useRouter } from "next/navigation";
import Image from "next/image";
import Link from "next/link";
import { apiFetch, decodeToken, restoreSession, ApiError } from "@/lib/api";
import { showAlert, showError } from "@/lib/alert";

interface UserProfile {
  userId: number;
  name: string;
  profileImgUrl: string;
  isFollowing: boolean | null;
}

export default function UserProfilePage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const router = useRouter();

  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [imgError, setImgError] = useState(false);
  const [followPending, setFollowPending] = useState(false);
  const [me, setMe] = useState<{ id: number; name: string } | null>(null);

  useEffect(() => {
    const syncAuth = () => setMe(decodeToken());
    syncAuth();
    window.addEventListener("auth-changed", syncAuth);
    return () => window.removeEventListener("auth-changed", syncAuth);
  }, []);

  useEffect(() => {
    restoreSession().then(() => {
      setLoading(true);
      setNotFound(false);
      setImgError(false);
      apiFetch<UserProfile>(`/users/${id}`)
        .then((res) => setProfile(res.data))
        .catch(() => setNotFound(true))
        .finally(() => setLoading(false));
    });
  }, [id]);

  const handleFollowToggle = async () => {
    if (!profile) return;
    setFollowPending(true);
    try {
      if (profile.isFollowing) {
        await apiFetch(`/follows/${profile.userId}`, { method: "DELETE" });
        setProfile({ ...profile, isFollowing: false });
      } else {
        await apiFetch(`/follows/${profile.userId}`, { method: "POST" });
        setProfile({ ...profile, isFollowing: true });
      }
    } catch (e) {
      showError(
        e instanceof ApiError ? e.message : "처리 중 오류가 발생했습니다.",
      );
    } finally {
      setFollowPending(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <p className="text-gray-400">불러오는 중...</p>
      </div>
    );
  }

  if (notFound || !profile) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <p className="text-red-400">존재하지 않는 사용자입니다.</p>
      </div>
    );
  }

  const isSelf = me !== null && me.id === profile.userId;

  return (
    <div className="min-h-screen bg-gray-50 p-10">
      <div className="max-w-md mx-auto">
        <div className="bg-white rounded-2xl shadow-sm p-8">
          <div className="flex flex-col items-center mb-6">
            <div className="relative w-20 h-20 rounded-full overflow-hidden border-2 border-gray-100 mb-3">
              <Image
                src={imgError ? "/default-avatar.svg" : profile.profileImgUrl}
                alt="프로필 사진"
                fill
                unoptimized
                onError={() => setImgError(true)}
                className="object-cover"
              />
            </div>
            <h1 className="text-lg font-bold text-gray-800">{profile.name}</h1>
          </div>

          {isSelf ? (
            <Link
              href="/mypage"
              className="block text-center px-4 py-2.5 bg-gray-100 hover:bg-gray-200 text-gray-700 text-sm font-semibold rounded-lg transition"
            >
              마이페이지로 이동
            </Link>
          ) : me ? (
            <button
              onClick={handleFollowToggle}
              disabled={followPending}
              className={`w-full px-4 py-2.5 text-sm font-semibold rounded-lg transition disabled:opacity-50 ${
                profile.isFollowing
                  ? "bg-gray-100 text-gray-600 hover:bg-gray-200"
                  : "bg-blue-600 text-white hover:bg-blue-700"
              }`}
            >
              {followPending
                ? "처리 중..."
                : profile.isFollowing
                  ? "팔로우 중"
                  : "팔로우"}
            </button>
          ) : (
            <div className="text-center">
              <p className="text-sm text-gray-400 mb-3">
                팔로우하려면 로그인이 필요합니다.
              </p>
              <button
                onClick={async () => {
                  await showAlert("로그인이 필요합니다.");
                  router.push("/login");
                }}
                className="px-4 py-2.5 text-sm text-blue-600 border border-blue-300 rounded-lg hover:bg-blue-50 transition"
              >
                로그인
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
