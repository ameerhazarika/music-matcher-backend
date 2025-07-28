package com.musicmatch.controller;

import com.musicmatch.payload.response.SpotifyTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Value("${spotify.client-id}")
    private String clientId; // client id

    @Value("${spotify.client-secret}")
    private String clientSecret;

    @Value("${spotify.redirect-uri}")
    private String redirectUri;

    // Step 1: Redirect user to Spotify login
    @GetMapping("/login")
    public ResponseEntity<Void> login() {
        System.out.println("Code working");
        String scopes = "user-top-read user-read-email";
        // Manually encode spaces as %20
        String encodedScopes = scopes.replace(" ", "%20");

        String url = UriComponentsBuilder
                .fromUriString("https://accounts.spotify.com/authorize")
                .queryParam("client_id", clientId)
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", encodedScopes)
                .build(true)  // encode other parts if needed
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(url));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    // Step 2: Handle callback and exchange code for access token

    @GetMapping("/callback")
    public ResponseEntity<?> callback(@RequestParam String code) {
        System.out.println("Received code: " + code);

        String tokenUrl = "https://accounts.spotify.com/api/token";

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(clientId, clientSecret);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        try {
            ResponseEntity<SpotifyTokenResponse> response = restTemplate.postForEntity(tokenUrl, request, SpotifyTokenResponse.class);

            SpotifyTokenResponse tokenResponse = response.getBody();

            System.out.println("Access Token: " + tokenResponse.getAccessToken());
            System.out.println("Refresh Token: " + tokenResponse.getRefreshToken());

            // Return the tokenResponse object (will be serialized to JSON automatically)
            return ResponseEntity.ok(tokenResponse);

        } catch (HttpClientErrorException e) {
            System.out.println("Spotify error:");
            System.out.println("Status Code: " + e.getStatusCode());
            System.out.println("Response Body: " + e.getResponseBodyAsString());

            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }
    @GetMapping("/profile")
    public ResponseEntity<String> getUserProfile(@RequestParam String accessToken) {
        String url = "https://api.spotify.com/v1/me";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);  // Add the access token here

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch profile");
        }
    }


}
