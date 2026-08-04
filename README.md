<div align="center">

# 🎫 티케팅고 (Ticketing Go)

**콘서트부터 페스티벌까지, 원하는 공연을 가장 빠르게 예매하세요.**

대기열 기반 실시간 좌석 예매 · 인원수별 좌석 자동 배정 · 오리지널 티켓 발급

<br/>

![Next.js](https://img.shields.io/badge/Next.js-16-black?logo=next.js)
![React](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?logo=typescript&logoColor=white)
![TailwindCSS](https://img.shields.io/badge/Tailwind_CSS-v4-38BDF8?logo=tailwindcss&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7F52FF?logo=kotlin&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0-6DB33F?logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java-25-007396?logo=openjdk&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-Sentinel-DC382D?logo=redis&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?logo=mysql&logoColor=white)
![Playwright](https://img.shields.io/badge/Playwright-E2E-45BA4B?logo=playwright&logoColor=white)
![Terraform](https://img.shields.io/badge/Terraform-AWS-844FBA?logo=terraform&logoColor=white)

</div>

---

## 📌 목차

- [소개](#-소개)
- [주요 기능](#-주요-기능)
- [기술 스택](#-기술-스택)
- [시작하기](#-시작하기)
- [E2E 시스템 테스트](#-e2e-시스템-테스트)
- [트러블슈팅](#-실행이-안-될-때-체크리스트)
- [Git 컨벤션](#-git-컨벤션)
- [프로젝트 구조](#-프로젝트-구조)

---

## 📖 소개

**티케팅고**는 트래픽이 몰리는 인기 공연 예매 상황을 실제 티켓팅 플랫폼처럼 재현한 콘서트 예매 서비스입니다. 동시 접속자가 몰리면 **Redis ZSet + Server-Sent Events (SSE)** 기반 대기열이 자동으로 발동하여 대기 순번 및 진입이 실시간으로 동기화되며, SSE 기반 실시간 좌석 선점 및 오리지널 티켓 발급 시스템을 제공합니다.

---

## ✨ 주요 기능

| 기능 | 설명 |
|---|---|
| 🔍 공연 탐색 | 검색, 최신순/마감임박순 정렬, 공연중/마감된 공연 필터 |
| ⏳ 실시간 대기열 | 동시 접속자 초과 시 Redis ZSet + SSE로 대기 순번 실시간 스트리밍 및 입장 자동 승격 |
| 💺 실시간 좌석 선점 | SSE 스트림(`/seats/status`) 기반 실시간 좌석 상태(`AVAILABLE`, `HOLD`, `SOLD_OUT`) 동기화 |
| 💺 좌석 자동 배정 | 성인/청소년 인원수 선택 ➔ 2인 이상 선택 시 인접 좌석 자동 페어링 |
| 💳 예매 및 결제 | 좌석 선점(occupy, 10분 TTL) ➔ 결제 확정, 실패/시간초과 시 자동 선점 해제 |
| 🎟️ 오리지널 티켓 | 마이페이지 결제 단위 티켓 묶음 확인, 카드 뒤집기(포스터/예매정보) 3D 인터랙션 및 모바일 QR 검증 지원 |
| 🔐 소셜 로그인 | 카카오, 네이버, 구글 OAuth2 소셜 로그인 및 토큰 재발급(RTR) |

---

## 🛠 기술 스택

### Frontend (`front/`)
- Next.js 16 (App Router) · React 19 · TypeScript 5.x · Tailwind CSS v4 · pnpm
- Fetch & `@microsoft/fetch-event-source` (SSE 실시간 스트림)
- SweetAlert2 · Lucide React

### Backend (`back/`)
- Kotlin 2.4.10 (100% 코틀린) · Java 25 · Spring Boot 4.0.7 (Virtual Threads)
- Spring Data JPA · MySQL · H2 (테스트 인메모리)
- Spring Data Redis (`StringRedisTemplate` / Lettuce, Pipelining, Lua Scripts) · Redis 7.2 Sentinel Cluster
- Spring SSE (`SeatStatusSseEmitterRegistry`) · OAuth2 · JJWT 0.13.0 · Bucket4j

### E2E Testing (`e2e/`)
- `@playwright/test` 1.62+ · Page Object Model (POM) 아키텍처
- 다중 `webServer` 자동 오케스트레이션 (Spring Boot + Next.js)

### Infrastructure (`infra/`)
- Terraform IaC · AWS (EC2, VPC, Security Group, IAM, SSM Parameter Store)

---

## 🚀 시작하기

### 1️⃣ 저장소 클론

```bash
git clone https://github.com/prgrms-be-devcourse/NBE10-12-2-Team02.git
cd NBE10-12-2-Team02
```

### 2️⃣ 프론트엔드 환경 설정

`front/.env.local` 파일을 새로 만들고 아래 내용을 입력합니다.

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

```bash
cd front
pnpm install
pnpm dev
```

### 3️⃣ 백엔드 환경 설정

`back/src/main/resources/application-secret.yaml`을 만들고 보안 키 항목을 채웁니다.

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          kakao:
            client-id: {카카오 클라이언트 ID}
            client-secret: {카카오 시크릿}
          naver:
            client-id: {네이버 클라이언트 ID}
            client-secret: {네이버 시크릿}
          google:
            client-id: {구글 클라이언트 ID}
            client-secret: {구글 시크릿}

custom:
  oauth:
    token:
      encryption-key: {AES-256 Base64 인코딩 키, 32바이트}
```

```bash
cd back
./gradlew bootRun
```

---

## 🧪 E2E 시스템 테스트

최상위 `e2e/` 패키지에서 Playwright를 구동하여 백엔드/프론트엔드 및 데이터베이스 전체 연동 테스트를 자동으로 실행할 수 있습니다.

```bash
cd e2e
pnpm install
npx playwright install chromium

# 백엔드(8080) 및 프론트엔드(3000) 자동 시동 후 E2E 테스트 실행
pnpm test
```

---

## 🩹 실행이 안 될 때 체크리스트

| 증상 | 원인 | 해결 |
|---|---|---|
| 백엔드가 안 켜짐 | `application-secret.yaml`에 필수 키 누락 | [3️⃣ 백엔드 환경 설정](#3️⃣-백엔드-환경-설정) 참고 |
| 모든 API가 `Failed to fetch` | 백엔드 미실행 또는 `.env.local` 포트 불일치 | 백엔드 콘솔의 `Started BackApplication` 확인 및 `.env.local` 확인 |
| Redis 연동 오류 | Redis 서버 미실행 | Docker로 Redis 6379 포트 실행 여부 확인 |

---

## 📐 Git 컨벤션

- 브랜치: `feat/{이슈번호}` · `fix/{이슈번호}` · `refactor/{이슈번호}`
- 커밋: `feat: 설명 (#이슈번호)` · `fix: 설명 (#이슈번호)`
- PR: 최소 1인 리뷰 승인 후 Squash Merge, 본문에 `Closes #이슈번호` 포함

---

## 📂 프로젝트 구조

```text
.
├── back/           # Kotlin 2.4 + Spring Boot 4.0 백엔드 (src/main/kotlin/com/back/)
├── front/          # Next.js 16 + React 19 프론트엔드 (src/app/)
├── e2e/            # Playwright E2E 통합 테스트 (specs/, pages/)
└── infra/          # Terraform 기반 AWS 인프라 구축 (EC2, VPC, IAM)
```

---

<div align="center">

Made with 🎫 by Team 02

</div>
