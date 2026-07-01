package com.back.global.security.jwt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public class TokenHashUtil {

    public static String sha256(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("토큰 해싱 실패", e);
        }
    }
}