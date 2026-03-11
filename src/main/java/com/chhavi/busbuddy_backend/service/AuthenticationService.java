package com.chhavi.busbuddy_backend.service;

import com.chhavi.busbuddy_backend.config.TokenProperties;
import com.chhavi.busbuddy_backend.constant.FirestoreCollections;
import com.chhavi.busbuddy_backend.constant.UserRole;
import com.chhavi.busbuddy_backend.exception.ResourceNotFoundException;
import com.chhavi.busbuddy_backend.exception.UnauthorizedException;
import com.chhavi.busbuddy_backend.gateway.FirebaseGateway;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Encapsulates token-related operations so controllers do not need to know
 * about JWT internals or Firebase user existence checks.
 */
@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final FirebaseGateway firebaseGateway;
    private final TokenProperties tokenProperties;

    public AuthenticationService(FirebaseGateway firebaseGateway, TokenProperties tokenProperties) {
        this.firebaseGateway = firebaseGateway;
        this.tokenProperties = tokenProperties;
    }

    /**
     * Generates an application-specific JWT only after verifying that the UID
     * exists in Firebase Auth. This prevents the backend from minting tokens
     * for arbitrary identifiers that are unknown to the identity provider.
     */
    /**
     * Issues a backend JWT after verifying a Firebase ID token (Option A login).
     *
     * This is the secure way to authenticate because the backend only trusts a
     * UID that Firebase has authenticated and signed.
     */
    public com.chhavi.busbuddy_backend.dto.auth.LoginResponse loginWithFirebaseIdToken(String firebaseIdToken) {
        if (firebaseIdToken == null || firebaseIdToken.isBlank()) {
            throw new UnauthorizedException("firebaseIdToken must not be blank");
        }

        final FirebaseToken decodedToken;
        try {
            decodedToken = firebaseGateway.auth().verifyIdToken(firebaseIdToken);
        } catch (FirebaseAuthException exception) {
            log.warn("Firebase ID token verification failed: {}", exception.getMessage());
            throw new UnauthorizedException("Invalid Firebase ID token");
        }

        String uid = decodedToken.getUid();
        DocumentSnapshot employeeDocument;
        try {
            employeeDocument = firebaseGateway.collection(FirestoreCollections.EMPLOYEES)
                    .document(uid)
                    .get()
                    .get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new UnauthorizedException("Interrupted while loading authorization data");
        } catch (Exception exception) {
            throw new UnauthorizedException("Unable to load authorization data");
        }

        if (!employeeDocument.exists()) {
            throw new ResourceNotFoundException("Employee profile not found for uid: " + uid);
        }

        String roleValue = employeeDocument.getString("role");
        UserRole role = UserRole.from(roleValue);
        String companyId = employeeDocument.getString("companyId");
        if (companyId == null || companyId.isBlank()) {
            throw new UnauthorizedException("Employee is not assigned to a company");
        }

        String token = buildBackendJwt(uid, role, companyId);
        return new com.chhavi.busbuddy_backend.dto.auth.LoginResponse(token, role.name(), companyId);
    }

    private String buildBackendJwt(String uid, UserRole role, String companyId) {
        Date issuedAt = new Date();
        Date expiration = new Date(issuedAt.getTime() + tokenProperties.getExpirationTimeMs());
        SecretKey key = signingKey();

        log.info("Issuing backend JWT for uid={} with role={}", uid, role);
        return Jwts.builder()
                .subject(uid)
                .claim("role", role.name())
                .claim("companyId", companyId)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    /**
     * Verifies tokens issued by this backend. The method accepts the raw
     * Authorization header because controllers should stay thin and avoid
     * security-specific header parsing logic.
     */
    public boolean verifyCustomToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing or invalid Authorization header");
        }

        try {
            Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(authorizationHeader.substring(7));
            return true;
        } catch (Exception exception) {
            log.warn("Custom token verification failed: {}", exception.getMessage());
            throw new UnauthorizedException("Invalid or expired token");
        }
    }

    /**
     * Derives a HMAC signing key from configured secret text so signing and
     * verification always use the same material and algorithm.
     */
    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(tokenProperties.getSecretKey().getBytes(StandardCharsets.UTF_8));
    }
}
