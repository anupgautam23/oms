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
     * Validate JWT token
     * @param token JWT token
     * @param userDetails user details
     * @return true if token is valid
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
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
}