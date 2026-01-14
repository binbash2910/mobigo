package com.binbash.mobigo.helper;

import com.binbash.mobigo.domain.People;
import com.binbash.mobigo.domain.User;
import com.binbash.mobigo.repository.PeopleRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class PeopleHelper {

    private final PeopleRepository peopleRepository;
    private final UserHelper userHelper;

    public PeopleHelper(PeopleRepository peopleRepository, UserHelper userHelper) {
        this.peopleRepository = peopleRepository;
        this.userHelper = userHelper;
    }

    public Optional<People> getCurrentPeople() {
        Optional<User> userOpt = userHelper.getCurrentUser();
        if (userOpt.isPresent()) {
            return peopleRepository.findByUser(userOpt.get());
        }
        return Optional.empty();
    }
}
