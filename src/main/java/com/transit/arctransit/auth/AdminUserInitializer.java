package com.transit.arctransit.auth;

// import appuser domain repository
import com.transit.arctransit.auth.domain.AppUser;
import com.transit.arctransit.auth.domain.AppUserRepository;

// import jdbc and logger
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// import springframework beans
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// initializes the default system administrator if none exist
@Component
public class AdminUserInitializer implements CommandLineRunner {
	
	private static final Logger log = 
			LoggerFactory.getLogger(AdminUserInitializer.class);
	
	private static final String SYSTEM_ADMIN_ROLE = "SYSTEM_ADMIN";

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    
    private final String adminUsername;
    private final String adminPassword;

    public AdminUserInitializer(
    						AppUserRepository userRepository,
    						PasswordEncoder passwordEncoder,
				    		JdbcTemplate jdbcTemplate,
				    		@Value("${arc.security.admin.username}") String adminUsername,
				    		@Value("${arc.security.admin.password}") String adminPassword) {
    	this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jdbcTemplate = jdbcTemplate;
		this.adminUsername = adminUsername;
		this.adminPassword = adminPassword;
    		}

    @Override
    @Transactional
    public void run(String... args) {
    	
    	// Add a admin user after the application has started
        userRepository.findByUsername(adminUsername)
        										.ifPresentOrElse(
										        			this::ensureSystemAdminRole,
										        			this::createAdmin
        										);
    }
    
    private void createAdmin() {
    	AppUser admin = new AppUser(
				    			adminUsername,
				    			passwordEncoder.encode(adminPassword),
				    			"Default Admin",
				    			null
    	);
    	admin = userRepository.save(admin);
    	assignSystemAdminRole(admin.getId());
    }

	private void ensureSystemAdminRole(AppUser admin) {
		if(!hasSystemAdminRole(admin.getId())) {
			assignSystemAdminRole(admin.getId());
			
			log.info(
					"Added SYSTEM_ADMIN role to existing user '{}'.",
																adminUsername
			);
			
		}
	}

	private boolean hasSystemAdminRole(Long userId) {
		
		Boolean exists = jdbcTemplate.queryForObject(
													"""
													SELECT EXISTS (
														SELECT 1
														FROM arc.user_roles
														WHERE user_id = ?
														AND role_code = ?
													)
													""",
													Boolean.class,
													userId,
													SYSTEM_ADMIN_ROLE
		);
		return Boolean.TRUE.equals(exists);
	}
	
	private void assignSystemAdminRole(Long userId) {
		jdbcTemplate.update(
						"""
						INSERT INTO arc.user_roles
		                    (user_id, role_code, assigned_at)
		                VALUES
		                    (?, ?, CURRENT_TIMESTAMP)
		                ON CONFLICT
							(user_id, role_code)
		                DO NOTHING
						""",
						userId,
						SYSTEM_ADMIN_ROLE
		);
	}
}
