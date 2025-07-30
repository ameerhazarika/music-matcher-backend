package com.musicmatch.controller;

import com.musicmatch.model.UserProfile;
import com.musicmatch.payload.response.JwtResponse;
import com.musicmatch.security.jwt.JwtUtils;
import com.musicmatch.service.SpotifyAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    @Value("${spotify.redirect-uri}")
    private String redirectUri;

    private final SpotifyAuthService spotifyAuthService;
    private final JwtUtils jwtUtils;
    public AuthController(SpotifyAuthService spotifyAuthService, JwtUtils jwtUtils) {
        this.spotifyAuthService = spotifyAuthService;
        this.jwtUtils = jwtUtils;
    }


    @GetMapping("/login")
    public ResponseEntity<Void> login() {
        String scopes = "user-top-read user-read-email";
        String encodedScopes = scopes.replace(" ", "%20");

        String url = UriComponentsBuilder
                .fromUriString("https://accounts.spotify.com/authorize")
                .queryParam("client_id", clientId)
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", encodedScopes)
                .build(true)
                .toUriString();

        logger.info("Redirecting user to Spotify login: {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(url));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @GetMapping("/callback")
    public ResponseEntity<?> callback(@RequestParam String code) {
        try {
            UserProfile userProfile = spotifyAuthService.authenticateUser(code);
            String jwt = jwtUtils.generateJwtFromSpotifyId(userProfile.getSpotifyId());
            JwtResponse jwtResponse = new JwtResponse(jwt, userProfile.getDisplayName(), userProfile.getEmail());
            return ResponseEntity.ok(jwtResponse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Authentication failed: " + e.getMessage());
        }
    }
}
