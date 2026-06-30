package com.back.domain.user.service;

import com.back.domain.user.dto.SignupRequest;
import com.back.domain.user.dto.SignupResponse;
import com.back.domain.user.entity.LoginType;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByIdAndDeletedAtIsNull(request.id())) {
            throw new ServiceException(ErrorCode.USER_ID_ALREADY_EXISTS);
        }
        if (userRepository.existsByEmailAndDeletedAtIsNull(request.email())) {
            throw new ServiceException(ErrorCode.USER_EMAIL_ALREADY_EXISTS);
        }
        User user = userRepository.save(
                User.create(request.id(), request.email(),
                        passwordEncoder.encode(request.password()),
                        request.name(), LoginType.NORMAL)
        );
        return SignupResponse.from(user);
    }
}