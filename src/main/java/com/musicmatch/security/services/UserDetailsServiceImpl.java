package com.musicmatch.security.services;

import com.musicmatch.model.User;
import com.musicmatch.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserProfileRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String spotifyId) throws UsernameNotFoundException {
        User user = userRepository.findBySpotifyId(spotifyId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with spotifyId: " + spotifyId));

        return new UserDetailsImpl(user);
    }
}
