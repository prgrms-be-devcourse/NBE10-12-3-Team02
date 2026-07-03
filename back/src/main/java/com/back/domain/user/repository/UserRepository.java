package com.back.domain.user.repository;

import com.back.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByLoginIdAndDeletedAtIsNull(String loginId);
    boolean existsByEmailAndDeletedAtIsNull(String email);
    Optional<User> findByUserIdAndDeletedAtIsNull(Long userId);

    @Query("SELECT u FROM User u WHERE u.loginId = :loginId AND u.deletedAt IS NULL")
    Optional<User> findByLoginIdAndDeletedAtIsNull(@Param("loginId") String loginId);
}