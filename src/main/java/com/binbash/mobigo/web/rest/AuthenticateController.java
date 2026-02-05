package com.binbash.mobigo.web.rest;

import static com.binbash.mobigo.security.SecurityUtils.AUTHORITIES_CLAIM;
import static com.binbash.mobigo.security.SecurityUtils.JWT_ALGORITHM;
import static com.binbash.mobigo.security.SecurityUtils.USER_ID_CLAIM;

import com.binbash.mobigo.domain.InvalidatedToken;
import com.binbash.mobigo.repository.InvalidatedTokenRepository;
import com.binbash.mobigo.security.DomainUserDetailsService.UserWithId;
import com.binbash.mobigo.web.rest.vm.LoginVM;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * Controller to authenticate users.
 */
@RestController
@RequestMapping("/api")
public class AuthenticateController {

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticateController.class);

    private final JwtEncoder jwtEncoder;

    private final InvalidatedTokenRepository invalidatedTokenRepository;

    @Value("${jhipster.security.authentication.jwt.token-validity-in-seconds:0}")
    private long tokenValidityInSeconds;

    @Value("${jhipster.security.authentication.jwt.token-validity-in-seconds-for-remember-me:0}")
    private long tokenValidityInSecondsForRememberMe;

    private final AuthenticationManagerBuilder authenticationManagerBuilder;

    public AuthenticateController(
        JwtEncoder jwtEncoder,
        AuthenticationManagerBuilder authenticationManagerBuilder,
        InvalidatedTokenRepository invalidatedTokenRepository
    ) {
        this.jwtEncoder = jwtEncoder;
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.invalidatedTokenRepository = invalidatedTokenRepository;
    }

    @PostMapping("/authenticate")
    public ResponseEntity<JWTToken> authorize(@Valid @RequestBody LoginVM loginVM) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
            loginVM.getUsername(),
            loginVM.getPassword()
        );

        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = this.createToken(authentication, loginVM.isRememberMe());
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(jwt);
        return new ResponseEntity<>(new JWTToken(jwt), httpHeaders, HttpStatus.OK);
    }

    /**
     * {@code GET /authenticate} : check if the user is authenticated.
     *
     * @return the {@link ResponseEntity} with status {@code 204 (No Content)},
     * or with status {@code 401 (Unauthorized)} if not authenticated.
     */
    @GetMapping("/authenticate")
    public ResponseEntity<Void> isAuthenticated(Principal principal) {
        LOG.debug("REST request to check if the current user is authenticated");
        return ResponseEntity.status(principal == null ? HttpStatus.UNAUTHORIZED : HttpStatus.NO_CONTENT).build();
    }

    public String createToken(Authentication authentication, boolean rememberMe) {
        String authorities = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(" "));

        Instant now = Instant.now();
        Instant validity;
        if (rememberMe) {
            validity = now.plus(this.tokenValidityInSecondsForRememberMe, ChronoUnit.SECONDS);
        } else {
            validity = now.plus(this.tokenValidityInSeconds, ChronoUnit.SECONDS);
        }

        // @formatter:off
        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
            .issuedAt(now)
            .expiresAt(validity)
            .subject(authentication.getName())
            .claim(AUTHORITIES_CLAIM, authorities);
        if (authentication.getPrincipal() instanceof UserWithId user) {
            builder.claim(USER_ID_CLAIM, user.getId());
        }

        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, builder.build())).getTokenValue();
    }

    /**
     * {@code POST /logout} : Invalidate the current JWT token.
     *
     * Adds the token to a blacklist so it cannot be reused, preventing identity theft
     * even if the token is intercepted. Also cleans up expired blacklisted tokens.
     *
     * @param authentication the current authentication
     * @param request the HTTP request containing the Bearer token
     * @return the {@link ResponseEntity} with status {@code 200 (OK)}
     */
    @PostMapping("/logout")
    @Transactional
    public ResponseEntity<Void> logout(Authentication authentication, HttpServletRequest request) {
        LOG.debug("REST request to logout user: {}", authentication.getName());

        // Extract the raw token from the Authorization header
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String tokenHash = computeTokenHash(token);

            // Get token expiration from the JWT claims
            Instant expiresAt;
            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                expiresAt = jwtAuth.getToken().getExpiresAt();
            } else {
                // Fallback: use max remember-me validity
                expiresAt = Instant.now().plus(this.tokenValidityInSecondsForRememberMe, ChronoUnit.SECONDS);
            }

            // Store the token hash in the blacklist
            if (!invalidatedTokenRepository.existsByTokenHash(tokenHash)) {
                InvalidatedToken invalidatedToken = new InvalidatedToken();
                invalidatedToken.setTokenHash(tokenHash);
                invalidatedToken.setExpiresAt(expiresAt);
                invalidatedToken.setInvalidatedAt(Instant.now());
                invalidatedTokenRepository.save(invalidatedToken);
            }

            // Cleanup expired blacklisted tokens
            invalidatedTokenRepository.deleteExpiredTokens(Instant.now());
        }

        SecurityContextHolder.clearContext();
        return ResponseEntity.ok().build();
    }

    /**
     * Compute SHA-256 hash of a token for secure storage.
     */
    public static String computeTokenHash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Object to return as body in JWT Authentication.
     */
    static class JWTToken {

        private String idToken;

        JWTToken(String idToken) {
            this.idToken = idToken;
        }

        @JsonProperty("id_token")
        String getIdToken() {
            return idToken;
        }

        void setIdToken(String idToken) {
            this.idToken = idToken;
        }
    }
}
