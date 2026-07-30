"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { apiFetch, decodeToken, ApiError } from "@/lib/api";

interface EligibleConcert {
  concertId: number;
  concertTitle: string;
  posterUrl: string | null;
}

export default function BoardWritePage() {
  const router = useRouter();
  const [concerts, setConcerts] = useState<EligibleConcert[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedId, setSelectedId] = useState<number | "">("");
  const [form, setForm] = useState({ title: "", content: "" });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!decodeToken()) {
      router.replace("/login");
      return;
    }
    apiFetch<EligibleConcert[]>("/reviews/eligible-concerts")
      .then((res) => setConcerts(res.data))
      .catch(() => setConcerts([]))
      .finally(() => setLoading(false));
  }, [router]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedId) {
      setError("콘서트를 선택해주세요.");
      return;
    }
    if (!form.title.trim() || !form.content.trim()) {
      setError("제목과 내용을 입력해주세요.");
      return;
    }
    setSubmitting(true);
    setError("");
    try {
      await apiFetch(`/concerts/${selectedId}/reviews`, {
        method: "POST",
        body: JSON.stringify(form),
      });
      router.push("/board");
    } catch (e) {
      if (e instanceof ApiError) {
        setError(e.message);
      } else {
        setError("오류가 발생했습니다. 다시 시도해주세요.");
      }
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <p className="text-gray-400">불러오는 중...</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-10">
      <div className="max-w-2xl mx-auto px-4">
        <h1 className="text-2xl font-bold text-gray-800 mb-8">후기 작성</h1>

        {concerts.length === 0 ? (
          <div className="bg-white rounded-2xl shadow-sm p-10 text-center">
            <p className="text-gray-500 text-sm leading-relaxed">
              최근 6개월 이내에 관람한 콘서트가 없습니다.
            </p>
            <button
              onClick={() => router.push("/board")}
              className="mt-6 px-5 py-2 text-sm text-blue-600 border border-blue-300 rounded-lg hover:bg-blue-50 transition"
            >
              게시판으로 돌아가기
            </button>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="bg-white rounded-2xl shadow-sm p-8 space-y-6">
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-2">
                콘서트 선택
              </label>
              <select
                value={selectedId}
                onChange={(e) => setSelectedId(Number(e.target.value) || "")}
                className="w-full border border-gray-200 rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-300 bg-white"
              >
                <option value="">콘서트를 선택하세요</option>
                {concerts.map((c) => (
                  <option key={c.concertId} value={c.concertId}>
                    {c.concertTitle}
                  </option>
                ))}
              </select>
            </div>

            {selectedId !== "" && (
              <>
                <div>
                  <label className="block text-sm font-semibold text-gray-700 mb-2">
                    제목
                  </label>
                  <input
                    type="text"
                    placeholder="제목을 입력하세요 (최대 100자)"
                    maxLength={100}
                    value={form.title}
                    onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
                    className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-300"
                  />
                </div>

                <div>
                  <label className="block text-sm font-semibold text-gray-700 mb-2">
                    내용
                  </label>
                  <textarea
                    placeholder="후기 내용을 입력하세요 (최대 2000자)"
                    maxLength={2000}
                    rows={8}
                    value={form.content}
                    onChange={(e) => setForm((f) => ({ ...f, content: e.target.value }))}
                    className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-300 resize-none"
                  />
                </div>
              </>
            )}

            {error && <p className="text-red-500 text-sm">{error}</p>}

            <div className="flex gap-3">
              <button
                type="submit"
                disabled={submitting || selectedId === ""}
                className="px-6 py-2.5 bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white text-sm font-semibold rounded-lg transition"
              >
                {submitting ? "등록 중..." : "후기 등록"}
              </button>
              <button
                type="button"
                onClick={() => router.push("/board")}
                className="px-6 py-2.5 bg-gray-100 hover:bg-gray-200 text-gray-600 text-sm font-semibold rounded-lg transition"
              >
                취소
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
