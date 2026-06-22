package com.artsync.domain.space;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 예약 공간 (화방, 스튜디오 등).
 * 한 사용자가 여러 Space 를 운영할 수 있고,
 * 다른 Space 에는 참가자로 예약을 넣을 수 있다.
 */
@Entity
@Table(name = "spaces")
public class Space {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    /** 이 공간을 운영하는 사용자 id */
    @Column(nullable = false)
    private Long ownerId;

    /** 참여자가 처음 등록할 때 입력하는 수업 코드 */
    @Column(name = "join_code", unique = true, length = 12)
    private String joinCode;

    /** 신규 참여자에게 적용될 기본 월간 수업 횟수 한도 */
    @Column(nullable = false)
    private int defaultMonthlyLimit = 4;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Space() {
        // JPA 기본 생성자
    }

    public Space(String name, String description, Long ownerId, String joinCode) {
        this.name = name;
        this.description = description;
        this.ownerId = ownerId;
        this.joinCode = joinCode;
        this.defaultMonthlyLimit = 4;
        this.createdAt = LocalDateTime.now();
    }

    public void update(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void updateDefaultMonthlyLimit(int limit) {
        this.defaultMonthlyLimit = limit;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Long getOwnerId() { return ownerId; }
    public String getJoinCode() { return joinCode; }
    public int getDefaultMonthlyLimit() { return defaultMonthlyLimit; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
