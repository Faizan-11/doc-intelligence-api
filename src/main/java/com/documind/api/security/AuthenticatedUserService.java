
package com.documind.api.security;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Service;

import com.documind.api.entity.User;
import com.documind.api.repository.UserRepository;

@Service
public class AuthenticatedUserService {

    private final UserRepository userRepository;

    public AuthenticatedUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                        instanceof UserDetails userDetails)) {

            throw new AuthenticationCredentialsNotFoundException(
                    "No authenticated user found."
            );
        }

        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() ->
                        new AuthenticationCredentialsNotFoundException(
                                "Authenticated user no longer exists."
                        )
                );
    }

    public UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
