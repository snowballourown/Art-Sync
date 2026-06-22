package com.artsync.domain.user;

/** 아이디/비밀번호 찾기에 사용하는 계정 복구 질문 목록 */
public enum SecurityQuestion {
    Q01("나의 가장 소중한 것은?"),
    Q02("가장 좋은 기억을 가지고 있는 곳은?"),
    Q03("어릴 적 가장 좋아했던 별명은?"),
    Q04("처음 키웠던 반려동물의 이름은?"),
    Q05("가장 좋아하는 색깔은?"),
    Q06("가장 좋아하는 음식은?"),
    Q07("가장 존경하는 사람은?"),
    Q08("처음 다닌 학교 이름은?"),
    Q09("가장 기억에 남는 여행지는?"),
    Q10("내가 가장 좋아하는 작품이나 노래는?");

    private final String text;

    SecurityQuestion(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
