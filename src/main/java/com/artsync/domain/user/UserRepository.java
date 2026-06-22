package com.artsync.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/**
 * User 엔터티 영속성 처리.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLoginId(String loginId);

    List<User> findByName(String name);

    boolean existsByLoginId(String loginId);
}
