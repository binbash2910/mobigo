package com.binbash.mobigo.helper;

import com.binbash.mobigo.domain.User;
import com.binbash.mobigo.repository.UserRepository;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class UserHelper {

    private final UserRepository userRepository;

    public UserHelper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                return ((UserDetails) principal).getUsername();
            } else {
                return principal.toString(); // In case of anonymous or token with raw string
            }
        }
        return null;
    }

    public Optional<User> getCurrentUser() {
        String userName = this.getCurrentUsername();
        if (!StringUtils.isEmpty(userName)) {
            return userRepository.findOneByLogin(userName);
        }
        return Optional.empty();
    }
}
