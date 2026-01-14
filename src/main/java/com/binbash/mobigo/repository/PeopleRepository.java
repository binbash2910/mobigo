package com.binbash.mobigo.repository;

import com.binbash.mobigo.domain.People;
import com.binbash.mobigo.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the People entity.
 */
@SuppressWarnings("unused")
@Repository
public interface PeopleRepository extends JpaRepository<People, Long> {
    Optional<People> findByUser(User user);
}
