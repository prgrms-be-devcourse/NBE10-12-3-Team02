package com.back.global.ratelimit;

import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import com.back.global.security.SecurityUser;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.Map;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private Cache<String, Bucket> buckets;
    @Value("${app.rate-limit.seat-status.capacity:5}")
    private int capacity;

    @Value("${app.rate-limit.seat-status.refill-tokens:5}")
    private int refillTokens;

    @Value("${app.rate-limit.seat-status.refill-seconds:1}")
    private long refillSeconds;
    @Value("${app.rate-limit.seat-status.expire-after-access-minutes:10}")
    private long expireAfterAccessMinutes;

    @Value("${app.rate-limit.seat-status.maximum-size:10000}")
    private long maximumSize;

    @PostConstruct
    void init() {
        buckets = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(expireAfterAccessMinutes))
                .maximumSize(maximumSize)
                .build();
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        String bucketKey = resolveBucketKey(request);

        Bucket bucket = buckets.get(bucketKey, key -> createBucket());

        if (bucket.tryConsume(1)) {
            return true;
        }

        throw new ServiceException(ErrorCode.TOO_MANY_REQUESTS);
    }

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.classic(
                capacity,
                Refill.intervally(refillTokens, Duration.ofSeconds(refillSeconds))
        );

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private String resolveBucketKey(HttpServletRequest request) {
        String scheduleId = extractScheduleId(request);

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null &&
                authentication.getPrincipal() instanceof SecurityUser securityUser) {

            return "user:" + securityUser.getId()
                    + ":schedule:" + scheduleId
                    + ":seat-status";
        }

        return "ip:" + getClientIp(request)
                + ":schedule:" + scheduleId
                + ":seat-status";
    }
    @SuppressWarnings("unchecked")
    private String extractScheduleId(HttpServletRequest request) {
        Map<String, String> pathVariables =
                (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

        if (pathVariables == null) {
            throw new ServiceException(ErrorCode.BAD_REQUEST);
        }

        String scheduleId = pathVariables.get("scheduleId");

        if (scheduleId == null || scheduleId.isBlank()) {
            throw new ServiceException(ErrorCode.BAD_REQUEST);
        }

        return scheduleId;
    }


    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");

        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");

        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

}
