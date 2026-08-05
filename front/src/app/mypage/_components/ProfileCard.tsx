"use client";

import { useState, useRef } from "react";
import Image from "next/image";
import { Camera } from "lucide-react";
import {
  apiFetch,
  BASE_URL,
  getAccessToken,
  setAccessToken,
} from "@/lib/api";
import { showAlert, showConfirm, showSuccess, showError } from "@/lib/alert";
import PasswordStrengthMeter from "@/app/components/PasswordStrengthMeter";
import { SocialBadge } from "./SocialBadge";
import type { MyPageData } from "./types";

interface ProfileCardProps {
  data: MyPageData;
  onDataUpdate: (name: string, email: string) => void;
}

export function ProfileCard({ data, onDataUpdate }: ProfileCardProps) {
  const isSocialLogin = data.loginType !== "NORMAL";

  const [isEditing, setIsEditing] = useState(false);
  const [editName, setEditName] = useState("");
  const [editEmail, setEditEmail] = useState("");
  const [editPassword, setEditPassword] = useState("");
  const [editPasswordCheck, setEditPasswordCheck] = useState("");
  const [isSavingProfile, setIsSavingProfile] = useState(false);

  const fileInputRef = useRef<HTMLInputElement>(null);
  const [profilePreviewUrl, setProfilePreviewUrl] = useState<string | null>(null);
  const [selectedProfileFile, setSelectedProfileFile] = useState<File | null>(null);
  const [isUploadingProfile, setIsUploadingProfile] = useState(false);
  const [profileCacheKey, setProfileCacheKey] = useState(() => Date.now());
  const [profileImgError, setProfileImgError] = useState(false);

  const startEditing = () => {
    setEditName(data.name);
    setEditEmail(data.email);
    setEditPassword("");
    setEditPasswordCheck("");
    setIsEditing(true);
  };

  const cancelEditing = () => {
    setIsEditing(false);
    setEditPassword("");
    setEditPasswordCheck("");
  };

  const handleSaveProfile = async () => {
    if (editName.trim() === "") {
      showAlert("이름을 입력해주세요.");
      return;
    }
    if (editName.includes(" ")) {
      showAlert("이름에 공백을 포함할 수 없습니다.");
      return;
    }
    if (editEmail.trim() === "") {
      showAlert("이메일을 입력해주세요.");
      return;
    }
    if (editPassword !== "") {
      if (editPassword.length < 8) {
        showAlert("비밀번호는 8자 이상이어야 합니다.");
        return;
      }
      if (editPassword !== editPasswordCheck) {
        showAlert("새 비밀번호가 일치하지 않습니다.");
        return;
      }
    }

    setIsSavingProfile(true);
    try {
      const body: Record<string, string> = {
        name: editName,
        email: editEmail,
      };
      if (editPassword !== "") body.password = editPassword;
      await apiFetch("/users/me", {
        method: "PATCH",
        body: JSON.stringify(body),
      });
      onDataUpdate(editName, editEmail);
      setIsEditing(false);
      setEditPassword("");
      setEditPasswordCheck("");
      showSuccess("정보가 수정되었습니다.");
    } catch (e) {
      showError(
        e instanceof Error ? e.message : "정보 수정 중 오류가 발생했습니다.",
      );
    } finally {
      setIsSavingProfile(false);
    }
  };

  const handleProfileFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    e.target.value = "";
    const ALLOWED = ["image/jpeg", "image/png", "image/webp"];
    if (!ALLOWED.includes(file.type)) {
      showAlert("jpg, jpeg, png, webp 파일만 업로드할 수 있습니다.");
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      showAlert("파일 크기는 5MB 이하여야 합니다.");
      return;
    }
    setSelectedProfileFile(file);
    setProfilePreviewUrl(URL.createObjectURL(file));
  };

  const handleUploadProfileImage = async () => {
    if (!selectedProfileFile) return;
    const formData = new FormData();
    formData.append("file", selectedProfileFile);
    setIsUploadingProfile(true);
    try {
      const token = getAccessToken();
      const res = await fetch(`${BASE_URL}/api/v1/users/me/profile-image`, {
        method: "POST",
        credentials: "include",
        headers: token ? { Authorization: `Bearer ${token}` } : {},
        body: formData,
      });
      if (!res.ok) {
        const json = await res.json().catch(() => ({}));
        throw new Error(
          (json as { msg?: string }).msg || "업로드에 실패했습니다.",
        );
      }
      const newToken = res.headers.get("Authorization");
      if (newToken?.startsWith("Bearer ")) setAccessToken(newToken.slice(7));
      if (profilePreviewUrl) URL.revokeObjectURL(profilePreviewUrl);
      setProfilePreviewUrl(null);
      setSelectedProfileFile(null);
      setProfileImgError(false);
      setProfileCacheKey(Date.now());
      window.dispatchEvent(new Event("profile-image-changed"));
      showSuccess("프로필 사진이 변경되었습니다.");
    } catch (e) {
      showError(e instanceof Error ? e.message : "업로드에 실패했습니다.");
    } finally {
      setIsUploadingProfile(false);
    }
  };

  const handleDeleteProfileImage = async () => {
    const confirmed = await showConfirm(
      "프로필 사진을 기본 이미지로 변경하시겠어요?",
      { title: "기본 이미지로 변경", confirmText: "변경", cancelText: "취소" },
    );
    if (!confirmed) return;
    try {
      await apiFetch("/users/me/profile-image", { method: "DELETE" });
      if (profilePreviewUrl) URL.revokeObjectURL(profilePreviewUrl);
      setProfilePreviewUrl(null);
      setSelectedProfileFile(null);
      setProfileImgError(true);
      window.dispatchEvent(new Event("profile-image-changed"));
      showSuccess("기본 이미지로 변경되었습니다.");
    } catch (e) {
      showError(e instanceof Error ? e.message : "삭제에 실패했습니다.");
    }
  };

  const cancelProfileEdit = () => {
    if (profilePreviewUrl) URL.revokeObjectURL(profilePreviewUrl);
    setProfilePreviewUrl(null);
    setSelectedProfileFile(null);
  };

  return (
    <div className="bg-white rounded-2xl shadow-sm p-8 mb-6">
      <div className="flex flex-col items-center mb-6">
        <div className="relative w-20 h-20 rounded-full overflow-hidden border-2 border-gray-100 mb-2">
          <Image
            src={
              profilePreviewUrl ||
              (!data.profileImageUrl || profileImgError
                ? "/default-avatar.svg"
                : `${data.profileImageUrl}?t=${profileCacheKey}`)
            }
            alt="프로필 사진"
            fill
            unoptimized
            onError={() => setProfileImgError(true)}
            className="object-cover"
          />
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            className="absolute inset-0 bg-black/30 flex items-center justify-center opacity-0 hover:opacity-100 transition"
            aria-label="프로필 사진 변경"
          >
            <Camera size={18} className="text-white" />
          </button>
        </div>
        <input
          ref={fileInputRef}
          type="file"
          accept=".jpg,.jpeg,.png,.webp"
          className="hidden"
          onChange={handleProfileFileSelect}
        />
        {selectedProfileFile ? (
          <div className="flex gap-2 mt-1">
            <button
              type="button"
              onClick={cancelProfileEdit}
              className="text-xs text-gray-500 hover:text-gray-700 border border-gray-200 px-3 py-1 rounded-lg transition"
            >
              취소
            </button>
            <button
              type="button"
              onClick={handleUploadProfileImage}
              disabled={isUploadingProfile}
              className="text-xs bg-blue-600 hover:bg-blue-700 text-white px-3 py-1 rounded-lg transition disabled:opacity-50"
            >
              {isUploadingProfile ? "저장 중..." : "저장"}
            </button>
          </div>
        ) : (
          <button
            type="button"
            onClick={handleDeleteProfileImage}
            className="text-xs text-gray-400 hover:text-gray-600 mt-1 transition"
          >
            기본 이미지로 변경
          </button>
        )}
      </div>

      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg font-bold text-gray-700">내 정보</h2>
        {!isEditing && (
          <button
            onClick={startEditing}
            className="text-xs text-blue-600 hover:text-blue-700 border border-blue-200 hover:border-blue-300 px-3 py-1 rounded-lg transition"
          >
            정보 수정
          </button>
        )}
      </div>

      {isEditing ? (
        <div className="space-y-3">
          <div>
            <label className="block text-xs text-gray-400 mb-1">이름</label>
            <input
              type="text"
              value={editName}
              onChange={(e) => setEditName(e.target.value)}
              className="w-full p-2.5 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400"
            />
          </div>
          <div>
            <label className="block text-xs text-gray-400 mb-1">이메일</label>
            <input
              type="email"
              value={editEmail}
              onChange={(e) => setEditEmail(e.target.value)}
              className="w-full p-2.5 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400"
            />
          </div>
          {!isSocialLogin && (
            <>
              <div>
                <label className="block text-xs text-gray-400 mb-1">
                  새 비밀번호 (변경 시에만 입력)
                </label>
                <input
                  type="password"
                  value={editPassword}
                  onChange={(e) => setEditPassword(e.target.value)}
                  placeholder="8자 이상"
                  className="w-full p-2.5 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400"
                />
                <PasswordStrengthMeter password={editPassword} />
              </div>
              {editPassword !== "" && (
                <div>
                  <label className="block text-xs text-gray-400 mb-1">
                    새 비밀번호 확인
                  </label>
                  <input
                    type="password"
                    value={editPasswordCheck}
                    onChange={(e) => setEditPasswordCheck(e.target.value)}
                    className="w-full p-2.5 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400"
                  />
                </div>
              )}
            </>
          )}
          <div className="flex gap-2 pt-2">
            <button
              onClick={cancelEditing}
              disabled={isSavingProfile}
              className="flex-1 p-2.5 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-lg font-semibold text-sm transition disabled:opacity-50"
            >
              취소
            </button>
            <button
              onClick={handleSaveProfile}
              disabled={isSavingProfile}
              className="flex-1 p-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-semibold text-sm transition disabled:opacity-50"
            >
              {isSavingProfile ? "저장 중..." : "저장"}
            </button>
          </div>
        </div>
      ) : (
        <div className="space-y-2 text-gray-600">
          <p>
            <span className="inline-block w-20 text-gray-400">이름</span>
            {data.name}
          </p>
          {isSocialLogin ? (
            <p className="flex items-center gap-1.5">
              <span className="inline-block w-20 shrink-0 text-gray-400">
                로그인 방식
              </span>
              <SocialBadge provider={data.loginType} />
            </p>
          ) : (
            <p>
              <span className="inline-block w-20 text-gray-400">아이디</span>
              {data.id}
            </p>
          )}
          <p>
            <span className="inline-block w-20 text-gray-400">이메일</span>
            {data.email}
          </p>
        </div>
      )}
    </div>
  );
}
