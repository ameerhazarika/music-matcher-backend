package com.musicmatch.repository;

import com.musicmatch.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserProfileRepository extends MongoRepository<User, String> {
    Optional<User> findBySpotifyId(String spotifyId);
    List<User> findBySpotifyIdNot (String spotifyId);
}
