package com.transit.arctransit.auth.application;

import com.transit.arctransit.auth.*;
import com.transit.arctransit.auth.domain.*;
import com.transit.arctransit.common.exception.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@Transactional
public class AppUserAdministrationService implements UserAdministrationService {

    private final AppUserRepository userRepository;
    private final UserRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AppUserAdministrationService(AppUserRepository userRepository, UserRoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public CurrentUserView currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResourceNotFoundException("No authenticated user session.");
        }
        AppUser user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found in database."));
        return new CurrentUserView(
                user.getUsername(),
                user.getUserRoles().stream().map(UserRole::getRoleCode).collect(Collectors.toSet())
        );
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Page<UserSummaryView> searchUsers(UserQuery query, Pageable pageable) {
        return userRepository.findAll(pageable).map(user -> new UserSummaryView(
                user.getUsername(),
                user.getDisplayName(),
                user.getAccountStatus().name()
        ));
    }

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UserView createUser(CreateUserCommand command) {
        if (userRepository.findByUsername(command.username()).isPresent()) {
            throw new BusinessConflictException("Username is already taken.");
        }

        AppUser newUser = new AppUser(
                command.username(),
                passwordEncoder.encode(command.password()),
                command.displayName(),
                command.email()
        );
        // Save first to get the generated ID
        userRepository.saveAndFlush(newUser);

        Long currentUserId = getCurrentUserIdOrSystem();

        for (String role : command.roles()) {
            roleRepository.save(new UserRole(newUser.getId(), role, currentUserId));
        }

        return toUserView(newUser);
    }

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UserView changeAccountStatus(ChangeAccountStatusCommand command) {
        AppUser user = userRepository.findByUsername(command.username())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + command.username()));

        try {
            AccountStatus newStatus = AccountStatus.valueOf(command.newStatus());
            user.changeAccountStatus(newStatus);
        } catch (IllegalArgumentException e) {
            throw new CommandValidationException("Invalid account status: " + command.newStatus());
        }

        return toUserView(user);
    }

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UserView replaceRoles(ReplaceUserRolesCommand command) {
        AppUser user = userRepository.findByUsername(command.username())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + command.username()));

        roleRepository.deleteByUserId(user.getId());

        Long currentUserId = getCurrentUserIdOrSystem();
        for (String role : command.roles()) {
            roleRepository.save(new UserRole(user.getId(), role, currentUserId));
        }

        return toUserView(user);
    }

    private UserView toUserView(AppUser user) {
        var roles = roleRepository.findByUserId(user.getId()).stream()
                .map(UserRole::getRoleCode)
                .collect(Collectors.toSet());

        return new UserView(
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getAccountStatus().name(),
                roles
        );
    }

    private Long getCurrentUserIdOrSystem() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null; // System generated
        }
        return userRepository.findByUsername(auth.getName())
                .map(AppUser::getId)
                .orElse(null);
    }
}
