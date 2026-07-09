package com.back.domain.user.dto;

import com.back.domain.user.entity.User;
import java.util.List;

public record MyPageResponse(
        String name,
        String id,
        String email,
        String loginType,
        List<TicketGroupInfo> ticketGroups
) {
    public static MyPageResponse from(User user, List<TicketGroupInfo> ticketGroups) {
        return new MyPageResponse(
                user.getName(),
                user.getLoginId(),
                user.getEmail(),
                user.getLoginType().name(),
                ticketGroups
        );
    }
}