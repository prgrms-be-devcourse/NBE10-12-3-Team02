package com.back.global.security;

import lombok.Getter;
import org.springframework.security.core.userdetails.User;

import java.util.List;

@Getter
public class SecurityUser extends User {
    private final Long id;
    private final String name;

    public SecurityUser(
            Long id,
            String name
    ) {
        super(String.valueOf(id), "", List.of());
        this.id = id;
        this.name = name;
    }
}