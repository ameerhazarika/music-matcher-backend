package com.musicmatch.payload.response;

public class JwtResponse {
    private String token;
    private String displayName;
    private String email;

    public JwtResponse(String token, String displayName, String email) {
        this.token = token;
        this.displayName = displayName;
        this.email = email;
    }

    // getters and setters
    public String getToken() {
        return token;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }
}
