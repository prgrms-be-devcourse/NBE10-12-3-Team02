const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL;

export interface RsData<T> {
  resultCode: string;
  msg: string;
  data: T;
}

let accessToken: string | null = null;

export function setAccessToken(token: string | null) {
  accessToken = token;
  if (typeof window !== "undefined") {
    window.dispatchEvent(new Event("auth-changed"));
  }
}

export function getAccessToken() {
  return accessToken;
}

export function decodeToken(): { id: number; name: string } | null {
  if (!accessToken) return null;
  try {
    const base64 = accessToken.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    const binary = atob(base64);
    const bytes = Uint8Array.from(binary, (c) => c.charCodeAt(0));
    const json = JSON.parse(new TextDecoder("utf-8").decode(bytes));
    return { id: json.id, name: json.name };
  } catch {
    return null;
  }
}

export async function apiFetch<T>(
  path: string,
  options: RequestInit = {}
): Promise<RsData<T>> {
  const res = await fetch(`${BASE_URL}/api/v1${path}`, {
    ...options,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      ...options.headers,
    },
  });

  const newAuthHeader = res.headers.get("Authorization");
  if (newAuthHeader?.startsWith("Bearer ")) {
    setAccessToken(newAuthHeader.slice(7));
  }

  const json: RsData<T> = await res.json();

  if (!res.ok) {
    throw new Error(json.msg || "요청에 실패했습니다.");
  }

  return json;
}

let restorePromise: Promise<void> | null = null;

// 새로고침 등으로 메모리 토큰이 사라졌을 때, refreshToken 쿠키로 세션 복구 시도
export function restoreSession(): Promise<void> {
  if (!restorePromise) {
    restorePromise = (async () => {
      try {
        await apiFetch("/auth/refresh", { method: "POST" });
      } catch {
        // refreshToken이 없거나 만료됐으면 조용히 비로그인 상태로 둠
      }
    })();
  }
  return restorePromise;
}