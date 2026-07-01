package com.back.global.security.auth;

import com.back.global.security.SecurityUser;
import com.back.global.security.jwt.payload.AccessTokenPayload;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class SecurityAuthenticationFactory {
    public Authentication create(AccessTokenPayload payload) {
        UserDetails securityUser = new SecurityUser(
                payload.userId(),
                payload.name()
        );

        return new UsernamePasswordAuthenticationToken(
                securityUser,
                null,
                securityUser.getAuthorities()
        );
    }
}