import http from 'k6/http';
import { check, sleep } from 'k6';

// k6 부하 테스트 옵션 설정 (가상 스레드 대용량 동시성 검증)
export const options = {
  stages: [
    { duration: '10s', target: 100 },  // 10초간 100 VU 상승
    { duration: '30s', target: 500 },  // 30초간 500 VU 동시성 요청 유지
    { duration: '30s', target: 1000 }, // 30초간 1,000 VU 부하 테스트
    { duration: '10s', target: 0 },    // 부하 감소
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],   // 에러율 1% 미만
    http_req_duration: ['p(95)<300'], // 95% 요청 300ms 이내 처리
  },
};

// 1. 테스트 시작 시 1회 수행: 로그인 시도 후 JWT 토큰을 발급받습니다.
export function setup() {
  let token = '';
  try {
    const loginRes = http.post('http://localhost:8080/api/v1/auth/login', JSON.stringify({
      id: 'testuser',
      password: 'password123'
    }), {
      headers: { 'Content-Type': 'application/json' },
    });
    if (loginRes.status === 200) {
      token = loginRes.headers['Authorization'] || '';
    }
  } catch (e) {
    // 로그인 안 되어도 공개 API 테스트가 진행되도록 예외 처리
  }
  return { token };
}

// 2. JWT 토큰 + X-Test-Delay(50ms 운영 I/O 대기) 헤더를 조합하여 요청을 전송합니다.
export default function (data) {
  const headers = {
    'X-Test-Delay': '50', // 50ms 운영 환경 I/O 대기 부하 헤더
  };

  if (data.token) {
    headers['Authorization'] = data.token;
  }

  // 1) 공개 목록 API 테스트
  const res = http.get('http://localhost:8080/api/v1/concerts', { headers });

  // 2) 만약 인가된 무거운 좌석 선택 API를 테스트하고 싶을 경우 아래 주석 해제:
  // const res = http.get('http://localhost:8080/api/v1/concerts/1/schedules/1/seats', { headers });

  check(res, {
    'status is 200': (r) => r.status === 200,
  });
  sleep(0.1);
}
