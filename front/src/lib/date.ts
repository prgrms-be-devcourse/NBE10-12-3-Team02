/** "2026-07-30T14:32:00" → "2026-07-30 14:32" */
export function formatDateTime(iso?: string | null): string {
  if (!iso) return "";
  return iso.slice(0, 16).replace("T", " ");
}
