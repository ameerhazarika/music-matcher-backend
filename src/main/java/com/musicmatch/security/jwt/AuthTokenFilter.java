package com.musicmatch.security.jwt;

import com.musicmatch.security.services.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        logger.warn("🔥 AuthTokenFilter triggered for path: " + request.getRequestURI());
        logger.warn("🔥 Auth header in AuthTokenFilter: " + request.getHeader("Authorization"));
        String path = request.getRequestURI();
        logger.debug("Incoming request to: " + path);

        // Skip filtering for public routes
        if (path.equals("/api/auth/login") || path.equals("/api/auth/callback")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = parseJwt(request);
            logger.debug("Authorization header: " + request.getHeader("Authorization"));
            if (jwt != null) {
                boolean valid = jwtUtils.validateJwtToken(jwt);
                logger.debug("Token valid? REALLY REALLY VALID " + valid);

                if (valid) {
                    String spotifyId = jwtUtils.getSpotifyIdFromJwt(jwt);
                    var userDetails = userDetailsService.loadUserByUsername(spotifyId);
                    logger.debug("Loaded user details: " + userDetails.getUsername() + ", authorities: " + userDetails.getAuthorities());

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities());

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } else {
                logger.debug("No JWT token found in request");
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication", e);
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }
}
