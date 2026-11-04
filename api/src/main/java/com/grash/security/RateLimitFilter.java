package com.grash.security;

import com.grash.exception.CustomException;
import com.grash.service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that applies rate limiting to authenticated users based on their user ID.
 * This filter should be added after authentication filters (JwtTokenFilter, ApiKeyAuthFilter)
 * to ensure the user is already authenticated.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final ClientIpResolver clientIpResolver;

    /**
     * The STOMP/SockJS handshake (/ws/info, transport probes) cannot carry the JWT header, so it would always
     * count against the per-IP anonymous bucket. Browsers repeat it on every page load and reconnect, which
     * exhausted the anonymous limit for everyone sharing an IP (e.g. an office NAT) and then blocked logins.
     * The handshake exposes no data; message delivery itself is authenticated separately.
     */
    @Override
    protected boolean shouldNotFilter(@NotNull HttpServletRequest request) {
        String path = request.getServletPath();
        return path != null && (path.equals("/ws") || path.startsWith("/ws/"));
    }

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain) throws ServletException, IOException {
        // Skip rate limiting if disabled
        if (!rateLimiterService.isRateLimitEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get the current authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof CustomUserDetail userDetail) {
            Long userId = userDetail.getUser().getId();
            String userIdKey = "user:" + userId;

            // Check rate limit
            if (!rateLimiterService.resolveAuthenticatedUserBucket(userIdKey).tryConsume(1)) {
                log.warn("Rate limit exceeded for user ID: {}", userId);
                setResponseTooManyRequests(response);
                return;
            }
        } else {
            String clientIp = clientIpResolver.resolve(request);
            if (!rateLimiterService.resolveUnAuthenticatedUserBucket(clientIp).tryConsume(1)) {
                setResponseTooManyRequests(response);
                return;
            }
        }

        // Continue the filter chain
        filterChain.doFilter(request, response);
    }

    private static void setResponseTooManyRequests(@NotNull HttpServletResponse response) throws IOException {
        response.setStatus(429); // HTTP 429 Too Many Requests
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. " +
                "Please try again later.\"}");
    }
}
