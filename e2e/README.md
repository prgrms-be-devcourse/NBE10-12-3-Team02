# NBE10-12-3-Team02 E2E System Test Package

Playwright 기반 시스템통합 E2E (End-to-End) 테스트 패키지입니다.

## 🚀 빠른 시작 (Quick Start)

### 1. 패키지 설치
```bash
cd e2e
pnpm install
npx playwright install chromium
```

### 2. 테스트 실행
```bash
# 헤드리스 모드로 E2E 테스트 전체 실행
pnpm test

# 대화형 UI 모드로 E2E 테스트 실행 및 디버깅
pnpm test:ui

# 브라우저 화면 표시(Headed) 모드로 실행
pnpm test:headed

# HTML 테스트 결과 리포트 보기
pnpm test:report
```

## 📂 구조 (Structure)

```
e2e/
├── package.json
├── playwright.config.ts
├── tsconfig.json
├── pages/              # Page Object Models (페이지 추상화 클래스)
│   ├── LoginPage.ts
│   ├── ConcertListPage.ts
│   ├── SeatSelectionPage.ts
│   └── MyPage.ts
└── specs/              # 시나리오별 E2E 테스트 스펙
    ├── auth.spec.ts
    ├── concert.spec.ts
    └── seat-booking.spec.ts
```
