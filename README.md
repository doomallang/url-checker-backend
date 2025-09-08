<!-- 헤더 배너 -->
<p align="center">
  <img src="https://readme-typing-svg.demolab.com?font=Inter&weight=700&size=28&pause=1200&center=true&vCenter=true&width=900&lines=%F0%9F%94%8D+Uptime+Monitor+Backend;Spring+Boot+3+%7C+JPA+%7C+JWT+%7C+Scheduler+%7C+Observability" alt="Typing SVG" />
</p>

<h1 align="center">🛡️ Uptime Monitor Backend</h1>
<p align="center">
  <img src="https://img.shields.io/badge/Java-21-1F2937?logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.x-0F9157?logo=springboot&logoColor=white" />
  <img src="https://img.shields.io/badge/JPA-334155" />
  <img src="https://img.shields.io/badge/PostgreSQL-4169E1?logo=postgresql&logoColor=white" />
  <img src="https://img.shields.io/badge/JWT-000000?logo=jsonwebtokens&logoColor=white" />
  <img src="https://img.shields.io/badge/WebClient-2563EB" />
  <img src="https://img.shields.io/badge/Scheduler-FF8A00" />
  <img src="https://img.shields.io/badge/Logging-AOP%20%7C%20Filter%20%7C%20MDC-111827" />
</p>

---

## 📌 소개
실서비스에서 **헬스체크/지연/오류**를 수집/요약하는 백엔드입니다.  
스케줄러가 주기적으로 대상 URL을 호출하여 결과를 저장하고, 프론트엔드(Next.js) 대시보드에 **요약(Uptime/Avg Latency)** 과 **최근 결과**를 제공합니다.  
운영 친화적으로 **전역 로깅 필터 + WebClient 슬로우/에러 로깅 + corrId(MDC)** 를 적용했습니다.

---

## 🧱 주요 기능
- ✅ 대상 URL 등록/수정/삭제/토글(활성화) 관리 API
- ✅ 스케줄러 기반 헬스체크(HTTP) + 타임아웃/지연 측정
- ✅ 결과 히스토리 저장 + 요약 API(1h/24h)
- ✅ JWT 인증(Signup/Login) + Spring Security
- ✅ CORS/WebMvcConfig (프론트/로컬 편의)
- ✅ 일일 정리 작업(retentionDays)로 결과 테이블 용량 관리
- ✅ 전역 요청 로깅(필터) + 서비스 AOP + WebClient 요약 로깅

---

## ⚙️ 기술 스택
- **Language**: Java 21
- **Framework**: Spring Boot 3.x, Spring Web, Spring Security, Validation, Scheduling
- **Data**: JPA/Hibernate, MySQL
- **HTTP Client**: WebClient (reactor-netty, 타임아웃/로깅 적용)
- **Auth**: JWT (Access Token)
- **Observability**: MDC Correlation-Id, HttpLoggingFilter, Service AOP, WebClient Filter

---

## 📂 폴더 구조(요약)
```
uptime-backend/
├─ src/main/java/com/doomole/uptime
│ ├─ config/ # WebClient, Security, CORS, AOP
│ ├─ controller/ # REST API (health check, auth)
│ ├─ domain/ # enums (CheckType, HealthStatus)
│ ├─ dto/ # Request/Response DTOs
│ ├─ entity/ # JPA Entities (HealthCheck, HealthCheckResult, User)
│ ├─ exception/ # Custom exceptions
│ ├─ filter/ # HttpLoggingFilter, CorrelationIdFilter
│ ├─ repo/ # Spring Data JPA Repos
│ ├─ scheduler/ # HealthCheckScheduler (tick/cleanup)
│ └─ service/ # HealthCheckService, AuthService
└─ ...
```

---

## 🗄️ DB 스키마(요약)

**health_check**
| column | desc |
|---|---|
| id (PK) | 식별자 |
| name | 표시 이름 |
| type | HTTP (확장 예정) |
| url | 대상 URL |
| interval_seconds | 체크 주기(초) |
| threshold_n, window_m | (확장 파라미터) |
| enabled | 활성화 여부 |
| status | 최신 상태(UP/DOWN/UNKNOWN) |
| fail_count | 연속 실패수 |
| last_checked_at | 마지막 체크 시각 |
| response_time_ms | 최근 응답 지연(ms) |
| last_error | 최근 오류 메시지 |
| created_at, updated_at | 생성/수정 시간 |

**health_check_result**
| column | desc |
|---|---|
| id (PK) | 식별자 |
| health_check_id (FK) | 대상 |
| observed_at | 관측 시각 |
| health_status | UP/DOWN |
| latency_ms | 지연 |
| http_code | HTTP 상태 |
| error_message | 오류 메시지 |

---

## 🔐 인증 & 보안
- `/api/v1/auth/**` : 공개(Signup/Login)
- `GET /api/**` : 데모 기준 공개(옵션)
- 기타 쓰기/관리 API : `Authorization: Bearer {token}` 필요
- Spring Security + `JwtAuthFilter` 로 Stateless 인증
- CORS: 프론트 도메인만 허용(설정값)

---

## 🧭 API 요약

### Health Check 관리
- `GET    /api/v1/health/check` : 목록
- `POST   /api/v1/health/check` : 생성 *(auth 필요)*
- `PUT    /api/v1/health/check/{id}` : 수정 *(auth 필요)*
- `PUT    /api/v1/health/check/{id}/toggle` : 활성/비활성 *(auth 필요)*
- `DELETE /api/v1/health/check/{id}` : 삭제 *(auth 필요)*

### 결과/요약
- `GET /api/v1/health/check/{id}/result?limit=100` : 최근 N개
- `GET /api/v1/health/check/{id}/summary?window=1h|24h` : 요약(가동률/평균지연/최신상태)

### 인증
- `POST /api/v1/auth/signup` : 회원가입 `{email,password}`
- `POST /api/v1/auth/login` : 로그인 → `{token}` 발급

## 🛣️ 로드맵

🔔 알림 연동(Slack/Webhook, 임계치 N회 연속 실패 시)

🌐 멀티 리전 체크(에이전트 분산)

🧮 가중치 기반 Uptime 계산(가용성 창 가중)

📈 Prometheus/Actuator 메트릭 공개

🧪 통합테스트(Testcontainers) 확장

## 🙋‍♂️ Contact
<p> <a href="mailto:doo_style@naver.com"><img src="https://img.shields.io/badge/Email-doo__style%40naver.com-0ea5e9?logo=gmail&logoColor=white" /></a> <a href="https://many.tistory.com"><img src="https://img.shields.io/badge/Blog-many.tistory.com-ff5f2e?logo=tistory&logoColor=white" /></a> </p>
