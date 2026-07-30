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
    http_req_failed: ['rate<0.05'],   // 500 서버 오류 발생률 5% 미만
    http_req_duration: ['p(95)<500'], // 95% 요청 500ms 이내 처리
  },
};

export default function () {
  // 실제 DB에 존재하는 좌석 포맷 (A-1 ~ A-30 및 B-1 ~ B-30 등)
  const row = String.fromCharCode(65 + (__VU % 5)); // A, B, C, D, E 행
  const seatNum = (__VU % 30) + 1;                  // 1 ~ 30 번
  const seatFormat = `${row}-${seatNum}`;

  const payload = JSON.stringify({
    seatNumber: seatFormat, // DB 실물 포맷 반영
  });

  const headers = {
    'Content-Type': 'application/json',
    'X-Test-Delay': '50', // 50ms I/O 대기 시뮬레이션
  };

  // 시드 데이터 중 가장 나중 날짜(2027-02-24) 회차(Concert 10, Schedule 21) 대상 요청
  const res = http.post(`${BASE_URL}/api/v1/concerts/10/schedules/21/seats/occupy`, payload, { headers });

  // 200(최초 선점 성공) 또는 409(타인 선점 중/경합)는 100% 비즈니스 수용 성공으로 처리
  check(res, {
    'status is 200 or 409': (r) => r.status === 200 || r.status === 409,
  });
  sleep(0.1);
}
