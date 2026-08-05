# NBE10-12-3-Team02 E2E System Test Package

Playwright 기반 시스템통합 E2E (End-to-End) 테스트 패키지입니다.  
프론트엔드(Next.js)와 백엔드(Spring Boot) 및 Redis/DB 전체 시스템 연동을 8대 도메인 스펙(총 15개 테스트)으로 검증합니다.

---

## 🚀 빠른 시작 (Quick Start)

### 1. 드라이버 설치 (최초 1회)
```bash
cd e2e
pnpm install
npx playwright install chromium
```

### 2. 테스트 실행 (명령어 단 1개로 완전 자동화)
```bash
# 헤드리스 모드로 전체 15개 E2E 테스트 실행 (백엔드 8080 & 프론트엔드 3000 자동 감지/시동)
pnpm test

# 대화형 UI 모드로 디버깅 및 시각적 테스트 실행
pnpm test:ui

# 브라우저 화면 표시(Headed) 모드로 실행
pnpm test:headed

# HTML 테스트 결과 리포트 보기
pnpm test:report
```

> 💡 **서버 자동 오케스트레이션 안내**:  
> `playwright.config.ts`에 다중 `webServer`가 적용되어 있어, 백엔드(`localhost:8080`)나 프론트엔드(`localhost:3000`)를 수동으로 켜두지 않아도 `pnpm test` 실행 시 Playwright가 두 서버를 자동으로 시동하고 테스트 종료 후 프로세스를 정상 정지합니다.

---

## 📂 파일 구조 및 8대 스펙 명세 (Structure)

```text
e2e/
├── specs/
│   ├── auth.spec.ts          # 1. 로그인 폼 및 비정상 로그인 예외 처리 검증 (2 tests)
│   ├── concert.spec.ts       # 2. 메인 공연 목록, 마감 탭, 검색, 상세페이지 검증 (3 tests)
│   ├── queue.spec.ts         # 3. 3인 동시 접속 대기열 정원(2인) 차단 시뮬레이션 (1 test)
│   ├── seat-booking.spec.ts  # 4. 좌석 선택 & User 1 선점 시 User 2 중복 클릭 차단 검증 (2 tests)
│   ├── payment.spec.ts       # 5. 결제 요약 정보 & 모의 결제 완료 검증 (1 test)
│   ├── board.spec.ts         # 6. 게시판 글쓰기, 댓글/좋아요 및 실시간 SSE 알림 통합 검증 (2 tests)
│   ├── mypage.spec.ts        # 7. 마이페이지 프로필 & 3D 티켓 뒤집기 인터랙션 통합 검증 (2 tests)
│   └── qr-verify.spec.ts     # 8. 데스크톱 및 모바일 QR 그룹 검증 통합 검증 (2 tests)
├── playwright.config.ts      # Playwright 엔진 및 다중 webServer 설정
├── package.json
└── README.md
```
