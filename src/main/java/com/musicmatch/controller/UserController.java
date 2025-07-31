package com.musicmatch.controller;

import com.musicmatch.model.User;
import com.musicmatch.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(Authentication authentication) {
        String spotifyId = authentication.getName(); // comes from JWT

        Optional<User> userOpt = userProfileRepository.findBySpotifyId(spotifyId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        return ResponseEntity.ok(userOpt.get());
    }
}
