package se.docksidelabs.airhockeyserver.config;

import java.util.List;
import java.util.Optional;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * STOMP channel interceptor that validates JWT access tokens
 * on CONNECT frames. Rejects unauthenticated connections.
 */
@Component
public class StompAuthInterceptor implements ChannelInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(StompAuthInterceptor.class);

    private final JwtValidator jwtValidator;

    public StompAuthInterceptor(JwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);

            if (token == null) {
                logger.warn("STOMP CONNECT rejected: no Authorization header");
                throw new IllegalArgumentException("Missing Authorization header");
            }

            Optional<JwtClaims> claims = jwtValidator.validate(token);
            if (claims.isEmpty()) {
                logger.warn("STOMP CONNECT rejected: invalid JWT");
                throw new IllegalArgumentException("Invalid or expired token");
            }

            try {
                logger.debug("STOMP CONNECT authenticated for user: {}", claims.get().getSubject());
            } catch (MalformedClaimException e) {
                logger.debug("STOMP CONNECT authenticated (could not read subject)");
            }
        }

        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders == null || authHeaders.isEmpty()) {
            return null;
        }

        String authHeader = authHeaders.getFirst();
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring("Bearer ".length());
        }

        return null;
    }
}
