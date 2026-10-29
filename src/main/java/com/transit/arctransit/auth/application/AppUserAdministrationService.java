package com.transit.arctransit.auth.application;

// auth module
import com.transit.arctransit.auth.ChangeAccountStatusCommand;
import com.transit.arctransit.auth.CreateUserCommand;
import com.transit.arctransit.auth.UpdateUserCommand;
import com.transit.arctransit.auth.PasswordPolicy;
import com.transit.arctransit.auth.ReplaceUserRolesCommand;
import com.transit.arctransit.auth.UserAdministrationService;
import com.transit.arctransit.auth.CurrentUserView;
import com.transit.arctransit.auth.UserSummaryView;
import com.transit.arctransit.auth.UserView;
import com.transit.arctransit.auth.UserQuery;
import com.transit.arctransit.audit.AuditRecordingService;

// auth domain
import com.transit.arctransit.auth.domain.AccountStatus;
import com.transit.arctransit.auth.domain.AppUser;
import com.transit.arctransit.auth.domain.AppUserRepository;
import com.transit.arctransit.auth.domain.Role;
import com.transit.arctransit.auth.domain.RoleRepository;
import com.transit.arctransit.auth.domain.UserRole;
import com.transit.arctransit.auth.domain.UserRoleRepository;
import com.transit.arctransit.auth.domain.UserRoleId;

// auth exceptions
import com.transit.arctransit.common.exception.BusinessConflictException;
import com.transit.arctransit.common.exception.CommandValidationException;
import com.transit.arctransit.common.exception.ResourceNotFoundException;

// springframework data, security, stereotype transaction
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;


// java
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@Validated
public class AppUserAdministrationService implements UserAdministrationService {

	// constants
	private static final Logger log =
					LoggerFactory.getLogger(AppUserAdministrationService.class);
	
	private static final String SYSTEM_ADMIN_ROLE = "SYSTEM_ADMIN";
	private static final String OPERATIONS_STAFF_ROLE = "OPERATIONS_STAFF";
	
	private static final Set<String> SUPPORTED_ROLES = Set.of(
			SYSTEM_ADMIN_ROLE,
			OPERATIONS_STAFF_ROLE
	);
	
	// dependencies
    private final AppUserRepository userRepository;
    private final UserRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditRecordingService auditService;
    
    private final String bootstrapAdminUsername;

    public AppUserAdministrationService(AppUserRepository userRepository,
    									UserRoleRepository roleRepository,
    									PasswordEncoder passwordEncoder,
                                        @Value("${arc.security.admin.username}") String bootstrapAdminUsername,
                                        AuditRecordingService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.bootstrapAdminUsername = 
        						normalizeUsername(bootstrapAdminUsername);
    }

	// current user
    @Override
    @Transactional(readOnly = true)
    public CurrentUserView currentUser() {
    	
    	Authentication authentication = requireAuthenticatedUser();
    	
    	AppUser user = userRepository
                .findByUsername(normalizeUsername(authentication.getName()))
                                .or(() -> userRepository.findByReservedUsername(normalizeUsername(authentication.getName())))
				    			.orElseThrow(() -> 
				    					new ResourceNotFoundException(
				    											"Current user not found."
				    					)
    	);
    	
    	Set<String> roles = user.getUserRoles()
							    			.stream()
							    			.map(UserRole::getRoleCode)
							    			.collect(Collectors.toUnmodifiableSet());
    
    return new CurrentUserView(
				    		user.getUsername(),
				    		roles
    );
    }
    
    
    // create user
    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UserView createUser(CreateUserCommand command) {
        validatePassword(command.password());
    	
    	String normalizedUsername = normalizeUsername(command.username());
    	
	    	if (userRepository
			    		.findByUsername(normalizedUsername)
                .isPresent() || userRepository.isUsernameReserved(normalizedUsername)) {
	    			
	    			throw new BusinessConflictException("Username already taken.");
	    	}
	    String normalizedEmail = normalizeOptionalEmail(command.email());
        if (normalizedEmail != null && userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new BusinessConflictException("Email address is already in use by another account.");
        }
    	
	    Set<String> normalizedRoles = normalizeRoles(command.roles());
	    
	    AppUser newUser = new AppUser(
					    		normalizedUsername,
					    		passwordEncoder.encode(command.password()),
					    		command.displayName().trim(),
					    		normalizedEmail
	    );
	    
        try {
            userRepository.saveAndFlush(newUser);
        } catch (org.springframework.dao.DataIntegrityViolationException exception) {
            throw accountWriteFailure(exception);
        }
	    
	    Long currentUserId = getCurrentUserId();
	    
	    for (String role : normalizedRoles) {
								    	roleRepository.save(
										    			new UserRole(
										    					newUser.getId(),
										    					role,
										    					currentUserId
										    			)
								    	);
	    }
        auditService.recordAction("USER_CREATED", "AppUser", newUser.getId(),
                "Created account " + normalizedUsername + ".");
	    log.info("Created user account '{}'.",
	    									normalizedUsername);
	    return	toUserView(newUser);
    }
    
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UserView getUser(String username) {
        return toUserView(userRepository.findByUsername(normalizeUsername(username))
                .orElseThrow(() -> new ResourceNotFoundException("User not found.")));
    }

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UserView updateUser(UpdateUserCommand command) {
        AppUser user = userRepository.findById(command.id())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        if (user.isArchived()) {
            throw new BusinessConflictException("Archived accounts cannot be edited.");
        }
        if (!java.util.Objects.equals(user.getVersion(), command.version())) {
            throw new BusinessConflictException("This account was changed by another user. Reopen the editor and try again.");
        }
        String username = normalizeUsername(command.username());
        if (userRepository.isUsernameReservedByOtherUser(username, user.getId())) {
            throw new BusinessConflictException("Username is already taken or reserved by another account.");
        }
        String email = normalizeOptionalEmail(command.email());
        if (email != null && userRepository.existsByEmailIgnoreCaseAndIdNot(email, user.getId())) {
            throw new BusinessConflictException("Email address is already in use by another account.");
        }
        Set<String> roles = normalizeRoles(command.roles());
        Set<String> previousRoles = roleRepository.findByUserId(user.getId()).stream()
                .map(UserRole::getRoleCode).collect(Collectors.toUnmodifiableSet());
        if (!roles.contains(SYSTEM_ADMIN_ROLE)) {
            protectLastActiveAdministrator(user);
        }
        Long actorId = getCurrentUserId();
        String previousUsername = user.getUsername();
        user.updateProfile(username, command.displayName().trim(), email);
        try {
            userRepository.saveAndFlush(user);
        } catch (org.springframework.dao.DataIntegrityViolationException exception) {
            throw accountWriteFailure(exception);
        }
        if (!roles.equals(previousRoles)) {
            replaceRoleAssignments(user, roles, actorId);
            auditService.recordAction("USER_ROLES_CHANGED", "AppUser", user.getId(),
                    "Roles changed from " + previousRoles + " to " + roles + ".");
        }
        auditService.recordAction("USER_UPDATED", "AppUser", user.getId(),
                "Account details updated; username " + previousUsername + " -> " + username
                        + "; display name and optional email saved.");
        return toUserView(user);
    }

    // change account status
    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UserView changeAccountStatus(ChangeAccountStatusCommand command) {
    	
    	String normalizedUsername = normalizeUsername(command.username());
    	
    	AppUser user = userRepository
				    			.findByUsername(normalizedUsername)
				    			.orElseThrow(() ->
				    					new ResourceNotFoundException("User not found."
				    															+ normalizedUsername)
    	);
    	
    	AccountStatus newStatus = parseAccountStatus(command.newStatus());
        if (newStatus != AccountStatus.ACTIVE) {
            protectLastActiveAdministrator(user);
        }
    	user.changeAccountStatus(newStatus);
        auditService.recordAction("USER_STATUS_CHANGED", "AppUser", user.getId(),
                "Account status changed to " + newStatus + ".");
    	
    	log.info("Changed account status for '{}' to '{}'.", 
    												normalizedUsername,
    												newStatus);
    	return toUserView(user);
    }
    
    // replace user roles
    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UserView replaceRoles(ReplaceUserRolesCommand command) {
    	
    	String normalizedUsername = normalizeUsername(command.username());
    	
    	AppUser user = userRepository
				    			.findByUsername(normalizedUsername)
				    			.orElseThrow(() ->
				    				new ResourceNotFoundException("User not found:"
				    														+ normalizedUsername)
    	);
    	
    	Set<String> normalizedRoles = normalizeRoles(command.roles());
        if (!normalizedRoles.contains(SYSTEM_ADMIN_ROLE)) {
            protectLastActiveAdministrator(user);
        }
    	
        Long currentUserId = getCurrentUserId();
        user.updateProfile(user.getUsername(), user.getDisplayName(), user.getEmail());
        userRepository.saveAndFlush(user);
        replaceRoleAssignments(user, normalizedRoles, currentUserId);
        auditService.recordAction("USER_ROLES_CHANGED", "AppUser", user.getId(),
                "Roles changed to " + normalizedRoles + ".");
    	log.info("Replace roles for user '{}'.",
    										normalizedUsername);
    	return toUserView(user);
    }
    
    // reset password
    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void resetPassword(String username, String newPassword) {

        String normalizedUsername = normalizeUsername(username);
        validatePassword(newPassword);

        AppUser user = userRepository
				                .findByUsername(normalizedUsername)
				                .orElseThrow(() ->
					                        new ResourceNotFoundException("User not found: "
									                									+ normalizedUsername)
        );


        
        user.changePassword(
                passwordEncoder.encode(newPassword)
        );
        auditService.recordAction("USER_PASSWORD_RESET", "AppUser", user.getId(),
                "Password reset.");
        log.info("Password reset for user '{}'.", 
        									normalizedUsername
        );
    }
    
    // delete OR archive user
    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void deleteUser(String username) {

        String normalizedUsername = normalizeUsername(username);

        AppUser user = userRepository
				                .findByUsername(normalizedUsername)
				                .orElseThrow(() ->
				                			new ResourceNotFoundException(
				                								"User not found: " 
				                											+ normalizedUsername)
        );

        boolean isTargetAdmin = roleRepository.findByUserId(user.getId())
			                	.stream()
			                    .anyMatch(role ->
			                    	SYSTEM_ADMIN_ROLE.equals(role.getRoleCode())
		);

        if (isTargetAdmin) {

            protectLastActiveAdministrator(user);

            Authentication authentication = requireAuthenticatedUser();

            String currentUsername = normalizeUsername(authentication.getName());

            // only admins can delete other admin users
            if (!bootstrapAdminUsername.equals(currentUsername)
                    && !userRepository.findByReservedUsername(bootstrapAdminUsername)
                            .map(defaultAdmin -> defaultAdmin.getId().equals(getCurrentUserId())).orElse(false)) {
                throw new BusinessConflictException(
                        "Only the configured Default Admin is "
						                        	+ "authorized to delete other "
							                        			+ "administrator accounts."
                );
            }
        }
        user.archive();
        auditService.recordAction("USER_ARCHIVED", "AppUser", user.getId(),
                "Account archived and disabled.");
        
        log.info("Archived user account '{}'.",
        								normalizedUsername);
    }
    
    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void hardDeleteUser(String username) {
        AppUser user = userRepository.findLockedByUsername(normalizeUsername(username))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        if (user.getArchivedAt() == null) {
            throw new BusinessConflictException("Archive the account before permanently deleting it.");
        }
        if (user.getId().equals(getCurrentUserId())
                || userRepository.findByReservedUsername(bootstrapAdminUsername)
                        .map(bootstrap -> bootstrap.getId().equals(user.getId())).orElse(false)) {
            throw new BusinessConflictException("The current account and configured Default Admin cannot be permanently deleted.");
        }
        if (roleRepository.existsByUserIdAndRoleCode(user.getId(), SYSTEM_ADMIN_ROLE)
                && !userRepository.findByReservedUsername(bootstrapAdminUsername)
                        .map(bootstrap -> bootstrap.getId().equals(getCurrentUserId())).orElse(false)) {
            throw new BusinessConflictException("Only the configured Default Admin can permanently delete administrator accounts.");
        }
        if (roleRepository.existsByAssignedByUserIdAndUserIdNot(user.getId(), user.getId())) {
            throw new BusinessConflictException("This account granted roles to other accounts. Keep it archived to preserve those references.");
        }
        user.getUserRoles().clear();
        roleRepository.deleteByUserId(user.getId());
        roleRepository.flush();
        userRepository.delete(user);
        userRepository.flush();
        auditService.recordAction("USER_DELETED", "AppUser", user.getId(),
                "Permanently deleted archived account " + user.getUsername() + " (" + user.getDisplayName()
                        + "). Historical actor names and username reservations are retained under user ID " + user.getId() + ".");
    }

    // view mapping
    private UserView toUserView(AppUser user) {

        Set<String> roles = roleRepository.findByUserId(user.getId())
					                        .stream()
					                        .map(UserRole::getRoleCode)
					                        .collect(Collectors.toUnmodifiableSet()
        );

        return new UserView(
		                user.getUsername(),
		                user.getDisplayName(),
		                user.getEmail(),
		                user.getAccountStatus().name(),
		                roles,
                        user.getId(),
                        user.getVersion()
        );
    }

    private UserSummaryView toUserSummaryView(AppUser user) {

        return new UserSummaryView(
			                user.getUsername(),
			                user.getDisplayName(),
			                user.getAccountStatus().name()
        );
    }
    
    // page
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Page<UserSummaryView> searchUsers(
						            UserQuery query,
						            Pageable pageable) {

        String keyword = query == null || query.searchKeyword() == null
                ? "" : query.searchKeyword().trim();
        return userRepository
                .searchUnarchivedUsers(keyword, pageable)
		                .map(this::toUserSummaryView);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Page<UserSummaryView> searchArchivedUsers(UserQuery query, Pageable pageable) {
        String keyword = query == null || query.searchKeyword() == null
                ? "" : query.searchKeyword().trim();
        return userRepository.searchArchivedUsers(keyword, pageable).map(this::toUserSummaryView);
    }

    private void protectLastActiveAdministrator(AppUser user) {
        if (user.isActive()
                && roleRepository.existsByUserIdAndRoleCode(user.getId(), SYSTEM_ADMIN_ROLE)
                && userRepository.findActiveAdministratorsForUpdate().size() <= 1) {
            throw new BusinessConflictException(
                    "Cannot remove or disable the only active SYSTEM_ADMIN account.");
        }
    }

    private void replaceRoleAssignments(AppUser user, Set<String> roles, Long actorId) {
        // The eager read-only role collection must not retain entities deleted by the role repository.
        user.getUserRoles().clear();
        roleRepository.deleteByUserId(user.getId());
        roleRepository.flush();
        for (String role : roles) {
            UserRole assignment = roleRepository.save(new UserRole(user.getId(), role, actorId));
            user.getUserRoles().add(assignment);
        }
    }

    private static RuntimeException accountWriteFailure(org.springframework.dao.DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException constraint) {
                if ("ux_app_users_email_ci".equals(constraint.getConstraintName())) {
                    return new BusinessConflictException("Email address is already in use by another account.");
                }
                if ("ux_app_users_username".equals(constraint.getConstraintName())
                        || "username_reservations_pkey".equals(constraint.getConstraintName())) {
                    return new BusinessConflictException("Username is already taken or reserved by another account.");
                }
            }
            cause = cause.getCause();
        }
        return exception;
    }

    private static void validatePassword(String password) {
        if (!PasswordPolicy.isValid(password)) {
            throw new CommandValidationException(PasswordPolicy.validationMessage(password));
        }
    }
    
    // helper methods
    private Authentication requireAuthenticatedUser() {

        Authentication authentication = SecurityContextHolder
								                        .getContext()
								                        .getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
        						   || authentication instanceof AnonymousAuthenticationToken) {

            throw new ResourceNotFoundException("No authenticated user session.");
        }
        return authentication;
    }

    private Long getCurrentUserId() {

        Authentication authentication = requireAuthenticatedUser();

        return userRepository
                .findByUsername(
                        normalizeUsername(authentication.getName())
                )
                .or(() -> userRepository.findByReservedUsername(normalizeUsername(authentication.getName())))
                .map(AppUser::getId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                        		"Authenticated user not found in database."
                        )
        );
    }
    
    // normalization and validation helpers
    private static String normalizeUsername(String username) {

        if (username == null || username.isBlank()) {
            throw new CommandValidationException("Username cannot be blank.");
        }
        return username
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private static String normalizeOptionalEmail(String email) {

        if (email == null) {
            return null;
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        return normalizedEmail.isBlank()
					                ? null
					                : normalizedEmail;
    }

    private static Set<String> normalizeRoles(Set<String> roles) {

        if (roles == null || roles.isEmpty()) {
            throw new CommandValidationException("User must have at least one role.");
        }

        Set<String> normalizedRoles = roles.stream()
						                   .map(AppUserAdministrationService::normalizeRole)
						                   .collect(Collectors.toUnmodifiableSet()
        );
        return normalizedRoles;
    }

    private static String normalizeRole(String role) {

        if (role == null || role.isBlank()) {
            throw new CommandValidationException(" User roles cannot be blank.");
        }

        String normalizedRole = role.trim()
                					.toUpperCase(Locale.ROOT);

        if (!SUPPORTED_ROLES.contains(normalizedRole)) {
            throw new CommandValidationException("Unsupported role: " + role);
        }
        return normalizedRole;
    }

    private static AccountStatus parseAccountStatus(
            String status) {

        if (status == null || status.isBlank()) {
            throw new CommandValidationException("Account status cannot be blank.");
        }

        try {
            return AccountStatus.valueOf(
                    status.trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new CommandValidationException("Invalid account status: " + status);
        }
    }
}
