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
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip filtering for public routes
        String path = request.getRequestURI();
        return path.equals("/api/auth/login") || path.equals("/api/auth/callback");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        logger.warn("🔥 AuthTokenFilter triggered for path: " + path);
        logger.warn("🔥 Authorization header: " + request.getHeader("Authorization"));
        logger.warn("🔥 Request method: " + request.getMethod());
        logger.warn("🔥 Query string: " + request.getQueryString());
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) ||
                path.equals("/api/auth/login") ||
                path.equals("/api/auth/callback")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = parseJwt(request);

            if (jwt != null) {
                logger.warn("✅ JWT token found: " + jwt);

                boolean valid = jwtUtils.validateJwtToken(jwt);
                logger.warn("✅ Token valid? " + valid);

                if (valid) {
                    String spotifyId = jwtUtils.getSpotifyIdFromJwt(jwt);
                    var userDetails = userDetailsService.loadUserByUsername(spotifyId);

                    logger.warn("✅ Loaded user details: " + userDetails.getUsername() + ", authorities: " + userDetails.getAuthorities());

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities());

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.warn("✅ Authentication set in SecurityContextHolder for: " + spotifyId);
                }
            } else {
                logger.warn("🚨 No JWT token found in request");
            }
        } catch (Exception e) {
            logger.error("❌ Cannot set user authentication", e);
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
