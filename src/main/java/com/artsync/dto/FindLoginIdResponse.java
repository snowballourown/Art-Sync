package com.artsync.dto;

import java.util.List;

/** 아이디 찾기 응답 */
public record FindLoginIdResponse(List<String> loginIds) {
}
