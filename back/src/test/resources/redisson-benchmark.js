import http from 'k6/http';
import { check, sleep } from 'k6';

/**
 * Redisson Pub/Sub 분산 락 실서버 고동시성 k6 부하 테스트
 *
 * [임시 비활성화 항목 - 부하 테스트 전용]
 * 아래 항목은 테스트 목적으로 임시 비활성화되어 있습니다.
 * 테스트 완료 후 커밋 '4a41044' 이전으로 git revert 하여 반드시 복구해야 합니다.
 *
 * 1. QueueInterceptor (대기열 검증):
 *    파일: back/src/main/kotlin/com/back/global/config/WebConfig.kt
 *    변경: addInterceptors 내 queueInterceptor 등록 주석 처리됨
 *
 * ※ 로그인 인증은 setup()에서 JWT 토큰을 발급받아 정상 처리됩니다. (복구 완료)
 */

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// k6 부하 테스트 옵션
export const options = {
  stages: [
    { duration: '10s', target: 100 },  // 10초간 100 VU 상승
    { duration: '30s', target: 500 },  // 30초간 500 VU 동시성 요청 유지
    { duration: '30s', target: 1000 }, // 30초간 1,000 VU 분산 락 동시 경합 부하 테스트
    { duration: '10s', target: 0 },    // 부하 감소
  ],
  thresholds: {
    http_req_failed: ['rate<0.05'],   // 500 서버 오류 발생률 5% 미만
    http_req_duration: ['p(95)<500'], // 95% 요청 500ms 이내 처리
  },
};

// 테스트 시작 시 1회 수행: test/12345678 계정으로 로그인 후 JWT 토큰 발급
export function setup() {
  const loginRes = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ id: 'test', password: '12345678' }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  const token = loginRes.headers['Authorization'] || '';

  if (!token) {
    console.error(`로그인 실패! status=${loginRes.status}, body=${loginRes.body}`);
  } else {
    console.log(`로그인 성공! JWT 토큰 발급 완료`);
  }

  return { token };
}

// 실제 Redisson 분산 락(SeatOccupyManager)을 경유하는 좌석 선점 API에 1,000 VU 동시 경합 부하 주입
export default function (data) {
  // 실제 DB에 존재하는 좌석 포맷 (A-1 ~ E-30)
  const row = String.fromCharCode(65 + (__VU % 5)); // A, B, C, D, E 행
  const seatNum = (__VU % 30) + 1;                  // 1 ~ 30 번
  const seatFormat = `${row}-${seatNum}`;

  const payload = JSON.stringify({ seatNumber: seatFormat });

  const headers = {
    'Content-Type': 'application/json',
    'Authorization': data.token,        // setup()에서 발급받은 JWT 토큰
    'X-Test-Delay': '50',               // 50ms I/O 대기 시뮬레이션 (TestDelayFilter)
  };

  // 시드 데이터 중 가장 나중 날짜(2027-02-24) 미래 회차 대상 (Concert 10, Schedule 21)
  const res = http.post(`${BASE_URL}/api/v1/concerts/10/schedules/21/seats/occupy`, payload, { headers });

  // 200(최초 선점 성공) / 409(타인 선점 중 경합) 모두 정상 비즈니스 응답으로 통과 처리
  check(res, {
    'status is 200 or 409': (r) => r.status === 200 || r.status === 409,
  });
  sleep(0.1);
}
