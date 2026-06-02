# Art-Sync — 화실 예약 관리 시스템

> 화실을 운영하는 선생님과 참여자를 위한 웹 기반 예약 관리 시스템  
> Spring Boot + JPA + MySQL · 세션 기반 인증 · 단일 페이지 정적 프론트엔드

---

## 프로젝트 개요

종이 다이어리로 관리하던 수업 예약을 디지털화한다.  
선생님은 수업을 만들고 시간대를 등록·공개하며 예약을 수락·거절한다.  
참여자는 열려 있는 수업을 찾아 원하는 시간대를 직접 신청한다.

| 역할 | 주요 기능 |
|---|---|
| **선생님 (TEACHER)** | 수업 생성, 시간대 등록·공개·삭제, 예약 수락/거절, 월별 예약 현황 조회 |
| **참여자 (PARTICIPANT)** | 수업 탐색, 예약 가능 시간 조회, 예약 신청·취소, 내 예약 현황 확인 |

---

## 기술 스택

| 구분 | 내용 |
|---|---|
| 언어 | Java 17 |
| 프레임워크 | Spring Boot 3.x (Web, Data JPA, Security) |
| 데이터베이스 | MySQL 8.x (개발환경 H2 인메모리) |
| 빌드 | Gradle |
| 인증 | 세션 기반 로그인 (Spring Security) |
| 프론트엔드 | HTML + Vanilla JS + CSS (정적 파일, 별도 빌드 불필요) |

---

## 실행 방법

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
> 회원가입 후 역할(선생님 / 참여자)을 선택하여 시작

---

## 프로젝트 구조

```
src/main/java/com/artsync/
├── config/          # Security 설정, 초기 데이터
├── controller/      # REST API 컨트롤러 (Auth, Space, Slot, Reservation, Notification)
├── domain/          # JPA 엔터티 + Repository (user, space, slot, reservation, notification)
├── dto/             # 요청·응답 DTO (Record)
├── service/         # 비즈니스 로직
└── common/          # 예외 처리, 세션 유틸

src/main/resources/static/
├── index.html       # 로그인 / 회원가입
├── admin.html       # 선생님 화면
├── member.html      # 참여자 화면
├── app.js           # 공용 JS (API 헬퍼, 유틸 함수)
└── style.css        # 공용 스타일
```

---

## 개발 완료 항목

### 1차 — 핵심 기능 구현

- [x] 회원가입 / 로그인 / 로그아웃 (세션 기반)
- [x] 선생님 — 예약 시간대 일괄 생성 (날짜·시간 범위 + 타임 단위 지정)
- [x] 선생님 — 슬롯 공개(activate) / 숨기기(deactivate) / 삭제
- [x] 선생님 — 예약 수락 / 거절 (거절 사유 입력 가능)
- [x] 참여자 — 날짜별 예약 가능 시간대 조회 및 예약 신청
- [x] 참여자 — 내 예약 현황 조회 및 취소
- [x] 인앱 알림 (예약 요청·수락·거절·취소 시 발송)
- [x] 비즈니스 규칙: 정원 초과 방지, 예약 마감 기준 (예약일 전날 23:59:59)

### 2차 — UI/UX 개선

- [x] 전체 UI 리디자인 (카드 레이아웃, 상태 배지)
- [x] 선생님 화면 — 예약 현황 대시보드 (날짜별 시간대 + 예약자 이름 표시)
- [x] 선생님 화면 — 시간대 만들기 그리드 UI (오전·오후 버튼 선택)
- [x] 선생님 화면 — 요일 반복 슬롯 일괄 생성
- [x] 선생님 화면 — 월별 달력 다이어리 뷰 (예약자 이름·호버 팝업)
- [x] 참여자 화면 — 예약 가능 달력 + 내 예약 달력 슬라이드 탭

### 3차 — 구조 개선 및 기능 추가

- [x] Space(수업) 기반 API 구조로 전환 (`/api/spaces/{spaceId}/*`)
- [x] 선생님이 여러 수업을 생성·관리할 수 있는 Space 개념 도입
- [x] 회원가입 시 역할 고정 (TEACHER / PARTICIPANT) — 로그인마다 역할 선택 불필요
- [x] 참여자 예약 시간 겹침 방지 (같은 날 시간이 겹치는 슬롯 중복 신청 차단)
- [x] B&W 디자인 시스템으로 전환 및 전체 이모지 제거
- [x] 선생님 달력 — 예약 확정(진한 배경) / 대기(점선 테두리) 색상 구분
- [x] TimeSlotService 단위 테스트 작성

---

## 데이터 모델 (간략)

```
users           spaces          time_slots          reservations          notifications
─────────       ──────────      ──────────          ────────────          ─────────────
id              id              id                  id                    id
login_id        name            space_id (FK)       slot_id (FK)          user_id (FK)
password        description     slot_date           member_id (FK)        type
name            owner_id (FK)   start_time          status                message
phone           created_at      end_time            requested_at          read
role (TEACHER                   capacity            decided_at            created_at
     |PARTICIPANT)              active              memo
created_at                      status
                                created_by (FK)
                                created_at
```

**예약 상태 흐름**

```
REQUESTED → CONFIRMED  (선생님 수락)
          → REJECTED   (선생님 거절)
          → CANCELLED  (참여자 취소)
CONFIRMED → CANCELLED  (참여자 취소, 마감 전)
```

---

## 비즈니스 규칙 요약

| 규칙 | 내용 |
|---|---|
| BR-01 | 예약 신청·수락은 예약일 **전날 23:59:59**까지만 가능 |
| BR-02 | 슬롯당 동시 수용 인원 초과 시 예약 불가 (기본 4명) |
| BR-03 | 마감된 슬롯의 예약은 수락 처리 불가 |
| BR-04 | `active=false` 슬롯은 참여자에게 노출되지 않음 |
| BR-05 | 같은 날 시간이 겹치는 슬롯은 중복 신청 불가 |

---

## 관련 문서

- [`요구사항명세서.md`](./요구사항명세서.md) — 시스템 요구사항 명세서 (SRS v1.0)
- [`설계문서.md`](./설계문서.md) — 아키텍처·DB·API 설계 (v1.0)
