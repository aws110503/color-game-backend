package com.colorgame.backend.security;

import com.colorgame.backend.repository.JwtBlacklistRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final JwtBlacklistRepository jwtBlacklistRepository;

    public JwtFilter(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService, JwtBlacklistRepository jwtBlacklistRepository) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.jwtBlacklistRepository = jwtBlacklistRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (jwtUtil.isTokenValid(token)) {
            // SECURITY: Check if token is blacklisted (logged out)
            System.out.println("✅ Token valid for: " + jwtUtil.extractUsername(token));
            String jti = jwtUtil.extractJti(token);
            if (jti != null && jwtBlacklistRepository.existsByJti(jti)) {
                filterChain.doFilter(request, response);
                return;
            }

            String username = jwtUtil.extractUsername(token);

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
            System.out.println("✅ Authentication set for: " + username);
        } else {
            System.out.println("❌ isTokenValid returned FALSE");
        
        }

        filterChain.doFilter(request, response);
    }
}
