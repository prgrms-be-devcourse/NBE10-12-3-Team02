# NBE10-12-3-Team02 E2E System Test Package

Playwright 기반 시스템통합 E2E (End-to-End) 테스트 패키지입니다.  
프론트엔드(Next.js)와 백엔드(Spring Boot) 및 Redis/DB 전체 시스템 연동을 자동으로 검증합니다.

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
# 헤드리스 모드로 전체 12개 E2E 테스트 실행 (백엔드 8080 & 프론트엔드 3000 자동 감지/시동)
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

## 📂 파일 구조 (Structure)

```text
e2e/
├── package.json            # Playwright 의존성 및 실행 스크립트
├── playwright.config.ts    # 다중 webServer 및 브라우저/리포터 환경설정
├── tsconfig.json           # TypeScript 모듈 매핑 설정
├── pages/                  # Page Object Models (페이지 추상화 클래스)
│   ├── LoginPage.ts        # 로그인 페이지
│   ├── ConcertListPage.ts  # 메인/공연 목록 페이지
│   ├── SeatSelectionPage.ts# 실시간 좌석 선택 페이지
│   ├── PaymentPage.ts      # 결제 및 예매 정보 입력 페이지
│   ├── BoardPage.ts        # 관람후기/기대평 커뮤니티 페이지
│   └── MyPage.ts           # 마이페이지 및 프로필 관리
└── specs/                  # 8대 유저 시나리오 (총 12개 테스트 케이스)
    ├── auth.spec.ts        # [시나리오 1] 인증/로그인 폼 및 에러 팝업
    ├── concert.spec.ts     # [시나리오 2] 공연 리스트, 검색, 마감필터, 상세회차
    ├── queue.spec.ts       # [시나리오 3] 대기열 진입 및 순번 노출
    ├── seat-booking.spec.ts# [시나리오 4] 실시간 좌석 SSE 스트림 연동
    ├── payment.spec.ts     # [시나리오 5] 결제 요약 정보 및 동의 체크
    ├── board-notification.spec.ts # [시나리오 6] 게시판 목록 및 작성 폼
    ├── mypage.spec.ts      # [시나리오 7] 마이페이지 렌더링
    └── ticket-verify.spec.ts# [시나리오 8] 모바일 QR 그룹 티켓 검증
```
