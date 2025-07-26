package com.musicmatch.repository;

import com.musicmatch.model.UserProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface UserProfileRepository extends MongoRepository<UserProfile, String> {
    Optional<UserProfile> findBySpotifyId(String spotifyId);
}
