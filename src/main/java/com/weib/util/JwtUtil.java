package com.weib.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final Environment environment;
    private static final long EXPIRATION_MS = 2 * 60 * 60 * 1000; // 2 hours

    public JwtUtil(@Value("${jwt.secret}") String secret, Environment environment) {
        this.environment = environment;
        // Ensure key is at least 256 bits (32 bytes) for HS256
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, Math.min(keyBytes.length, 32));
            keyBytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Long userId, String username, String role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRATION_MS);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public Long getUserId(Claims claims) {
        return Long.parseLong(claims.getSubject());
    }

    public String getUsername(Claims claims) {
        return claims.get("username", String.class);
    }

    public String getRole(Claims claims) {
        return claims.get("role", String.class);
    }

    // ========================================
    // 管理端 JWT Token 方法
    // ========================================

    /**
     * 生成管理端 JWT Token
     *
     * Token 有效期 2 小时，包含 userId、username、adminRole 三个 claims。
     * 使用独立的 admin-secret 签名密钥，与用户端 token 密钥分离。
     *
     * @param userId    管理员用户 ID
     * @param username  管理员用户名
     * @param adminRole 管理员角色类型（super_admin / auditor / viewer）
     * @return JWT token 字符串
     */
    public String generateAdminToken(Long userId, String username, String adminRole) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 7200 * 1000); // 2 小时

        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("adminRole", adminRole)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getAdminSigningKey())
                .compact();
    }

    /**
     * 验证管理端 JWT Token
     *
     * @param token JWT token 字符串
     * @return 解析后的 Claims，如果 token 无效则返回 null
     */
    public Claims validateAdminToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getAdminSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取管理端 JWT 签名密钥
     *
     * 从配置属性 jwt.admin-secret 读取密钥字符串，
     * 转换为 HMAC-SHA 密钥对象。
     *
     * @return SecretKey 管理端签名密钥
     * @throws IllegalStateException 如果 jwt.admin-secret 未配置
     */
    private SecretKey getAdminSigningKey() {
        String adminSecret = environment.getProperty("jwt.admin-secret");
        if (adminSecret == null || adminSecret.isEmpty()) {
            throw new IllegalStateException("JWT_ADMIN_SECRET 环境变量未配置");
        }
        byte[] keyBytes = adminSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
