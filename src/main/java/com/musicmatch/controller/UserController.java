package com.musicmatch.controller;

import com.musicmatch.model.User;
import com.musicmatch.repository.UserProfileRepository;
import com.musicmatch.service.SpotifyAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.sound.midi.Track;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
public class UserController {

  //  @Autowired
   // private UserProfileRepository userProfileRepository;
    private final UserProfileRepository userProfileRepository;
    private final SpotifyAuthService spotifyAuthService;
    public UserController(UserProfileRepository userProfileRepository, SpotifyAuthService spotifyAuthService){
        this.userProfileRepository=userProfileRepository;
        this.spotifyAuthService = spotifyAuthService;
    }
//    @GetMapping("/profile")
//    public ResponseEntity<?> getUserProfile(Authentication authentication) {
//        String spotifyId = authentication.getName(); // comes from JWT
//
//        Optional<User> userOpt = userProfileRepository.findBySpotifyId(spotifyId);
//        if (userOpt.isEmpty()) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
//        }
//
//        return ResponseEntity.ok(userOpt.get());
//    }
    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(Authentication authentication) {
        try {
            String spotifyId = authentication.getName(); // Extract from JWT
            Optional<User>user = userProfileRepository.findBySpotifyId(spotifyId);
            if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
             }
            // Fetch top tracks from Spotify API using stored access token
            User actualUser = user.get(); // ✅ Extract user from Optional
            List<Map<String, Object>> topTracks = spotifyAuthService.getUserTopTracks(actualUser.getAccessToken());

            Map<String, Object> response = new HashMap<>();
            response.put("user", actualUser);
            response.put("topTracks", topTracks);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch user profile");
        }
    }
    @GetMapping("/discover")
    public ResponseEntity<List<User>> getAllOtherUsers(Authentication authentication) {
        String spotifyId = authentication.getName();
        List<User> allUsers = userProfileRepository.findBySpotifyIdNot(spotifyId);

        List<User> otherUsers = userProfileRepository.findBySpotifyIdNot(spotifyId);

        otherUsers.forEach(user -> {
            try {
                List<Map<String, Object>> topTracks = spotifyAuthService.getUserTopTracks(user.getAccessToken());
                user.setTopTracks(topTracks);
            } catch (Exception e) {
                System.out.println("Failed to fetch top tracks for user: " + user.getSpotifyId());
            }
        });

        return ResponseEntity.ok(otherUsers);
    }

}
