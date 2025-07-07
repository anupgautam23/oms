package com.oms.notification.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

/**
 * JWT Utility class for handling JWT token operations in Notification Service
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
     * Validate JWT token (simplified version for notification service)
     * @param token JWT token
     * @return true if token is valid
     */
    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (JwtException e) {
            // Log the specific JWT exception for debugging
            System.err.println("JWT validation failed: " + e.getMessage());
            return false;
        } catch (Exception e) {
            // Log any other unexpected exceptions
            System.err.println("Token validation error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Validate JWT token with username verification
     * @param token JWT token
     * @param expectedUsername expected username
     * @return true if token is valid and username matches
     */
    public boolean isTokenValid(String token, String expectedUsername) {
        try {
            final String username = extractUsername(token);
            return username.equals(expectedUsername) && !isTokenExpired(token);
        } catch (JwtException e) {
            System.err.println("JWT validation failed: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Token validation error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Extract user ID from JWT token (if present in claims)
     * @param token JWT token
     * @return user ID or null if not present
     */
    public String extractUserId(String token) {
        try {
            return extractClaim(token, claims -> claims.get("userId", String.class));
        } catch (Exception e) {
            System.err.println("Error extracting user ID: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Extract user email from JWT token (if present in claims)
     * @param token JWT token
     * @return user email or null if not present
     */
    public String extractUserEmail(String token) {
        try {
            return extractClaim(token, claims -> claims.get("email", String.class));
        } catch (Exception e) {
            System.err.println("Error extracting user email: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get JWT expiration time
     * @return expiration time in milliseconds
     */
    public long getExpirationTime() {
        return jwtExpiration;
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
     * Utility method to safely extract string claim
     * @param token JWT token
     * @param claimName name of the claim
     * @return claim value or null if not present
     */
    public String extractStringClaim(String token, String claimName) {
        try {
            return extractClaim(token, claims -> claims.get(claimName, String.class));
        } catch (Exception e) {
            System.err.println("Error extracting claim '" + claimName + "': " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if token is about to expire (within 5 minutes)
     * @param token JWT token
     * @return true if token expires within 5 minutes
     */
    public boolean isTokenAboutToExpire(String token) {
        try {
            Date expiration = extractExpiration(token);
            Date now = new Date();
            long timeUntilExpiration = expiration.getTime() - now.getTime();
            return timeUntilExpiration < 300000; // 5 minutes in milliseconds
        } catch (Exception e) {
            System.err.println("Error checking token expiration: " + e.getMessage());
            return true; // Assume it's about to expire if we can't check
        }
    }
}