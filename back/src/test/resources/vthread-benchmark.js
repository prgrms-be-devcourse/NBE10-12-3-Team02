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

// GET /api/v1/concerts 공개 API 전용 (X-Test-Delay 50ms 운영 I/O 대기 부하 주입)
export default function () {
  const res = http.get('http://localhost:8080/api/v1/concerts', {
    headers: {
      'X-Test-Delay': '50', // 50ms 운영 환경 I/O 대기 시뮬레이션
    },
  });

  check(res, {
    'status is 200': (r) => r.status === 200,
  });
  sleep(0.1);
}
