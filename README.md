<div align="center">

# 🎫 티케팅고 (Ticketing Go)

**콘서트부터 페스티벌까지, 원하는 공연을 가장 빠르게 예매하세요.**

대기열 기반 실시간 좌석 예매 · 인원수별 좌석 자동 배정 · 오리지널 티켓 발급

<br/>

![Next.js](https://img.shields.io/badge/Next.js-16-black?logo=next.js)
![React](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?logo=typescript&logoColor=white)
![TailwindCSS](https://img.shields.io/badge/Tailwind_CSS-v4-38BDF8?logo=tailwindcss&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java-25-007396?logo=openjdk&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?logo=redis&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?logo=mysql&logoColor=white)
![WebSocket](https://img.shields.io/badge/WebSocket-STOMP-black)

</div>

---

## 📌 목차

- [소개](#-소개)
- [주요 기능](#-주요-기능)
- [기술 스택](#-기술-스택)
- [시작하기](#-시작하기)
- [트러블슈팅](#-실행이-안-될-때-체크리스트)
- [Git 컨벤션](#-git-컨벤션)
- [프로젝트 구조](#-프로젝트-구조)

---

## 📖 소개

**티케팅고**는 트래픽이 몰리는 인기 공연 예매 상황을 실제 티켓팅 플랫폼처럼 재현한 콘서트 예매 서비스입니다. 동시 접속자가 몰리면 Redis + WebSocket 기반 대기열이 자동으로 발동하고, 순번에 맞춰 실시간으로 입장이 허가됩니다.

## ✨ 주요 기능

| 기능 | 설명 |
|---|---|
| 🔍 공연 탐색 | 검색, 최신순/마감임박순 정렬, 공연중/마감된 공연 필터 |
| ⏳ 실시간 대기열 | 동시 접속자 초과 시 WebSocket(STOMP)으로 순번 실시간 안내, 입장 허가 시 `X-Queue-Token` 발급 |
| 💺 좌석 자동 배정 | 성인/청소년 인원수 입력 → 2인 이상이면 인접 좌석 자동 페어링 |
| 💳 예매 및 결제 | 좌석 선점(occupy) → 결제 확정, 실패 시 자동 선점 해제 |
| 🎟️ 오리지널 티켓 | 마이페이지에서 한 번의 결제 단위로 묶인 예매 확인, 클릭하면 앞면(포스터)·뒷면(정보)이 뒤집히는 실제 티켓 디자인, 인쇄 지원 |
| 🔐 소셜 로그인 | 카카오, 구글 로그인 지원 |

<br/>

<div align="center">
<sub>(여기에 데모 GIF나 스크린샷을 추가하면 좋아요)</sub>
</div>

## 🛠 기술 스택

**Frontend**
```
Next.js 16 (App Router) · React 19 · TypeScript · Tailwind CSS v4 · pnpm
@stomp/stompjs · sockjs-client · SweetAlert2
```

**Backend**
```
Spring Boot · Java 25 · MySQL · H2(로컬) · Redis · WebSocket(STOMP)
```

**개발 환경**
```
Frontend: VS Code / Cursor   ·   Backend: IntelliJ   ·   Windows: Git Bash
```

---

## 🚀 시작하기

### 1️⃣ 저장소 클론

```bash
git clone https://github.com/prgrms-be-devcourse/NBE10-12-2-Team02.git
cd NBE10-12-2-Team02
```

### 2️⃣ 프론트엔드 환경 설정

`front/.env.local` 파일을 새로 만들고 아래 내용을 입력합니다. (이 파일은 `.gitignore`에 등록되어 있어 각자 로컬에 직접 만들어야 합니다.)

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

> 백엔드가 SSL로 실행 중이라면 `https://localhost:8443`으로, 아니라면 `http://localhost:8080`으로 — **백엔드의 실제 포트/프로토콜과 반드시 일치**시켜주세요.

```bash
cd front
pnpm install
pnpm dev
```

### 3️⃣ 백엔드 환경 설정

`back/src/main/resources/application-secret.yaml`을 새로 만들고 아래 항목을 채웁니다 (값은 팀 채널에서 공유받으세요).

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          kakao:
            client-id: {카카오 클라이언트 ID}
            client-secret: {카카오 시크릿}
          google:
            client-id: {구글 클라이언트 ID}
            client-secret: {구글 시크릿}

custom:
  oauth:
    token:
      encryption-key: {AES-256 Base64 인코딩 키, 32바이트}
```

`encryption-key`는 로컬 DB(H2)가 각자 독립적이라 팀원과 값이 달라도 무방합니다. 아래 명령어로 새로 생성할 수 있습니다.

```bash
python3 -c "import secrets, base64; print(base64.b64encode(secrets.token_bytes(32)).decode())"
```

### 4️⃣ (필요 시) 로컬 SSL 인증서 생성

`application-dev.yaml`에 SSL이 설정되어 있다면, `back` 폴더 안에서 아래 명령어를 실행합니다.

```bash
keytool -genkeypair \
  -alias local-ssl \
  -keyalg RSA \
  -keysize 2048 \
  -storetype PKCS12 \
  -keystore src/main/resources/local-ssl.p12 \
  -validity 3650 \
  -storepass local1234 \
  -dname "CN=localhost"
```

생성 후 브라우저로 `https://localhost:8443`에 직접 접속해서, 인증서 경고가 뜨면 **"고급" → "안전하지 않음으로 이동"**을 눌러 한 번 수동으로 신뢰 처리해야 프론트에서 API 호출이 정상 작동합니다.

### 5️⃣ 백엔드 실행

IntelliJ에서 `BackApplication` 실행, 또는:

```bash
cd back
./gradlew bootRun
```

---

## 🩹 실행이 안 될 때 체크리스트

| 증상 | 원인 | 해결 |
|---|---|---|
| 백엔드가 안 켜짐 (`cannot find symbol`, `Unsatisfied dependency`) | 최근 추가된 클래스/설정이 로컬에 없음 | 최신 커밋 확인, 팀원에게 문의 |
| `Could not resolve placeholder` 에러 | `application-secret.yaml`에 필요한 키 누락 | [3️⃣ 백엔드 환경 설정](#3️⃣-백엔드-환경-설정) 참고 |
| `Unable to create key store` 에러 | `local-ssl.p12` 없음 | [4️⃣ 로컬 SSL 인증서 생성](#4️⃣-필요-시-로컬-ssl-인증서-생성) 참고 |
| 모든 API가 `Failed to fetch` | 백엔드 미실행 또는 `.env.local` 포트/프로토콜 불일치 | 백엔드 콘솔에 `Started BackApplication` 확인 + `.env.local` 재확인 |
| 로그인/회원가입도 안 됨 | 위와 동일 (백엔드 자체 미실행이 대부분) | 백엔드 콘솔 에러부터 확인 |

---

## 📐 Git 컨벤션

- 브랜치: `feat/{이슈번호}` · `fix/{이슈번호}`
- 커밋: `Feat: 설명 #이슈번호` · `Fix: 설명 #이슈번호`
- PR: 최소 1인 리뷰 승인 후 Squash Merge, 본문에 `Closes #이슈번호` 포함

## 📂 프로젝트 구조

```
.
├── front/          # Next.js 프론트엔드
│   └── src/app/    # App Router 기반 페이지
└── back/           # Spring Boot 백엔드
    └── src/main/java/com/back/domain/   # 도메인별 패키지
```

---

<div align="center">

Made with 🎫 by Team 02

</div>
