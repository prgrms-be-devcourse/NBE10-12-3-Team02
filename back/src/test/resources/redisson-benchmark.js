import http from 'k6/http';
import { check, sleep } from 'k6';

/**
 * Redisson Pub/Sub 분산 락 실서버 고동시성 k6 부하 테스트 스크립트
 *
 * [📌 추후 부하 테스트 재실행 시 사전 작업 가이드]
 * 이 스크립트로 백엔드 부하 테스트를 재실행할 경우, 아래 항목을 순서대로 확인/조정해야 합니다.
 *
 * 1. QueueInterceptor (대기열 검증) 임시 비활성화:
 *    - 대상 파일: back/src/main/kotlin/com/back/global/config/WebConfig.kt
 *    - 작업: addInterceptors() 메서드 내부의 queueInterceptor 등록 코드를 주석 처리합니다.
 *    - 이유: k6 동시성 주입 시 대기열 토큰 미발급으로 인한 403 Forbidden 즉시 반사 방지.
 *
 * 2. 테스트용 계정 준비:
 *    - setup() 함수에서 id: 'test', password: '12345678' 계정으로 자동 로그인합니다.
 *    - DB(H2)에 해당 계정이 존재하지 않으면 401 Unauthorized 오류가 발생합니다.
 *
 * 3. 예매 가능 미래 회차 ID 및 좌석 번호 지정:
 *    - 요청 URL: POST /api/v1/concerts/10/schedules/21/seats/occupy (2027-02-24 미래 회차)
 *    - 좌석 포맷: DB 실물 포맷인 `${row}-${seatNum}` (예: A-1 ~ E-30) 지정.
 *
 * 4. 부하 테스트 완료 후 필수 원복:
 *    - WebConfig.kt의 queueInterceptor 주석을 반드시 다시 해제(원복)해야 합니다.
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
