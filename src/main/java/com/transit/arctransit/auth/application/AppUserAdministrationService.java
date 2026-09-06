package com.transit.arctransit.auth.application;

// auth module
import com.transit.arctransit.auth.ChangeAccountStatusCommand;
import com.transit.arctransit.auth.CreateUserCommand;
import com.transit.arctransit.auth.ReplaceUserRolesCommand;
import com.transit.arctransit.auth.UserAdministrationService;
import com.transit.arctransit.auth.CurrentUserView;
import com.transit.arctransit.auth.UserSummaryView;
import com.transit.arctransit.auth.UserView;
import com.transit.arctransit.auth.UserQuery;

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


// java
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
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
    
    private final String bootstrapAdminUsername;

    public AppUserAdministrationService(AppUserRepository userRepository,
    									UserRoleRepository roleRepository,
    									PasswordEncoder passwordEncoder,
    									@Value("${arc.security.admin.username}") String bootstrapAdminUsername) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapAdminUsername = 
        						normalizeUsername(bootstrapAdminUsername);
    }

	// current user
    @Override
    @Transactional(readOnly = true)
    public CurrentUserView currentUser() {
    	
    	Authentication authentication = requireAuthenticatedUser();
    	
    	AppUser user = userRepository
				    			.findByUsername(authentication.getName())
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
    	
    	String normalizedUsername = normalizeUsername(command.username());
    	
	    	if (userRepository
			    		.findByUsername(normalizedUsername)
			    		.isPresent()) {
	    			
	    			throw new BusinessConflictException("Username already taken.");
	    	}
	    String normalizedEmail = normalizeOptionalEmail(command.email());
    	
	    Set<String> normalizedRoles = normalizeRoles(command.roles());
	    
	    AppUser newUser = new AppUser(
					    		normalizedUsername,
					    		passwordEncoder.encode(command.password()),
					    		command.displayName().trim(),
					    		normalizedEmail
	    );
	    
	    userRepository.saveAndFlush(newUser);
	    
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
	    log.info("Created user account '{}'.",
	    									normalizedUsername);
	    return	toUserView(newUser);
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
    	user.changeAccountStatus(newStatus);
    	
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
    	
    	roleRepository.deleteByUserId(user.getId());
    	
    	Long currentUserId = getCurrentUserId();
    	
    	for (String role : normalizedRoles) {
							    		roleRepository.save(
									    				new UserRole(
									    						user.getId(),
									    						role,
									    						currentUserId
									    				)
							    		);
    	}
    	log.info("Replace roles for user '{}'.",
    										normalizedUsername);
    	return toUserView(user);
    }
    
    // reset password
    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void resetPassword(String username, String newPassword) {

        String normalizedUsername = normalizeUsername(username);

        AppUser user = userRepository
				                .findByUsername(normalizedUsername)
				                .orElseThrow(() ->
					                        new ResourceNotFoundException("User not found: "
									                									+ normalizedUsername)
        );

        if (newPassword == null || newPassword.isBlank()) {
            throw new CommandValidationException(
            		"Invalid Password. Password cannot be blank."
            );
        }
        
        user.changePassword(
                passwordEncoder.encode(newPassword)
        );
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

            long totalAdmins = roleRepository.countByRoleCode(SYSTEM_ADMIN_ROLE);

            // if there is only one SYSTEM_ADMIN, it cannot be removed.
            if (totalAdmins <= 1) {
                throw new BusinessConflictException(
                        "Cannot delete the only remaining "
                        					+ "SYSTEM_ADMIN account."
                );
            }

            Authentication authentication = requireAuthenticatedUser();

            String currentUsername = normalizeUsername(authentication.getName());

            // only admins can delete other admin users
            if (!bootstrapAdminUsername.equals(currentUsername)) {
                throw new BusinessConflictException(
                        "Only the configured Default Admin is "
						                        	+ "authorized to delete other "
							                        			+ "administrator accounts."
                );
            }
        }
        roleRepository.deleteByUserId(user.getId());
        userRepository.delete(user);
        
        log.info("Deleted user account '{}'.",
        								normalizedUsername);
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
		                roles
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

        // ps. temporary implementation only
        return userRepository
		                .findAll(pageable)
		                .map(this::toUserSummaryView);
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
