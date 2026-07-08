package com.back.global.security.interceptor;

import com.back.domain.waiting.service.WaitingQueueManager;
import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import com.back.global.requestcontext.RequestContext;
import com.back.global.security.SecurityUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class QueueInterceptor implements HandlerInterceptor {
    private final StringRedisTemplate redisTemplate;
    private final RequestContext requestContext;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("DELETE".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        @SuppressWarnings("unchecked")
        Map<String, String> pathVariables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        String scheduleIdStr = (pathVariables != null) ? pathVariables.get("scheduleId") : null;
        if (scheduleIdStr == null || !scheduleIdStr.matches("^\\d+$")) {
            throw new ServiceException(ErrorCode.BAD_REQUEST);
        }
        Long scheduleId = Long.parseLong(scheduleIdStr);

        String token = request.getHeader("X-Queue-Token");
        if (token == null || token.isBlank()) {
            throw new ServiceException(ErrorCode.QUEUE_TOKEN_NOT_FOUND);
        }

        String activeQueueKey = generateQueueActiveKey(scheduleId);
        Double score = redisTemplate.opsForZSet().score(activeQueueKey, token);
        if (score == null || score < System.currentTimeMillis()) {
            throw new ServiceException(ErrorCode.QUEUE_SESSION_EXPIRED);
        }
        Long userId = requestContext.getActor().getId();

        String storedToken = redisTemplate.opsForValue()
                .get(WaitingQueueManager.generateActiveTokenKey(scheduleId, userId));
        if (!token.equals(storedToken)) {
            throw new ServiceException(ErrorCode.QUEUE_SESSION_EXPIRED);
        }
        return true;
    }
    //TODO 추후 RedisKey 관리 책임을 갖는 클래스로 분리 필요
    public static String generateQueueActiveKey(Long scheduleId) {
        return "queue:active:schedule:%d".formatted(scheduleId);
    }
}
