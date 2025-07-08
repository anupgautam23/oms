package com.oms.order.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Client for communicating with Auth Service
 */
@Component
public class UserServiceClient {
    
    private static final Logger logger = LoggerFactory.getLogger(UserServiceClient.class);
    
    @Value("${auth.service.url:http://localhost:8081}")
    private String authServiceUrl;
    
    private final RestTemplate restTemplate;
    
    public UserServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * Get user details from Auth Service using JWT token
     * @param token JWT token
     * @return UserDetails or null if failed
     */
    public UserDetails getUserDetails(String token) {
        try {
            String url = authServiceUrl + "/api/auth/me";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<UserDetails> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                entity, 
                UserDetails.class
            );
            
            logger.debug("Successfully retrieved user details from auth service");
            return response.getBody();
            
        } catch (Exception e) {
            logger.error("Failed to get user details from auth service: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Validate JWT token with Auth Service
     * @param token JWT token
     * @return true if token is valid
     */
    public boolean validateToken(String token) {
        try {
            String url = authServiceUrl + "/api/auth/validate";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                entity, 
                String.class
            );
            
            return response.getStatusCode().is2xxSuccessful();
            
        } catch (Exception e) {
            logger.error("Failed to validate token with auth service: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Inner class to represent user details from Auth Service
     */
    public static class UserDetails {
        @JsonProperty("id")
        private Long id;
        
        @JsonProperty("username")
        private String username;
        
        @JsonProperty("email")
        private String email;
        
        @JsonProperty("isActive")
        private boolean isActive;
        
        // Constructors
        public UserDetails() {}
        
        public UserDetails(Long id, String username, String email, boolean isActive) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.isActive = isActive;
        }
        
        // Getters and Setters
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public boolean isActive() {
            return isActive;
        }
        
        public void setActive(boolean active) {
            isActive = active;
        }
        
        @Override
        public String toString() {
            return "UserDetails{" +
                    "id=" + id +
                    ", username='" + username + '\'' +
                    ", email='" + email + '\'' +
                    ", isActive=" + isActive +
                    '}';
        }
    }
}