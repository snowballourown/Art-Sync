/* ============================================
   Art-Sync 공통 자바스크립트
   - REST API 호출 헬퍼
   - 상태 라벨, 날짜 포맷 등 공용 유틸
   ============================================ */

/**
 * REST API 호출 헬퍼.
 * 성공 시 응답 본문(JSON)을 반환, 실패 시 서버 메시지로 Error 를 던진다.
 */
async function api(method, url, body) {
    const options = {
        method: method,
        headers: {},
        credentials: 'same-origin'   // 세션 쿠키(JSESSIONID) 포함
    };
    if (body !== undefined && body !== null) {
        options.headers['Content-Type'] = 'application/json';
        options.body = JSON.stringify(body);
    }
    const res = await fetch(url, options);
    const text = await res.text();
    const data = text ? JSON.parse(text) : null;
    if (!res.ok) {
        const message = (data && data.message) ? data.message : ('오류가 발생했어요 (' + res.status + ')');
        throw new Error(message);
    }
    return data;
}

/** 로그인 상태 확인. 안 되어 있으면 로그인 페이지로 보낸다. 되어 있으면 사용자 정보 반환. */
async function requireLogin(expectedRole) {
    try {
        const user = await api('GET', '/api/auth/me');
        if (expectedRole && user.role !== expectedRole) {
            // 역할이 맞지 않으면 알맞은 페이지로
            location.href = (user.role === 'ADMIN') ? 'admin.html' : 'member.html';
            return null;
        }
        return user;
    } catch (e) {
        location.href = 'index.html';
        return null;
    }
}

async function logout() {
    try { await api('POST', '/api/auth/logout'); } catch (e) { /* 무시 */ }
    location.href = 'index.html';
}

/* ---------- 표시용 라벨 ---------- */

function reservationStatusLabel(status) {
    switch (status) {
        case 'REQUESTED': return '요청중';
        case 'CONFIRMED': return '확정';
        case 'REJECTED':  return '거절됨';
        case 'CANCELLED': return '취소됨';
        default: return status;
    }
}

function reservationStatusClass(status) {
    return 'st-' + status.toLowerCase();
}

function slotStatusLabel(status) {
    switch (status) {
        case 'AVAILABLE': return '예약가능';
        case 'FULL':      return '정원참';
        case 'CLOSED':    return '마감';
        default: return status;
    }
}

function slotStatusClass(status) {
    return 'st-' + status.toLowerCase();
}

/* ---------- 날짜 / 시간 포맷 ---------- */

/** "2026-05-20" -> "5월 20일" */
function formatDate(isoDate) {
    if (!isoDate) return '';
    const parts = isoDate.split('-');
    return Number(parts[1]) + '월 ' + Number(parts[2]) + '일';
}

/** "14:00:00" -> "14:00" */
function formatTime(isoTime) {
    if (!isoTime) return '';
    return isoTime.substring(0, 5);
}

/** "2026-05-15T13:20:30" -> "5/15 13:20" */
function formatDateTime(iso) {
    if (!iso) return '';
    const d = new Date(iso);
    if (isNaN(d)) return iso;
    return (d.getMonth() + 1) + '/' + d.getDate() + ' '
        + String(d.getHours()).padStart(2, '0') + ':'
        + String(d.getMinutes()).padStart(2, '0');
}

/** 오늘 날짜를 YYYY-MM-DD 로 */
function todayIso() {
    const d = new Date();
    return d.getFullYear() + '-'
        + String(d.getMonth() + 1).padStart(2, '0') + '-'
        + String(d.getDate()).padStart(2, '0');
}

/** 메시지 박스 표시 (type: 'ok' | 'error') */
function showMessage(elementId, text, type) {
    const el = document.getElementById(elementId);
    el.textContent = text;
    el.className = 'msg show ' + (type || 'ok');
    if (type === 'ok') {
        setTimeout(() => { el.className = 'msg'; }, 3000);
    }
}
