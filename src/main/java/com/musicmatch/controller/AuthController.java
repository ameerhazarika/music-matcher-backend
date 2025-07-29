package com.musicmatch.controller;

import com.musicmatch.model.UserProfile;
import com.musicmatch.service.SpotifyAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    @Value("${spotify.redirect-uri}")
    private String redirectUri;

    private final SpotifyAuthService spotifyAuthService;

    public AuthController(SpotifyAuthService spotifyAuthService) {
        this.spotifyAuthService = spotifyAuthService;
    }

    @GetMapping("/login")
    public ResponseEntity<Void> login() {
        String scopes = "user-top-read user-read-email";
        String encodedScopes = scopes.replace(" ", "%20");

        String url = UriComponentsBuilder
                .fromUriString("https://accounts.spotify.com/authorize")
                .queryParam("client_id", clientId) // or inject clientId directly
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", encodedScopes)
                .build(true)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(url));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @GetMapping("/callback")
    public ResponseEntity<?> callback(@RequestParam String code) {
        try {
            UserProfile userProfile = spotifyAuthService.authenticateUser(code);
            return ResponseEntity.ok(userProfile);  // returns saved user data
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
