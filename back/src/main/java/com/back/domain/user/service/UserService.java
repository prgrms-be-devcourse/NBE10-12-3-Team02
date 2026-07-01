package com.back.domain.user.service;

import com.back.domain.ticket.repository.TicketRepository;
import com.back.domain.user.dto.*;
import com.back.domain.user.entity.LoginType;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
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

    @Transactional
    public void withdraw(Long pathUserId, Long loginUserId) {
        if (!pathUserId.equals(loginUserId)) {
            throw new ServiceException(ErrorCode.USER_ACCESS_DENIED);
        }
        User user = userRepository.findByUserIdAndDeletedAtIsNull(pathUserId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND_OR_DELETED));
        user.withdraw();
    }

    public MyPageResponse getMyPage(Long userId) {
        User user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        List<TicketInfo> ticketList = ticketRepository.findAllByUserWithConcert(user)
                .stream()
                .map(TicketInfo::from)
                .toList();

        return MyPageResponse.from(user, ticketList);
    }
    @Transactional
    public void updateMyPage(Long userId, UpdateMyPageRequest request) {
        User user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        if (request.name() != null) {
            String trimmed = request.name().trim();
            if (trimmed.isEmpty() || trimmed.contains(" ")) {
                throw new ServiceException(ErrorCode.USER_NAME_INVALID);
            }
            user.updateName(trimmed);
        }

        if (request.email() != null) {
            if (!user.getEmail().equals(request.email())
                    && userRepository.existsByEmailAndDeletedAtIsNull(request.email())) {
                throw new ServiceException(ErrorCode.USER_EMAIL_ALREADY_EXISTS);
            }
            user.updateEmail(request.email());
        }
        if (request.password() != null) {
            user.updatePassword(passwordEncoder.encode(request.password()));
        }
    }
}