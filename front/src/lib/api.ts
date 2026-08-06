export const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL;

export interface RsData<T> {
  resultCode: string;
  msg: string;
  data: T;
}

let accessToken: string | null = null;
let csrfToken: string | null = null;
let csrfTokenPromise: Promise<string> | null = null;

const CSRF_PROTECTED_PATHS = new Set([
  "/auth/refresh",
  "/auth/logout",
  "/auth/restore",
]);

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
    const base64 = accessToken
      .split(".")[1]
      .replace(/-/g, "+")
      .replace(/_/g, "/");
    const binary = atob(base64);
    const bytes = Uint8Array.from(binary, (c) => c.charCodeAt(0));
    const json = JSON.parse(new TextDecoder("utf-8").decode(bytes));
    return { id: json.id, name: json.name };
  } catch {
    return null;
  }
}

const DEFAULT_ERROR_MESSAGE =
  "요청 처리 중 문제가 발생했습니다. 잠시 후 다시 시도해주세요.";

// 일반 Error 대신 이걸 던져서, 화면단에서 "어떤 종류의 실패인지"를 문구 대신
// resultCode(서버가 정해둔 코드)로 정확히 구분할 수 있게 한다.
export class ApiError extends Error {
  resultCode?: string;

  constructor(message: string, resultCode?: string) {
    super(message);
    this.name = "ApiError";
    this.resultCode = resultCode;
  }
}

// 여러 요청이 동시에 401을 받아도 refresh는 한 번만 실행되도록 하는 잠금장치
let refreshPromise: Promise<boolean> | null = null;

async function getCsrfToken(): Promise<string> {
  if (csrfToken) return csrfToken;

  if (!csrfTokenPromise) {
    csrfTokenPromise = (async () => {
      const res = await fetch(`${BASE_URL}/api/v1/auth/csrf`, {
        method: "GET",
        credentials: "include",
      });

      if (!res.ok) throw new ApiError("보안 토큰을 발급받지 못했습니다.");

      const body = (await res.json()) as RsData<{ token: string }>;
      csrfToken = body.data.token;
      return csrfToken;
    })().finally(() => {
      csrfTokenPromise = null;
    });
  }

  return csrfTokenPromise;
}

function refreshAccessToken(): Promise<boolean> {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      try {
        const token = await getCsrfToken();
        const res = await fetch(`${BASE_URL}/api/v1/auth/refresh`, {
          method: "POST",
          credentials: "include",
          headers: { "X-XSRF-TOKEN": token },
        });
        if (!res.ok) return false;
        const newAuthHeader = res.headers.get("Authorization");
        if (newAuthHeader?.startsWith("Bearer ")) {
          setAccessToken(newAuthHeader.slice(7));
          return true;
        }
        return false;
      } catch {
        return false;
      } finally {
        refreshPromise = null;
      }
    })();
  }
  return refreshPromise;
}

export async function apiFetch<T>(
  path: string,
  options: RequestInit = {},
  _isRetry = false,
): Promise<RsData<T>> {
  const headers = new Headers();
  headers.set("Content-Type", "application/json");
  if (accessToken) headers.set("Authorization", `Bearer ${accessToken}`);
  if (CSRF_PROTECTED_PATHS.has(path)) {
    headers.set("X-XSRF-TOKEN", await getCsrfToken());
  }
  new Headers(options.headers).forEach((value, key) => headers.set(key, value));

  const res = await fetch(`${BASE_URL}/api/v1${path}`, {
    ...options,
    credentials: "include",
    headers,
  });

  // 304 Not Modified 응답 처리 (ETag 폴링용)
  if (res.status === 304) {
    return {
      resultCode: "304",
      msg: "Not Modified",
      data: null as unknown as T,
    };
  }

  // 토큰이 만료된 채로 요청한 경우(401), 사용자에게 에러를 보여주는 대신
  // 조용히 refresh 후 원래 요청을 한 번만 다시 시도한다.
  if (res.status === 401 && !_isRetry && !path.startsWith("/auth/")) {
    const refreshed = await refreshAccessToken();
    if (refreshed) {
      return apiFetch<T>(path, options, true);
    }
  }

  const newAuthHeader = res.headers.get("Authorization");
  if (newAuthHeader?.startsWith("Bearer ")) {
    setAccessToken(newAuthHeader.slice(7));
  }

  let json: Partial<RsData<T>> & { message?: string } = {};
  try {
    json = await res.json();
  } catch {
    // 서버가 JSON이 아닌 에러 페이지를 내려준 경우 대비
  }

  if (!res.ok) {
    throw new ApiError(
      json.msg || json.message || DEFAULT_ERROR_MESSAGE,
      json.resultCode,
    );
  }

  return json as RsData<T>;
}

export interface AuthRestoreResponse {
  authenticated: boolean;
}

let restorePromise: Promise<boolean> | null = null;

// 웹사이트 최초 진입 또는 새로고침 시 1회 호출.
// 브라우저에 남아있는 refreshToken 쿠키로 로그인 상태를 복구해본다.
// 반환값: 로그인 상태가 복구됐으면 true, 아니면 false
export function restoreSession(): Promise<boolean> {
  if (!restorePromise) {
    restorePromise = (async () => {
      try {
        const res = await apiFetch<AuthRestoreResponse>("/auth/restore", {
          method: "POST",
        });
        return res.data.authenticated;
      } catch {
        return false;
      }
    })();
  }
  return restorePromise;
}
