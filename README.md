# 🎨 Art-Sync — 화방 예약 관리 시스템

> 화방을 운영하는 사장님과 손님(회원)을 위한 웹 기반 예약 관리 시스템  
> Spring Boot + JPA + MySQL · 세션 기반 인증 · 단일 페이지 정적 프론트엔드

---

## 📌 프로젝트 개요

종이 다이어리로 관리하던 수업 예약을 디지털화한다.  
사장님은 시간대를 만들고 예약을 수락·거절하며, 손님은 원하는 날짜·시간을 직접 신청한다.

| 역할 | 주요 기능 |
|---|---|
| **사장님 (ADMIN)** | 시간대 생성·공개·삭제, 예약 수락/거절, 예약 현황 조회 |
| **손님 (MEMBER)** | 예약 가능 시간 조회, 예약 신청·취소, 내 예약 현황 확인 |

---

## 🛠 기술 스택

| 구분 | 내용 |
|---|---|
| 언어 | Java 17 |
| 프레임워크 | Spring Boot 3.x (Web, Data JPA, Security) |
| 데이터베이스 | MySQL 8.x (H2 테스트용 사용 가능) |
| 빌드 | Gradle |
| 인증 | 세션 기반 로그인 (Spring Security) |
| 프론트엔드 | HTML + Vanilla JS + CSS (정적 파일, 별도 빌드 불필요) |

---

## 🚀 실행 방법

```bash
# 1. 저장소 클론
git clone https://github.com/your-repo/art-sync.git
cd art-sync

# 2. DB 설정 (src/main/resources/application.yml)
#    spring.datasource.url / username / password 수정

# 3. 빌드 및 실행
./gradlew bootRun
```

> 브라우저에서 `http://localhost:8080` 접속  
> 기본 사장님 계정: **admin / admin1234**

---

## 📁 프로젝트 구조

```
src/main/java/com/artsync/
├── config/          # Security 설정, 초기 데이터
├── controller/      # REST API 컨트롤러 (Auth, Slot, Reservation, Admin, Notification)
├── domain/          # JPA 엔터티 + Repository (user, slot, reservation, notification)
├── dto/             # 요청·응답 DTO (Record)
├── service/         # 비즈니스 로직
└── common/          # 예외 처리, 세션 유틸

src/main/resources/static/
├── index.html       # 로그인 / 회원가입
├── admin.html       # 사장님 화면
├── member.html      # 손님 화면
├── app.js           # 공용 JS (API 헬퍼, 유틸 함수)
└── style.css        # 공용 스타일
```

---

## ✅ 개발 완료 항목

### 1차 — 핵심 기능 구현

- [x] 회원가입 / 로그인 / 로그아웃 (세션 기반)
- [x] 사장님 — 예약 시간대 일괄 생성 (날짜·시간 범위 + 타임 단위 지정)
- [x] 사장님 — 슬롯 공개(activate) / 숨기기(deactivate) / 삭제
- [x] 사장님 — 예약 수락 / 거절 (거절 사유 입력 가능)
- [x] 손님 — 날짜별 예약 가능 시간대 조회 및 예약 신청
- [x] 손님 — 내 예약 현황 조회 및 취소
- [x] 인앱 알림 (예약 요청·수락·거절·취소 시 발송)
- [x] 비즈니스 규칙: 정원 초과 방지 (BR-02), 예약 마감 기준 (BR-01: 예약일 전날 23:59:59)

### 2차 — UI/UX 개선 (Claude 세션 1)

- [x] 전체 UI 리디자인 (갈색 톤, 카드 레이아웃, 상태 배지)
- [x] **사장님 화면**
  - 로그인 후 첫 화면 = **예약 현황 대시보드** (날짜별 시간대 + 예약자 이름 표시)
  - 처리 대기 중인 예약 요청 건수 배지
  - 시간대 만들기 / 시간대 관리 섹션을 접기·펼치기로 정리
- [x] **손님 화면**
  - 내 예약 현황을 최상단 배치 (확정·대기 → 취소·거절 순으로 정렬)
  - 예약 카드에 왼쪽 색상 줄로 상태 시각화

### 3차 — 예약 제한 + 사장님 UX 개선 (Claude 세션 2)

- [x] **손님 — 마감 기간 예약 차단**
  - 날짜 선택기 `min=내일` 설정으로 오늘 이전 선택 불가
  - 백엔드에서도 `isClosed()` 슬롯 필터링 (이중 방어)
  - 마감 날짜 선택 시 안내 메시지 표시
- [x] **사장님 — 당일·과거 날짜 슬롯 생성 경고**
  - 오늘 선택 → 노란 경고 ("회원 예약 불가 상태로 생성됨")
  - 과거 날짜 선택 → 빨간 경고 ("지난 날짜, 강제 생성 가능")
  - 경고가 있어도 생성 가능 (강제 등록 허용)
- [x] **사장님 — 시간대 만들기 그리드 UI**
  - 기존 시작·종료 시간 직접 입력 → 오전·오후 버튼 그리드로 교체
  - 버튼에 시작~종료 시간 함께 표시 (타임 길이 변경 시 실시간 반영)
  - 여러 시간대 동시 선택 후 한 번에 생성
- [x] **사장님 — 시간대 관리 카드에 예약자 목록 표시**
  - 각 슬롯 카드 우측에 예약자 이름 + 상태(✅ 확정 / ⏳ 대기) 노출
  - `ReservationResponse`에 `memberName` 필드 추가 (백엔드)
  - `GET /api/admin/reservations/by-date?date=X` 엔드포인트 신설

---

## 🗓 다음 개발 예정 항목 (2차 인터뷰 도출)

### 기능 1 — 날짜 다중 선택 + 수업 시간표 일괄 적용

> "매주 같은 요일에 같은 시간표를 등록하고 싶다"

- [ ] 시간대 만들기에서 날짜를 여러 개 선택 (달력 다중 선택 또는 요일 반복 설정)
- [ ] 선택한 모든 날짜에 동일한 시간 그리드 슬롯 일괄 생성
- [ ] 예) "매주 월·수·금, 10:00 / 13:00 / 15:00, 2시간" 한 번에 등록

### 기능 2 — 사장님 월별 다이어리 뷰

> "달력처럼 한눈에 이번 달 예약 현황을 보고 싶다"

- [ ] 월별 달력 형태로 예약 현황 표시
- [ ] 날짜 셀에 해당 일의 예약 건수 또는 예약자 이름 미리 보기
- [ ] 날짜 클릭 → 해당 날짜의 시간대별 상세 현황 슬라이드·팝업
- [ ] **호버(커서 올리기) 기능**: 예약자 이름에 마우스를 올리면 그 사람이 예약한 시간대를 툴팁으로 표시

---

## 📐 데이터 모델 (간략)

```
users           time_slots          reservations          notifications
─────────       ──────────          ────────────          ─────────────
id              id                  id                    id
login_id        slot_date           slot_id (FK)          user_id (FK)
password        start_time          member_id (FK)        type
name            end_time            status                message
phone           capacity            requested_at          read
role            active              decided_at            created_at
created_at      status              memo
                created_by (FK)
                created_at
```

**예약 상태 흐름**

```
REQUESTED → CONFIRMED  (사장님 수락)
          → REJECTED   (사장님 거절)
          → CANCELLED  (손님 취소)
CONFIRMED → CANCELLED  (손님 취소, 마감 전)
```

---

## 📋 비즈니스 규칙 요약

| 규칙 | 내용 |
|---|---|
| BR-01 | 예약 신청·수락은 예약일 **전날 23:59:59**까지만 가능 |
| BR-02 | 슬롯당 동시 수용 인원 초과 시 예약 불가 (기본 4명) |
| BR-03 | 마감된 슬롯의 예약은 수락 처리 불가 |
| BR-04 | `active=false` 슬롯은 손님에게 노출되지 않음 |

---

## 📝 관련 문서

- [`요구사항명세서.md`](./요구사항명세서.md) — 시스템 요구사항 명세서 (SRS v1.0)
- [`설계문서.md`](./설계문서.md) — 아키텍처·DB·API 설계 (v1.0)
