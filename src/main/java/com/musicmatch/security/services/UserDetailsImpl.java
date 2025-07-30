package com.musicmatch.security.services;

import com.musicmatch.model.UserProfile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class UserDetailsImpl implements UserDetails {

    private final UserProfile user;

    public UserDetailsImpl(UserProfile user) {
        this.user = user;
    }

    public UserProfile getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList(); // Add roles if needed
    }

    @Override
    public String getPassword() {
        return null; // Password not used in Spotify login
    }

    @Override
    public String getUsername() {
        return user.getSpotifyId(); // Used as the principal identifier
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
