package se.docksidelabs.airhockeyserver.config;

import java.util.Optional;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.HmacKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Validates JWT access tokens issued by the gateway.
 * Uses the same HMAC-SHA256 shared secret.
 */
@Component
public class JwtValidator {
    private static final Logger logger = LoggerFactory.getLogger(JwtValidator.class);
    private static final String ISSUER = "air-hockey-gateway";

    private final JwtConsumer jwtConsumer;

    public JwtValidator(@Value("${jwt.secret}") String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 characters");
        }

        this.jwtConsumer = new JwtConsumerBuilder()
                .setRequireExpirationTime()
                .setRequireSubject()
                .setExpectedIssuer(ISSUER)
                .setVerificationKey(new HmacKey(secret.getBytes()))
                .build();
    }

    /**
     * Validate a JWT access token and return its claims if valid.
     */
    public Optional<JwtClaims> validate(String token) {
        try {
            JwtClaims claims = jwtConsumer.processToClaims(token);

            String type = claims.getStringClaimValue("type");
            if (!"access".equals(type)) {
                logger.warn("Token type mismatch: expected access, got {}", type);
                return Optional.empty();
            }

            return Optional.of(claims);
        } catch (InvalidJwtException e) {
            logger.debug("JWT validation failed: {}", e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            logger.warn("Unexpected error validating JWT", e);
            return Optional.empty();
        }
    }
}
