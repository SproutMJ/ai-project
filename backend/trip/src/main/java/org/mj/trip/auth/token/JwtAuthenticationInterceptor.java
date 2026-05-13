package org.mj.trip.auth.token;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mj.trip.common.exception.MemberNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String MEMBER_ID_ATTRIBUTE = "memberId";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authorizationHeader = request.getHeader(HEADER_AUTHORIZATION);

        if (authorizationHeader == null || !authorizationHeader.startsWith(TOKEN_PREFIX)) {
            log.warn("Authorization header is missing or invalid");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }

        String token = authorizationHeader.substring(TOKEN_PREFIX.length());

        try {
            if (!jwtTokenProvider.validateToken(token)) {
                log.warn("Invalid JWT token");
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                return false;
            }

            Long memberId = jwtTokenProvider.getMemberId(token);
            request.setAttribute(MEMBER_ID_ATTRIBUTE, memberId);
            log.info("Authenticated member: {}", memberId);

            return true;
        } catch (Exception e) {
            log.error("Failed to authenticate user", e);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }
    }

    public static Long getMemberIdFromRequest(HttpServletRequest request) {
        Object memberId = request.getAttribute("memberId");
        if (memberId == null) {
            throw new MemberNotFoundException("인증된 사용자를 찾을 수 없습니다.");
        }
        return (Long) memberId;
    }
}
