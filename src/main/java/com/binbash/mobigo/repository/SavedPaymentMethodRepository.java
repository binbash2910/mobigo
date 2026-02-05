package com.binbash.mobigo.repository;

import com.binbash.mobigo.domain.SavedPaymentMethod;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the SavedPaymentMethod entity.
 */
@SuppressWarnings("unused")
@Repository
public interface SavedPaymentMethodRepository extends JpaRepository<SavedPaymentMethod, Long> {
    List<SavedPaymentMethod> findByProprietaireId(Long proprietaireId);

    List<SavedPaymentMethod> findByProprietaireUserLogin(String login);
}
