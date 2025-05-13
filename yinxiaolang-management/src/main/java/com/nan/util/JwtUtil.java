package com.nan.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret:yin_xiao_lang_default_secret_key_please_change_in_production}")
    private String secret;

    @Value("${jwt.expiration:86400000}") // 默认24小时
    private long expiration;

    /**
     * 从token中提取用户名
     */
    public String extractUsername(String token) {
        if (isInvalidTokenFormat(token)) {
            return null;
        }
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 从token中提取过期时间
     */
    public Date extractExpiration(String token) {
        if (isInvalidTokenFormat(token)) {
            return new Date(0);  // 返回一个过期的日期
        }
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * 提取特定的claim
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claims != null ? claimsResolver.apply(claims) : null;
    }

    /**
     * 验证token格式是否明显无效
     */
    private boolean isInvalidTokenFormat(String token) {
        if (token == null || token.trim().isEmpty()) {
            return true;
        }
        // JWT应该有两个点，分隔header、payload和signature
        return token.chars().filter(ch -> ch == '.').count() != 2;
    }

    /**
     * 提取所有的claims
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (MalformedJwtException e) {
            if (log.isDebugEnabled()) {
                log.debug("JWT格式错误: {}", e.getMessage());
            }
            return null;
        } catch (JwtException e) {
            if (log.isDebugEnabled()) {
                log.debug("JWT验证失败: {}", e.getMessage());
            }
            return null;
        } catch (Exception e) {
            log.error("JWT解析异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 检查token是否过期
     */
    private Boolean isTokenExpired(String token) {
        try {
            final Date expiration = extractExpiration(token);
            return expiration == null || expiration.before(new Date());
        } catch (Exception e) {
            log.debug("检查token过期失败: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 为用户生成token
     */
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username);
    }

    /**
     * 创建token
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 验证token是否有效
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        if (isInvalidTokenFormat(token)) {
            return false;
        }
        
        try {
            final String username = extractUsername(token);
            return username != null && username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            log.debug("验证token失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
} 