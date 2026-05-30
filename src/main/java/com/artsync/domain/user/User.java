package com.artsync.domain.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 시스템 사용자 (사장님 또는 회원).
 * 설계문서 2.2 - users 테이블 대응.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 로그인 아이디 (중복 불가) */
    @Column(nullable = false, unique = true, length = 50)
    private String loginId;

    /** 암호화되어 저장되는 비밀번호 (BCrypt) */
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Role role;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected User() {
        // JPA 기본 생성자
    }

    public User(String loginId, String password, String name, String phone, Role role) {
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.role = role;
        this.createdAt = LocalDateTime.now();
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public Long getId() {
        return id;
    }

    public String getLoginId() {
        return loginId;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public Role getRole() {
        return role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
