package com.back.domain.user.repository;

import com.back.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByIdAndDeletedAtIsNull(String id);
    boolean existsByEmailAndDeletedAtIsNull(String email);
}