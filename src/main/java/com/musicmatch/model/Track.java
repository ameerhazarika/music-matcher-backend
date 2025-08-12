package com.musicmatch.model;

public class Track {
    private String name;
    private String artist;
    private String albumImageUrl;

    // Constructors
    public Track() {}

    public Track(String name, String artist, String albumImageUrl) {
        this.name = name;
        this.artist = artist;
        this.albumImageUrl = albumImageUrl;
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbumImageUrl() {
        return albumImageUrl;
    }

    public void setAlbumImageUrl(String albumImageUrl) {
        this.albumImageUrl = albumImageUrl;
    }
}
