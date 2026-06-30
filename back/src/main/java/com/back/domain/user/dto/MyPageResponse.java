package com.back.domain.user.dto;

import com.back.domain.user.entity.User;
import java.util.List;

public record MyPageResponse(
        String name,
        String id,
        String email,
        String loginType,
        List<TicketInfo> ticketList
) {
    public static MyPageResponse from(User user, List<TicketInfo> ticketList) {
        return new MyPageResponse(
                user.getName(),
                user.getId(),
                user.getEmail(),
                user.getLoginType().name(),
                ticketList
        );
    }
}