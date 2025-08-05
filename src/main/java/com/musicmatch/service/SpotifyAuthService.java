package com.musicmatch.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.musicmatch.model.User;
import com.musicmatch.payload.response.SpotifyTokenResponse;
import com.musicmatch.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Instant;
import java.util.*;

@Service
public class SpotifyAuthService {

    private static final Logger logger = LoggerFactory.getLogger(SpotifyAuthService.class);

    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    @Value("${spotify.redirect-uri}")
    private String redirectUri;

    private final UserProfileRepository userProfileRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    public SpotifyAuthService(UserProfileRepository userProfileRepository, ObjectMapper objectMapper) {
        this.userProfileRepository = userProfileRepository;
        this.objectMapper = objectMapper;
    }

    public User authenticateUser(String code) {
        logger.info("Starting authentication flow for code: {}", code);

        SpotifyTokenResponse tokenResponse = exchangeCodeForToken(code);
        logger.debug("Token response received: {}", tokenResponse);

        Map<String, Object> profileData = fetchUserProfile(tokenResponse.getAccessToken());
        logger.debug("Fetched Spotify user profile: {}", profileData);

        User savedProfile = saveOrUpdateUserProfile(tokenResponse, profileData);
        logger.info("User profile saved/updated with ID: {}", savedProfile.getSpotifyId());

        return savedProfile;
    }

    private SpotifyTokenResponse exchangeCodeForToken(String code) {
        logger.info("Exchanging authorization code for access token...");

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
            logger.error("Failed to exchange code for token: {}", e.getResponseBodyAsString(), e);
            throw new RuntimeException("Failed to exchange code for token", e);
        }
    }

    private Map<String, Object> fetchUserProfile(String accessToken) {
        logger.info("Fetching user profile from Spotify API...");

        String url = "https://api.spotify.com/v1/me";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            logger.error("Failed to fetch Spotify user profile: {}", e.getResponseBodyAsString(), e);
            throw new RuntimeException("Failed to fetch Spotify user profile", e);
        }
    }

    private User saveOrUpdateUserProfile(SpotifyTokenResponse tokenResponse, Map<String, Object> profileData) {
        String spotifyId = (String) profileData.get("id");
        String displayName = (String) profileData.get("display_name");
        String email = (String) profileData.get("email");

        List<Map<String, Object>> images = (List<Map<String, Object>>) profileData.get("images");
        String profileImage = (images != null && !images.isEmpty()) ? (String) images.get(0).get("url") : null;

        Optional<User> existingUserOpt = userProfileRepository.findBySpotifyId(spotifyId);

        User userProfile = existingUserOpt.orElse(new User());
        userProfile.setSpotifyId(spotifyId);
        userProfile.setDisplayName(displayName);
        userProfile.setEmail(email);
        userProfile.setProfileImage(profileImage);
        userProfile.setAccessToken(tokenResponse.getAccessToken());
        userProfile.setRefreshToken(tokenResponse.getRefreshToken());
        userProfile.setTokenExpiry(Instant.now().plusSeconds(tokenResponse.getExpiresIn()));

        return userProfileRepository.save(userProfile);
    }
    /**
     * Fetches user's top tracks from Spotify API
     * @param accessToken The user's Spotify access token
     * @return List of top tracks data
     */
    public List<Map<String, Object>> getUserTopTracks(String accessToken) {
        try {
            String url = "https://api.spotify.com/v1/me/top/tracks?limit=20&time_range=medium_term";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            logger.info("Fetching user top tracks from Spotify API");

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                JsonNode items = jsonResponse.get("items");

                List<Map<String, Object>> topTracks = new ArrayList<>();

                for (JsonNode item : items) {
                    Map<String, Object> track = new HashMap<>();
                    track.put("id", item.get("id").asText());
                    track.put("name", item.get("name").asText());
                    track.put("popularity", item.get("popularity").asInt());
                    track.put("duration_ms", item.get("duration_ms").asLong());
                    track.put("explicit", item.get("explicit").asBoolean());
                    track.put("preview_url", item.has("preview_url") && !item.get("preview_url").isNull()
                            ? item.get("preview_url").asText() : null);

                    // Extract artists
                    List<Map<String, Object>> artists = new ArrayList<>();
                    JsonNode artistsNode = item.get("artists");
                    for (JsonNode artist : artistsNode) {
                        Map<String, Object> artistData = new HashMap<>();
                        artistData.put("id", artist.get("id").asText());
                        artistData.put("name", artist.get("name").asText());
                        artistData.put("type", artist.get("type").asText());
                        artists.add(artistData);
                    }
                    track.put("artists", artists);

                    // Extract album information
                    JsonNode albumNode = item.get("album");
                    Map<String, Object> album = new HashMap<>();
                    album.put("id", albumNode.get("id").asText());
                    album.put("name", albumNode.get("name").asText());
                    album.put("release_date", albumNode.get("release_date").asText());
                    album.put("total_tracks", albumNode.get("total_tracks").asInt());

                    // Extract album images
                    List<Map<String, Object>> images = new ArrayList<>();
                    JsonNode imagesNode = albumNode.get("images");
                    for (JsonNode image : imagesNode) {
                        Map<String, Object> imageData = new HashMap<>();
                        imageData.put("url", image.get("url").asText());
                        imageData.put("height", image.has("height") && !image.get("height").isNull()
                                ? image.get("height").asInt() : null);
                        imageData.put("width", image.has("width") && !image.get("width").isNull()
                                ? image.get("width").asInt() : null);
                        images.add(imageData);
                    }
                    album.put("images", images);
                    track.put("album", album);

                    topTracks.add(track);
                }

                logger.info("Successfully fetched {} top tracks", topTracks.size());
                return topTracks;

            } else {
                logger.error("Failed to fetch top tracks. Status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to fetch user top tracks from Spotify");
            }

        } catch (Exception e) {
            logger.error("Error fetching user top tracks: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch user top tracks", e);
        }
    }

    /**
     * Fetches user's top artists from Spotify API
     * @param accessToken The user's Spotify access token
     * @return List of top artists data
     */
    public List<Map<String, Object>> getUserTopArtists(String accessToken) {
        try {
            String url = "https://api.spotify.com/v1/me/top/artists?limit=20&time_range=medium_term";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            logger.info("Fetching user top artists from Spotify API");

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                JsonNode items = jsonResponse.get("items");

                List<Map<String, Object>> topArtists = new ArrayList<>();

                for (JsonNode item : items) {
                    Map<String, Object> artist = new HashMap<>();
                    artist.put("id", item.get("id").asText());
                    artist.put("name", item.get("name").asText());
                    artist.put("popularity", item.get("popularity").asInt());
                    artist.put("followers", item.get("followers").get("total").asInt());

                    // Extract genres
                    List<String> genres = new ArrayList<>();
                    JsonNode genresNode = item.get("genres");
                    for (JsonNode genre : genresNode) {
                        genres.add(genre.asText());
                    }
                    artist.put("genres", genres);

                    // Extract images
                    List<Map<String, Object>> images = new ArrayList<>();
                    JsonNode imagesNode = item.get("images");
                    for (JsonNode image : imagesNode) {
                        Map<String, Object> imageData = new HashMap<>();
                        imageData.put("url", image.get("url").asText());
                        imageData.put("height", image.has("height") && !image.get("height").isNull()
                                ? image.get("height").asInt() : null);
                        imageData.put("width", image.has("width") && !image.get("width").isNull()
                                ? image.get("width").asInt() : null);
                        images.add(imageData);
                    }
                    artist.put("images", images);

                    topArtists.add(artist);
                }

                logger.info("Successfully fetched {} top artists", topArtists.size());
                return topArtists;

            } else {
                logger.error("Failed to fetch top artists. Status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to fetch user top artists from Spotify");
            }

        } catch (Exception e) {
            logger.error("Error fetching user top artists: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch user top artists", e);
        }
    }

    /**
     * Refreshes an access token using a refresh token
     * @param refreshToken The refresh token
     * @return New access token
     */
    public String refreshAccessToken(String refreshToken) {
        try {
            String url = "https://accounts.spotify.com/api/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(clientId, clientSecret);

            String body = "grant_type=refresh_token&refresh_token=" + refreshToken;

            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                return jsonResponse.get("access_token").asText();
            } else {
                throw new RuntimeException("Failed to refresh access token");
            }

        } catch (Exception e) {
            logger.error("Error refreshing access token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to refresh access token", e);
        }
    }
}
