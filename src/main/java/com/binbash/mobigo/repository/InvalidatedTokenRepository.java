package com.binbash.mobigo.repository;

import com.binbash.mobigo.domain.InvalidatedToken;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the InvalidatedToken entity.
 */
@Repository
public interface InvalidatedTokenRepository extends JpaRepository<InvalidatedToken, Long> {
    /**
     * Check if a token hash exists in the blacklist.
     */
    boolean existsByTokenHash(String tokenHash);

    /**
     * Remove expired tokens from the blacklist (cleanup).
     */
    @Modifying
    @Query("DELETE FROM InvalidatedToken t WHERE t.expiresAt < ?1")
    void deleteExpiredTokens(Instant now);
}
