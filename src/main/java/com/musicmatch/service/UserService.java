package com.musicmatch.service;

import com.musicmatch.model.User;
import com.musicmatch.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserProfileRepository userRepo;

    public Optional<User> getUserBySpotifyId(String spotifyId) {
        return userRepo.findBySpotifyId(spotifyId);
    }

    public User saveUser(User user) {
        return userRepo.save(user);
    }
}
