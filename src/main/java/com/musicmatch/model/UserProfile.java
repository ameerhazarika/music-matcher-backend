package com.musicmatch.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document("users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {
    @Id
    private String id;
    private String spotifyId;
    private String name;
    private String email;
    private String profileImageUrl;

    private List<String> topArtists;
    private List<String> topGenres;
    private List<String> topTracks;
}
