package com.musicmatch.service;

import com.musicmatch.model.UserProfile;
import com.musicmatch.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserProfileRepository userRepo;

    public Optional<UserProfile> getUserBySpotifyId(String spotifyId) {
        return userRepo.findBySpotifyId(spotifyId);
    }

    public UserProfile saveUser(UserProfile user) {
        return userRepo.save(user);
    }
}
