package com.back.global.security.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;

import java.util.HashMap;
import java.util.Map;

class RequestBodyParsingBenchmarkTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("URL Path Variable vs RequestBody Parsing 벤치마크")
    void benchmarkParsingPerformance() throws Exception {
        int requestCount = 50000;
        MockHttpServletRequest urlRequest = new MockHttpServletRequest();
        Map<String, String> pathVariables = new HashMap<>();
        pathVariables.put("scheduleId", "12345");
        urlRequest.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, pathVariables);
        String jsonBody = "{\"concertId\":1,\"scheduleId\":12345,\"seatNumber\":\"A-1\"}";

        // Case A: URL 기반 추출
        long urlStartTime = System.nanoTime();
        for (int i = 0; i < requestCount; i++) {
            @SuppressWarnings("unchecked")
            Map<String, String> vars = (Map<String, String>) urlRequest.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            String scheduleIdStr = (vars != null) ? vars.get("scheduleId") : null;
            if (scheduleIdStr != null) {
                Long scheduleId = Long.parseLong(scheduleIdStr);
            }
        }
        long urlEndTime = System.nanoTime();
        long urlDurationMs = (urlEndTime - urlStartTime) / 1_000_000;

        // Case B: Request Body 파싱 기반 추출
        long bodyStartTime = System.nanoTime();
        for (int i = 0; i < requestCount; i++) {
            byte[] cachedBody = jsonBody.getBytes();

            @SuppressWarnings("unchecked")
            Map<String, Object> bodyMap = objectMapper.readValue(cachedBody, Map.class);
            Object scheduleIdObj = bodyMap.get("scheduleId");
            if (scheduleIdObj != null) {
                Long scheduleId = Long.valueOf(scheduleIdObj.toString());
            }
        }
        long bodyEndTime = System.nanoTime();
        long bodyDurationMs = (bodyEndTime - bodyStartTime) / 1_000_000;

        System.out.println("HTTP 인터셉터 파싱 방식 성능 측정 (요청수: " + requestCount + "회)");
        System.out.println("URL Path Variable 조회 : " + urlDurationMs + " ms");
        System.out.println("Request Body 캐싱 및 JSON 파싱 : " + bodyDurationMs + " ms");
    }
}
