"use client";

import { useState } from "react";
import Image from "next/image";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";
import { showConfirm, showSuccess, showError } from "@/lib/alert";
import { getLocalConcertPoster } from "@/lib/concertDetailImages";
import type { TicketGroupInfo, TicketSummary, LegacyTicketSummary } from "./types";

type StatusFilter = "all" | "valid" | "canceled";

interface TicketsSectionProps {
  ticketGroups: TicketGroupInfo[];
  onCancelSuccess: (updated: TicketGroupInfo[]) => void;
}

function isTicketValid(t: TicketSummary): boolean {
  const legacy = t as LegacyTicketSummary;
  if (t.isValid !== undefined) return t.isValid;
  if (legacy.valid !== undefined) return legacy.valid;
  return true;
}

function splitIntoReservations(group: TicketGroupInfo): TicketGroupInfo[] {
  const map = new Map<string, TicketSummary[]>();
  for (const ticket of group.tickets) {
    const key = ticket.groupToken || `ticket-${ticket.ticketId}`;
    if (!map.has(key)) map.set(key, []);
    map.get(key)!.push(ticket);
  }
  return Array.from(map.values()).map((tickets) => ({
    ...group,
    tickets,
    totalPrice: tickets.reduce((sum, t) => sum + t.ticketPrice, 0),
  }));
}

const TICKETS_PER_PAGE = 5;

export function TicketsSection({
  ticketGroups: rawGroups,
  onCancelSuccess,
}: TicketsSectionProps) {
  const router = useRouter();
  const [currentPage, setCurrentPage] = useState(1);
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("all");
  const [cancelingKey, setCancelingKey] = useState<number | null>(null);

  const reservations = rawGroups.flatMap(splitIntoReservations);
  const sortedGroups = [...reservations].sort((a, b) => {
    const maxA = Math.max(...a.tickets.map((t) => t.ticketId));
    const maxB = Math.max(...b.tickets.map((t) => t.ticketId));
    return maxB - maxA;
  });

  const filteredGroups = sortedGroups.filter((group) => {
    const allInvalid = group.tickets.every((t) => !isTicketValid(t));
    if (statusFilter === "valid") return !allInvalid;
    if (statusFilter === "canceled") return allInvalid;
    return true;
  });

  const totalPages = Math.ceil(filteredGroups.length / TICKETS_PER_PAGE);
  const pagedGroups = filteredGroups.slice(
    (currentPage - 1) * TICKETS_PER_PAGE,
    currentPage * TICKETS_PER_PAGE,
  );

  const handleFilterChange = (filter: StatusFilter) => {
    setStatusFilter(filter);
    setCurrentPage(1);
  };

  const goToTicketDetail = (group: TicketGroupInfo) => {
    const encoded = encodeURIComponent(JSON.stringify(group));
    router.push(`/mypage/tickets?group=${encoded}`);
  };

  const handleCancelGroup = async (group: TicketGroupInfo) => {
    const validTickets = group.tickets.filter((t) => t.isValid);
    const confirmed = await showConfirm(
      validTickets.length > 1
        ? `좌석 ${validTickets.length}매를 모두 취소하시겠어요?`
        : "예매를 취소하시겠어요?",
      {
        title: "예매 취소",
        confirmText: "취소하기",
        cancelText: "돌아가기",
        danger: true,
      },
    );
    if (!confirmed) return;

    setCancelingKey(group.tickets[0].ticketId);
    try {
      await Promise.all(
        validTickets.map((t) =>
          apiFetch(`/tickets/cancel/${t.ticketId}`, { method: "PATCH" }),
        ),
      );
      const canceledIds = new Set(validTickets.map((t) => t.ticketId));
      const updated = rawGroups.map((g) =>
        g.scheduleId === group.scheduleId
          ? {
              ...g,
              tickets: g.tickets.map((t) =>
                canceledIds.has(t.ticketId) ? { ...t, isValid: false } : t,
              ),
            }
          : g,
      );
      onCancelSuccess(updated);
      await showSuccess("예매가 취소되었습니다.");
    } catch (e) {
      showError(
        e instanceof Error ? e.message : "취소 처리 중 오류가 발생했습니다.",
      );
    } finally {
      setCancelingKey(null);
    }
  };

  return (
    <div className="bg-white rounded-2xl shadow-sm p-8">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg font-bold text-gray-700">내 티켓</h2>
        <span className="text-sm text-gray-400">
          {filteredGroups.reduce((sum, g) => sum + g.tickets.length, 0)}개의
          티켓
        </span>
      </div>

      <div className="flex gap-2 mb-6">
        {(
          [
            { key: "all", label: "전체" },
            { key: "valid", label: "예매완료" },
            { key: "canceled", label: "취소됨" },
          ] as const
        ).map((f) => (
          <button
            key={f.key}
            onClick={() => handleFilterChange(f.key)}
            className={`px-3 py-1.5 rounded-lg text-sm font-semibold border transition ${
              statusFilter === f.key
                ? "bg-blue-600 text-white border-blue-600"
                : "bg-white text-gray-600 border-gray-200 hover:border-blue-400"
            }`}
          >
            {f.label}
          </button>
        ))}
      </div>

      {filteredGroups.length === 0 ? (
        <p className="text-sm text-gray-400 text-center py-10">
          해당 조건의 티켓이 없습니다.
        </p>
      ) : (
        <div className="space-y-6">
          {pagedGroups.map((group) => {
            const allInvalid = group.tickets.every((t) => !isTicketValid(t));
            const statusLabel = allInvalid ? "취소됨" : "예매완료";
            const statusClass = allInvalid
              ? "bg-gray-100 text-gray-400"
              : "bg-green-100 text-green-700";
            return (
              <div
                key={group.tickets[0].ticketId}
                onClick={() => goToTicketDetail(group)}
                role="button"
                tabIndex={0}
                className="w-full flex shadow-md rounded-2xl overflow-hidden text-left hover:shadow-lg transition cursor-pointer"
              >
                <div className="flex-shrink-0 w-36 relative aspect-[3/4] bg-gradient-to-br from-blue-200 to-indigo-300 flex items-center justify-center text-white font-bold text-sm overflow-hidden">
                  {group.posterUrl ? (
                    <Image
                      fill
                      unoptimized
                      src={getLocalConcertPoster(group.posterUrl)}
                      alt={group.concertName}
                      sizes="144px"
                      className="object-cover"
                    />
                  ) : (
                    "포스터"
                  )}
                </div>
                <div className="border-l-2 border-dashed border-gray-200 my-4" />
                <div className="flex-1 bg-white p-6">
                  <div className="flex justify-between items-start mb-3">
                    <h3 className="font-bold text-gray-800 text-lg">
                      {group.concertName}
                    </h3>
                    <div className="flex items-center gap-2">
                      {!allInvalid && (
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            handleCancelGroup(group);
                          }}
                          disabled={
                            cancelingKey === group.tickets[0].ticketId
                          }
                          className="whitespace-nowrap text-xs text-gray-400 hover:text-red-500 border border-gray-200 hover:border-red-300 px-3 py-1 rounded-lg transition disabled:opacity-50"
                        >
                          {cancelingKey === group.tickets[0].ticketId
                            ? "취소 중..."
                            : "예매 취소"}
                        </button>
                      )}
                      <span
                        className={`whitespace-nowrap px-2 py-1 text-xs rounded-full font-semibold ${statusClass}`}
                      >
                        {statusLabel}
                      </span>
                    </div>
                  </div>
                  <div className="space-y-1 text-sm text-gray-500">
                    <p>
                      <span className="inline-block w-20 text-gray-400">
                        좌석
                      </span>
                      {group.tickets.length}매 (
                      {group.tickets.map((t) => t.seatNumber).join(", ")})
                    </p>
                    <p>
                      <span className="inline-block w-20 text-gray-400">
                        공연기간
                      </span>
                      {group.startDate} ~ {group.endDate}
                    </p>
                    <p>
                      <span className="inline-block w-20 text-gray-400">
                        결제금액
                      </span>
                      <span className="text-blue-600 font-bold">
                        {group.totalPrice.toLocaleString()}원
                      </span>
                    </p>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 mt-8">
          <button
            onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
            disabled={currentPage === 1}
            className="px-3 py-2 rounded-lg border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-default"
          >
            이전
          </button>
          {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
            <button
              key={page}
              onClick={() => setCurrentPage(page)}
              className={`w-10 h-10 rounded-lg border text-sm font-semibold ${
                currentPage === page
                  ? "bg-blue-600 border-blue-600 text-white"
                  : "bg-white border-gray-200 text-gray-600 hover:bg-gray-50"
              }`}
            >
              {page}
            </button>
          ))}
          <button
            onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
            disabled={currentPage === totalPages}
            className="px-3 py-2 rounded-lg border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-default"
          >
            다음
          </button>
        </div>
      )}
    </div>
  );
}
