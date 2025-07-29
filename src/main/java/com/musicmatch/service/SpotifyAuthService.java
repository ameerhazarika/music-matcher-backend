package com.musicmatch.service;

import com.musicmatch.model.UserProfile;
import com.musicmatch.payload.response.SpotifyTokenResponse;
import com.musicmatch.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SpotifyAuthService {

    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    @Value("${spotify.redirect-uri}")
    private String redirectUri;

    private final UserProfileRepository userProfileRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public SpotifyAuthService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public UserProfile authenticateUser(String code) {
        SpotifyTokenResponse tokenResponse = exchangeCodeForToken(code);
        Map<String, Object> profileData = fetchUserProfile(tokenResponse.getAccessToken());

        return saveOrUpdateUserProfile(tokenResponse, profileData);
    }

    private SpotifyTokenResponse exchangeCodeForToken(String code) {
        String tokenUrl = "https://accounts.spotify.com/api/token";

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
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Failed to exchange code for token: " + e.getResponseBodyAsString(), e);
        }
    }

    private Map<String, Object> fetchUserProfile(String accessToken) {
        String url = "https://api.spotify.com/v1/me";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Failed to fetch Spotify user profile: " + e.getResponseBodyAsString(), e);
        }
    }

    private UserProfile saveOrUpdateUserProfile(SpotifyTokenResponse tokenResponse, Map<String, Object> profileData) {
        String spotifyId = (String) profileData.get("id");
        String displayName = (String) profileData.get("display_name");
        String email = (String) profileData.get("email");

        List<Map<String, Object>> images = (List<Map<String, Object>>) profileData.get("images");
        String profileImage = (images != null && !images.isEmpty()) ? (String) images.get(0).get("url") : null;

        Optional<UserProfile> existingUserOpt = userProfileRepository.findBySpotifyId(spotifyId);

        UserProfile userProfile = existingUserOpt.orElse(new UserProfile());
        userProfile.setSpotifyId(spotifyId);
        userProfile.setDisplayName(displayName);
        userProfile.setEmail(email);
        userProfile.setProfileImage(profileImage);
        userProfile.setAccessToken(tokenResponse.getAccessToken());
        userProfile.setRefreshToken(tokenResponse.getRefreshToken());
        userProfile.setTokenExpiry(Instant.now().plusSeconds(tokenResponse.getExpiresIn()));

        return userProfileRepository.save(userProfile);
    }
}
