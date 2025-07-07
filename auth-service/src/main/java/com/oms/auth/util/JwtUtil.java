package com.oms.auth.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT Utility class for handling JWT token operations
 * Compatible with JJWT 0.12.3
 */
@Component
public class JwtUtil {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private long jwtExpiration;
    
    /**
     * Extract username from JWT token
     * @param token JWT token
     * @return username
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    /**
     * Extract a specific claim from JWT token
     * @param token JWT token
     * @param claimsResolver function to extract claim
     * @return extracted claim
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    /**
     * Generate JWT token for user
     * @param userDetails user details
     * @return JWT token
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }
    
    /**
     * Generate JWT token with extra claims
     * @param extraClaims additional claims
     * @param userDetails user details
     * @return JWT token
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }
    
    /**
     * Get JWT expiration time
     * @return expiration time in milliseconds
     */
    public long getExpirationTime() {
        return jwtExpiration;
    }
    
    /**
     * Build JWT token with specified parameters
     * @param extraClaims additional claims
     * @param userDetails user details
     * @param expiration expiration time
     * @return JWT token
     */
    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return Jwts
                .builder()
                .claims(extraClaims)                                    
                .subject(userDetails.getUsername())                     
                .issuedAt(new Date(System.currentTimeMillis()))        
                .expiration(new Date(System.currentTimeMillis() + expiration))  
                .signWith(getSignInKey(), Jwts.SIG.HS256)              
                .compact();
    }
    
    /**
     * Validate JWT token with user details
     * @param token JWT token
     * @param userDetails user details
     * @return true if token is valid
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }
    
    /**
     * Validate JWT token (without user details)
     * @param token JWT token
     * @return true if token is valid
     */
    public boolean isTokenValid(String token) {
        try {
            // Try to extract claims and check expiration
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (JwtException e) {
            // Token is invalid (malformed, expired, etc.)
            return false;
        } catch (Exception e) {
            // Any other exception means token is invalid
            return false;
        }
    }
    
    /**
     * Check if token is expired
     * @param token JWT token
     * @return true if token is expired
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    /**
     * Extract expiration date from token
     * @param token JWT token
     * @return expiration date
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    /**
     * Extract issued at date from token
     * @param token JWT token
     * @return issued at date
     */
    public Date extractIssuedAt(String token) {
        return extractClaim(token, Claims::getIssuedAt);
    }
    
    /**
     * Extract all claims from JWT token
     * @param token JWT token
     * @return all claims
     */
    private Claims extractAllClaims(String token) {
        return Jwts
                .parser()                                               
                .verifyWith(getSignInKey())                            
                .build()
                .parseSignedClaims(token)                              
                .getPayload();                                         
    }
    
    /**
     * Get signing key for JWT
     * @return signing key
     */
    private SecretKey getSignInKey() {                                 
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * Get remaining time until token expires
     * @param token JWT token
     * @return remaining time in milliseconds
     */
    public long getTokenRemainingTime(String token) {
        Date expiration = extractExpiration(token);
        return expiration.getTime() - System.currentTimeMillis();
    }
    
    /**
     * Check if token is about to expire (within specified minutes)
     * @param token JWT token
     * @param minutes minutes threshold
     * @return true if token expires within the specified minutes
     */
    public boolean isTokenAboutToExpire(String token, int minutes) {
        long remainingTime = getTokenRemainingTime(token);
        return remainingTime < (minutes * 60 * 1000); // Convert minutes to milliseconds
    }
    
    /**
     * Extract user ID from token if present
     * @param token JWT token
     * @return user ID or null if not present
     */
    public String extractUserId(String token) {
        try {
            return extractClaim(token, claims -> claims.get("userId", String.class));
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Extract user email from token if present
     * @param token JWT token
     * @return user email or null if not present
     */
    public String extractUserEmail(String token) {
        try {
            return extractClaim(token, claims -> claims.get("email", String.class));
        } catch (Exception e) {
            return null;
        }
    }
}