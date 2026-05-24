# 📋 Art-Sync 개발 일지

> 과제 회차별로 무엇을 추가하고 변경했는지 기록합니다.  
> 형식: `[날짜] 회차 — 작업 내용`

---

## [2026-05-15] 1회차 — 프로젝트 초기 구축

### 🏗 프로젝트 세팅
- Spring Boot 3.x + Gradle 프로젝트 생성
- MySQL 연동, 세션 기반 Spring Security 설정
- 공통 예외 처리 (`GlobalExceptionHandler`, `BusinessException`, `NotFoundException`)

### 🗄 도메인 & DB
| 엔티티 | 설명 |
|---|---|
| `User` | 회원/사장님 공통. `Role(ADMIN/MEMBER)` 구분 |
| `TimeSlot` | 수업 가능 시간대. 날짜·시작·종료 시간·정원·공개 여부 |
| `Reservation` | 회원의 예약 요청. `ReservationStatus(REQUESTED/CONFIRMED/REJECTED/CANCELLED)` |
| `Notification` | 앱 내 알림. 예약 요청·수락·거절·취소 이벤트 발생 시 생성 |

### 🔌 API (백엔드)
- `POST /api/auth/signup` — 회원가입
- `POST /api/auth/login` / `POST /api/auth/logout` — 로그인/로그아웃
- `GET /api/slots` — 회원용 날짜별 공개 슬롯 조회
- `POST /api/admin/slots` — 슬롯 일괄 생성 (시간 범위 → 자동 분할)
- `POST /api/admin/slots/single` — 슬롯 단건 생성
- `PATCH /api/admin/slots/{id}/activate` / `deactivate` — 슬롯 공개/숨김
- `DELETE /api/admin/slots/{id}` — 슬롯 삭제
- `POST /api/reservations` — 예약 신청
- `DELETE /api/reservations/{id}` — 예약 취소
- `GET /api/reservations/me` — 내 예약 목록
- `GET /api/admin/reservations` — 처리 대기(REQUESTED) 목록
- `GET /api/admin/reservations/by-date` — 날짜별 예약 현황
- `PATCH /api/admin/reservations/{id}/confirm` / `reject` — 예약 수락/거절
- `GET /api/notifications/me` — 알림 목록
- `PATCH /api/notifications/{id}/read` — 알림 읽음 처리

### 🖥 화면 (프론트엔드)
- `index.html` — 로그인 / 회원가입 화면
- `member.html` — 날짜 선택 → 슬롯 조회 → 예약 신청, 내 예약 현황
- `admin.html` — 예약 현황 대시보드, 처리 대기 목록, 시간대 만들기(그리드 선택), 시간대 관리/공개, 알림 패널

---

## [2026-05-24] 2회차 — 사장님 UX 개선 (2차 인터뷰 반영)

### ✨ 새 기능: 월별 다이어리 뷰

**배경:** "달력처럼 한눈에 이번 달 예약 현황을 보고 싶다" (2차 인터뷰)

**백엔드**
| 파일 | 변경 내용 |
|---|---|
| `TimeSlotRepository` | `findBySlotDateBetweenOrderBySlotDateAscStartTimeAsc()` 쿼리 메서드 추가 |
| `TimeSlotService` | `getSlotsForAdminBetween(from, to)` 서비스 메서드 추가 |
| `DaySummaryResponse` | 신규 DTO — 날짜·슬롯 수·예약 목록을 하루 단위로 묶어 반환 |
| `AdminReservationController` | `GET /api/admin/reservations/monthly-summary?year=&month=` 엔드포인트 추가 |

**프론트엔드 (`admin.html`, `style.css`)**
- 월별 달력 그리드 (7열, 이전/다음 월 이동)
- 날짜 셀에 예약자 이름 미리보기 (최대 3명 + 초과 건수)
- 셀 색상 구분: 슬롯 없음(흰색) / 슬롯만 있음(연두) / 예약 있음(초록) / 대기 중 예약 있음(노랑)
- 예약자 이름 **호버 툴팁** → 해당 회원이 예약한 시간대 표시 (예: `김철수: 10:00, 14:00`)
- 날짜 셀 클릭 → 슬롯·시간대별 상세 예약자 패널 슬라이드

### ✨ 새 기능: 날짜 다중 선택 + 시간표 일괄 적용

**배경:** "매주 같은 요일에 같은 시간표를 등록하고 싶다" (2차 인터뷰)

**프론트엔드 (`admin.html`, `style.css`)** *(백엔드 변경 없음)*
- 시간대 만들기에 **단일 날짜 / 요일 반복** 탭 전환 추가
- 요일 반복 모드: 시작일·종료일 + 요일 선택 버튼(일~토)
- 선택된 날짜를 칩 형태로 미리보기 (예: `5/26(월)`, `5/28(수)`, …)
- 만들기 실행 시 N일 × M타임 = K개 슬롯 일괄 생성

---

<!-- 아래 형식을 복사해서 다음 회차 작업 후 붙여넣으세요 -->
<!--
## [YYYY-MM-DD] N회차 — 한 줄 요약

### ✨ 새 기능: ...
### 🐛 버그 수정: ...
### ♻️ 리팩토링: ...
-->

---

## [2026-05-24] 2회차 테스트 — 기능 검증 및 버그 수정

> 구현 완료 후 직접 실행하며 테스트한 결과 발견된 버그 및 수정 내역

### 🐛 버그 1 — 시간대 만들기 섹션이 클릭해도 열리지 않음

**발견 경위:** 2회차 기능 구현 후 첫 실행 테스트에서 즉시 발견

**원인 분석:**
- `renderCalendar()` 함수 내에서 HTML 문자열을 조합할 때 `onclick="openDayDetail('' + dateStr + '')"` 형태로 따옴표 이스케이프가 깨짐
- JavaScript 파싱 단계에서 문법 오류(`Unexpected token`) 발생 → 전체 스크립트 실행 중단 → 모든 클릭 이벤트 동작 불가

**수정:** `''` → `\'` 로 이스케이프 처리 (`admin.html`)

---

### 🐛 버그 2 — 회원 화면에서 예약 신청 후 버튼이 그대로 남음

**발견 경위:** 회원 계정으로 예약 신청 후 UI 상태 확인 중 발견

**원인 분석:**
- 슬롯의 `remaining` 잔여석은 `CONFIRMED` 상태 예약만 차감
- 내가 `REQUESTED`(대기중) 상태로 신청해도 `remaining > 0` 조건이 그대로라 "예약 신청하기" 버튼이 유지됨
- 중복 신청 시도 시에야 서버에서 오류 반환 (사용자 경험 불량)

**수정:** `loadSlots()` 에서 내 예약 목록을 동시에 조회하여, 이미 신청/확정된 슬롯 ID를 추적 → 해당 슬롯은 "✅ 예약 신청 완료" 버튼(비활성)으로 표시 (`member.html`)

---

### 🐛 버그 3 — 요일 반복으로 만든 슬롯이 회원 화면에서 보이지 않음

**발견 경위:** 사장님 계정으로 요일 반복 시간표 생성 후 회원 화면에서 조회 시 발견

**원인 분석:**
- 슬롯은 기본 `active = false`(비공개) 상태로 생성됨
- 단일 날짜는 그때그때 "시간대 관리"에서 공개 가능하나, 수십 개 날짜를 반복 생성하면 날짜별로 일일이 공개하는 것이 현실적으로 불가능

**수정:** 슬롯 만들기 폼에 **"만들면서 바로 공개"** 체크박스 추가 (기본값: 체크됨). 체크 시 각 슬롯 생성 직후 activate API를 연달아 호출하여 즉시 공개 처리 (`admin.html`)

---

### 🐛 버그 4 — 다른 계정으로 로그인 시 이름이 이전 계정으로 고정됨

**발견 경위:** 테스트 계정 전환 중 발견. 다른 아이디로 로그인해도 이름이 바뀌지 않음

**원인 분석:**
- `index.html` 접속 시 이미 로그인된 세션이 있으면 자동으로 `member.html` / `admin.html`로 리다이렉트
- 로그인 폼 자체가 노출되지 않아 계정 전환이 불가능한 구조
- 결과적으로 기존 세션의 사용자 이름이 계속 표시됨

**수정:** 자동 리다이렉트 제거 → 현재 로그인된 계정 정보를 배너로 표시 + **"내 페이지로 이동"** / **"로그아웃 후 다른 계정으로"** 버튼 제공 (`index.html`)

---

### 🐛 버그 5 — 요일 반복 슬롯이 선택 요일보다 하루 앞 날짜로 생성됨

**발견 경위:** 월·화·수·목·금 선택 후 생성했는데 달력에서 일·월·화·수·목으로 표시됨. 더 분석하니 슬롯 날짜 자체가 하루씩 밀려서 저장되고 있었고, 그로 인해 마감 처리 기준도 어긋나 예약 불가여야 할 날짜에 예약이 허용되는 문제 확인

**원인 분석:**
- `getRepeatDates()` 함수에서 날짜 문자열 생성 시 `Date.toISOString()`을 사용
- `toISOString()`은 **UTC 기준** 변환이라, 한국(UTC+9)에서는 자정(00:00 KST) = 전날 15:00 UTC로 계산됨
- 예) 월요일 2026-05-25 00:00 KST → `toISOString()` → `"2026-05-24T15:00:00Z"` → 슬롯이 일요일(2026-05-24)로 생성

```
선택한 요일:  월  화  수  목  금
실제 생성일:  일  월  화  수  목  ← 하루씩 앞당겨짐
```

**수정:** `toISOString()` 제거 → 로컬 시간 기준 날짜 포맷 함수 `localDateIso(d)` 추가하여 대체 (`admin.html`)

```javascript
// 수정 전 (UTC 기준 → 하루 밀림)
result.push(cur.toISOString().slice(0, 10));

// 수정 후 (로컬 시간 기준 → 정확)
result.push(localDateIso(cur));
```
