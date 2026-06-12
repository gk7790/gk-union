package com.gk.auth.utils;

import com.gk.auth.entity.SysUser;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jwt.Jwt;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 生成token需要有固定的key
 * setSubject: 模块(admin, zap, relay)
 * userId: 用户id
 * uname: 用户账户
 * 其他根据各个模块自定义
 */
@Slf4j
public class JwtUtils {

    public static final String USER_ID = "id";
    public static final String TENANT_ID = "tenantId";
    public static final String MERCHANT_ID = "merchantId";
    public static final String DEPT_ID = "deptId";
    public static final String ROLE_ID = "roleId";
    public static final String SUBJECT_TYPE = "subjectType";
    public static final String UNAME = "uname";
    public static final String SUPER_Admin = "sAdmin";
    public static final String DOMAIN = "domain";


    // JWT 签名密钥（生产环境应从配置读取）
    private static final String SECRET = "X8h9V4nK7eLpBz2G1qR0sY5mDf3wJt6uN4cE7aP1bQ8rFv9Z";
    private static final long expiration = 86400000L; // 24小时

    private static final SecretKey SECRET_KEY = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

    /**
     * 生成 JWT Token
     */
    public static String generateToken(String subject, Map<String, Object> claims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        return Jwts.builder().setClaims(claims).setIssuer("gk-cloud")
                .setSubject(subject).setIssuedAt(now).setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY).compact();
    }

    /**
     * 验证并解析 JWT Token
     */
    public static Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("JWT Token 已过期: {}", token);
            throw new RuntimeException("Token 已过期");
        } catch (UnsupportedJwtException e) {
            log.warn("不支持的 JWT Token: {}", token);
            throw new RuntimeException("不支持的 Token 格式");
        } catch (MalformedJwtException e) {
            log.warn("JWT Token 格式错误: {}", token);
            throw new RuntimeException("Token 格式错误");
        } catch (SignatureException e) {
            log.warn("JWT Token 签名无效: {}", token);
            throw new RuntimeException("Token 签名无效");
        } catch (Exception e) {
            log.warn("JWT Token 解析失败: {}", e.getMessage());
            throw new RuntimeException("Token 解析失败");
        }
    }

    /**
     * 验证 Token 是否有效
     */
    public static boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从 Token 中获取权限列表
     */
    @SuppressWarnings("unchecked")
    public static java.util.List<String> getAuthoritiesFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("authorities", java.util.List.class);
    }

    /**
     * 刷新 Token（生成新Token）
     */
    public static String refreshToken(String token) {
        Claims claims = parseToken(token);

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    /**
     * 检查 Token 是否过期
     */
    private boolean isTokenExpired(Jwt jwt) {
        return jwt.getExpiresAt() != null &&
                jwt.getExpiresAt().isBefore(Instant.now());
    }

    public static JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(getSecretKey()).build();
    }


    // 获取 SecretKey
    private static SecretKey getSecretKey() {
        return new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
