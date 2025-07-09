package com.oms.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EmailRequestDto {
    
    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid email format")
    private String to;
    
    @NotBlank(message = "Subject is required")
    @Size(max = 255, message = "Subject must not exceed 255 characters")
    private String subject;
    
    @NotBlank(message = "Body is required")
    @Size(max = 5000, message = "Body must not exceed 5000 characters")
    private String body;
    
    // Default constructor
    public EmailRequestDto() {}
    
    // Constructor with parameters
    public EmailRequestDto(String to, String subject, String body) {
        this.to = to;
        this.subject = subject;
        this.body = body;
    }
    
    // Getters and setters
    public String getTo() {
        return to;
    }
    
    public void setTo(String to) {
        this.to = to;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public String getBody() {
        return body;
    }
    
    public void setBody(String body) {
        this.body = body;
    }
    
    @Override
    public String toString() {
        return "EmailRequestDto{" +
                "to='" + to + '\'' +
                ", subject='" + subject + '\'' +
                ", body='" + (body != null ? body.substring(0, Math.min(50, body.length())) + "..." : "null") + '\'' +
                '}';
    }
}