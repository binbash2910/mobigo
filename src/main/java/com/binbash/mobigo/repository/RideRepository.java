package com.binbash.mobigo.repository;

import com.binbash.mobigo.domain.Ride;
import com.binbash.mobigo.domain.enumeration.RideStatusEnum;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Ride entity.
 */
@SuppressWarnings("unused")
@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {
    @Query("SELECT r FROM Ride r LEFT JOIN FETCH r.vehicule v LEFT JOIN FETCH v.proprietaire")
    List<Ride> findAllWithVehiculeAndProprietaire();

    @Query("SELECT r FROM Ride r LEFT JOIN FETCH r.vehicule v LEFT JOIN FETCH v.proprietaire WHERE r.id = :id")
    Optional<Ride> findByIdWithVehiculeAndProprietaire(Long id);

    @Query("SELECT COUNT(r) FROM Ride r WHERE r.vehicule.id = :vehicleId AND r.statut IN :statuts")
    long countByVehiculeIdAndStatutIn(@Param("vehicleId") Long vehicleId, @Param("statuts") List<RideStatusEnum> statuts);
}
