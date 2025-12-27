package com.rubberhuman.client.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

import java.util.Base64;
import java.util.Date;

public class SessionManager {

    @Getter
    private static String token;

    @Getter
    private static String username;

    @Getter
    private static String role;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void saveSession(String t, String u, String r) {
        token = t;
        username = u;
        role = r;
    }

    public static void clearSession() {
        token = null;
        username = null;
        role = null;
    }

    public static boolean isLoggedIn() {
        return token != null && !isTokenExpired(token);
    }


    private static boolean isTokenExpired(String token) {
        try {
            // JWT 结构: Header.Payload.Signature
            String[] parts = token.split("\\.");
            if (parts.length != 3) return true;

            // 解码 Payload (中间部分)
            String payloadJson = new String(Base64.getDecoder().decode(parts[1]));

            // 解析 JSON
            JsonNode jsonNode = objectMapper.readTree(payloadJson);

            // 获取过期时间戳 (exp 是秒，Java 需要毫秒)
            long exp = jsonNode.get("exp").asLong();
            long now = System.currentTimeMillis() / 1000;

            // 如果当前时间 > 过期时间，则已过期
            return now > exp;
        } catch (Exception e) {
            e.printStackTrace();
            return true; // 解析失败视为过期
        }
    }
}