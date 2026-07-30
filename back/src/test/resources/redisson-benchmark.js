import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// k6 부하 테스트 옵션 (Redisson Pub/Sub 분산 락 실서버 고동시성 성능 검증)
export const options = {
  stages: [
    { duration: '10s', target: 100 },  // 10초간 100 VU 상승
    { duration: '30s', target: 500 },  // 30초간 500 VU 동시성 요청 유지
    { duration: '30s', target: 1000 }, // 30초간 1,000 VU 분산 락 동시 경합 부하 테스트
    { duration: '10s', target: 0 },    // 부하 감소
  ],
  thresholds: {
    http_req_failed: ['rate<0.05'],   // 에러율 5% 미만
    http_req_duration: ['p(95)<500'], // 95% 요청 500ms 이내 처리
  },
};

// 1. 테스트 시작 시 1회 수행: 로그인 시도 후 JWT 토큰을 발급받습니다.
export function setup() {
  let token = '';
  try {
    const loginRes = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify({
      id: 'testuser',
      password: 'password123'
    }), {
      headers: { 'Content-Type': 'application/json' },
    });
    if (loginRes.status === 200) {
      token = loginRes.headers['Authorization'] || '';
    }
  } catch (e) {
    // 예외 처리
  }
  return { token };
}

// 2. 실제 백엔드의 Redisson 분산 락(SeatOccupyManager)을 호출하는 실시간 좌석 선점 API 부하 주입
export default function (data) {
  const payload = JSON.stringify({
    seatNumber: `A${__VU}`, // VU 번호마다 서로 다른 좌석 선점 경합 시뮬레이션
  });

  const headers = {
    'Content-Type': 'application/json',
    'X-Test-Delay': '50', // 50ms I/O 대기 시뮬레이션
  };

  if (data.token) {
    headers['Authorization'] = data.token;
  }

  const res = http.post(`${BASE_URL}/api/v1/concerts/1/schedules/1/seats/occupy`, payload, { headers });

  check(res, {
    'status is 200 or 400': (r) => r.status === 200 || r.status === 400 || r.status === 409,
  });
  sleep(0.1);
}
