import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// k6 부하 테스트 옵션 (Redisson Pub/Sub 분산 락 고동시성 성능 검증)
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

export default function () {
  // Redisson Pub/Sub 락 및 I/O 부하 테스트
  const res = http.get(`${BASE_URL}/api/v1/concerts`, {
    headers: {
      'X-Test-Delay': '50', // 50ms I/O 및 분산 락 경합 시뮬레이션
    },
  });

  check(res, {
    'status is 200': (r) => r.status === 200,
  });
  sleep(0.1);
}
