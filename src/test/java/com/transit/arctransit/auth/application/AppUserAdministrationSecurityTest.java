package com.transit.arctransit.auth.application;

import com.transit.arctransit.audit.*;
import com.transit.arctransit.audit.application.AppAuditRecordingService;
import com.transit.arctransit.audit.domain.AuditLog;
import com.transit.arctransit.audit.domain.AuditLogRepository;
import com.transit.arctransit.auth.*;
import com.transit.arctransit.auth.domain.*;
import com.transit.arctransit.auth.security.AuthenticationEventListener;
import com.transit.arctransit.common.exception.BusinessConflictException;
import com.transit.arctransit.common.exception.CommandValidationException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationEventPublisher;
import org.springframework.security.authorization.SpringAuthorizationEventPublisher;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AppUserAdministrationSecurityTest {
    private AnnotationConfigApplicationContext context;
    private UserAdministrationService service;
    private AppUserRepository users;
    private UserRoleRepository roles;
    private AuditLogRepository audit;
    private PasswordEncoder passwords;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(TestConfiguration.class);
        service = context.getBean(UserAdministrationService.class);
        users = context.getBean(AppUserRepository.class);
        roles = context.getBean(UserRoleRepository.class);
        audit = context.getBean(AuditLogRepository.class);
        passwords = context.getBean(PasswordEncoder.class);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test
    void staffCannotInvokeAdministrationOrAuditQueriesDirectly() {
        authenticate("staff", "OPERATIONS_STAFF");
        assertThrows(AccessDeniedException.class, () -> service.createUser(command()));
        assertThrows(AccessDeniedException.class, () -> service.resetPassword("staff", "ArcTransit1!"));
        assertThrows(AccessDeniedException.class, () -> service.changeAccountStatus(
                new ChangeAccountStatusCommand("staff", "DISABLED")));
        assertThrows(AccessDeniedException.class, () -> service.replaceRoles(
                new ReplaceUserRolesCommand("staff", Set.of("SYSTEM_ADMIN"))));
        assertThrows(AccessDeniedException.class, () -> service.getUser("admin"));
        assertThrows(AccessDeniedException.class, () -> service.updateUser(new com.transit.arctransit.auth.UpdateUserCommand(
                2L, 0L, "staff", "Staff", null, Set.of("OPERATIONS_STAFF"))));
        assertThrows(AccessDeniedException.class, () -> service.deleteUser("admin"));
        assertThrows(AccessDeniedException.class, () -> service.searchUsers(null, PageRequest.of(0, 10)));
        assertThrows(AccessDeniedException.class, () -> service.searchArchivedUsers(null, PageRequest.of(0, 10)));
        assertThrows(AccessDeniedException.class, () -> context.getBean(AuditQueryService.class)
                .searchAudit(null, PageRequest.of(0, 10)));
        verifyNoInteractions(users, roles, passwords);
        ArgumentCaptor<AuditLog> logs = ArgumentCaptor.forClass(AuditLog.class);
        verify(audit, times(10)).save(logs.capture());
        assertTrue(logs.getAllValues().stream().allMatch(log -> "DENIED".equals(log.getOutcome())
                && "staff".equals(log.getPerformedBy())));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ArcTransit1!", "Transit899!@#"})
    void administratorCreatesAccountThroughProxyWithoutAuditingPassword(String password) {
        authenticate("admin", "SYSTEM_ADMIN");
        when(users.findByUsername("admin")).thenReturn(Optional.of(user(1L, "admin")));
        when(users.saveAndFlush(any())).thenAnswer(invocation -> {
            AppUser saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 2L);
            return saved;
        });
        when(passwords.encode(password)).thenReturn("private-password-hash");
        when(roles.findByUserId(2L)).thenReturn(List.of(new UserRole(2L, "OPERATIONS_STAFF", 1L)));
        UserView created = service.createUser(new CreateUserCommand("new.staff", password, "New Staff", null,
                Set.of("OPERATIONS_STAFF")));
        assertEquals("new.staff", created.username());
        assertEquals(Set.of("OPERATIONS_STAFF"), created.roles());
        verify(passwords).encode(password);
        ArgumentCaptor<AuditLog> log = ArgumentCaptor.forClass(AuditLog.class);
        verify(audit).save(log.capture());
        assertEquals("USER_CREATED", log.getValue().getActionName());
        assertEquals("admin", log.getValue().getPerformedBy());
        assertFalse(log.getValue().getDetails().contains(password));
        assertFalse(log.getValue().getDetails().contains("private-password-hash"));
    }

    @Test
    void administratorCanResetPasswordWithHashSymbol() {
        authenticate("admin", "SYSTEM_ADMIN");
        AppUser staff = user(2L, "staff");
        when(users.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(passwords.encode("Cedar9#Sea4!Moon")).thenReturn("new-private-hash");
        service.resetPassword("staff", "Cedar9#Sea4!Moon");
        assertEquals("new-private-hash", staff.getPasswordHash());
        verify(passwords).encode("Cedar9#Sea4!Moon");
    }

    @Test
    void proxyRejectsLowercaseOnlyPasswordBeforeCreatingAccount() {
        authenticate("admin", "SYSTEM_ADMIN");
        ConstraintViolationException failure = assertThrows(ConstraintViolationException.class,
                () -> service.createUser(new CreateUserCommand("test.staff", "usertest123",
                        "Test Staff", null, Set.of("OPERATIONS_STAFF"))));
        assertTrue(failure.getConstraintViolations().stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().endsWith("password")));
        verifyNoInteractions(users, roles, passwords, audit);
    }

    @Test
    void proxyRejectsMissingDisplayNameAndWeakResetPassword() {
        authenticate("admin", "SYSTEM_ADMIN");
        assertThrows(ConstraintViolationException.class, () -> service.createUser(
                new CreateUserCommand("new.staff", "ArcTransit1!", " ", null, Set.of("OPERATIONS_STAFF"))));
        when(users.findByUsername("staff")).thenReturn(Optional.of(user(2L, "staff")));
        assertThrows(CommandValidationException.class, () -> service.resetPassword("staff", "weak"));
        verifyNoInteractions(passwords, audit);
    }

    @Test
    void accountEditorCannotDemoteTheOnlyActiveAdministrator() {
        authenticate("admin", "SYSTEM_ADMIN");
        AppUser admin = user(1L, "admin");
        ReflectionTestUtils.setField(admin, "version", 0L);
        when(users.findById(1L)).thenReturn(Optional.of(admin));
        when(roles.findByUserId(1L)).thenReturn(List.of(new UserRole(1L, "SYSTEM_ADMIN", 1L)));
        when(roles.existsByUserIdAndRoleCode(1L, "SYSTEM_ADMIN")).thenReturn(true);
        when(users.findActiveAdministratorsForUpdate()).thenReturn(List.of(admin));
        assertThrows(BusinessConflictException.class, () -> service.updateUser(new com.transit.arctransit.auth.UpdateUserCommand(
                1L, 0L, "renamed", "Changed Name", null, Set.of("OPERATIONS_STAFF"))));
        assertEquals("admin", admin.getUsername());
        verify(users, never()).saveAndFlush(any());
        verify(roles, never()).deleteByUserId(any());
    }

    @Test
    void lastActiveAdministratorCannotBeDisabledOrDemoted() {
        authenticate("admin", "SYSTEM_ADMIN");
        AppUser admin = user(1L, "admin");
        when(users.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(roles.existsByUserIdAndRoleCode(1L, "SYSTEM_ADMIN")).thenReturn(true);
        when(users.findActiveAdministratorsForUpdate()).thenReturn(List.of(admin));
        assertThrows(BusinessConflictException.class, () -> service.changeAccountStatus(
                new ChangeAccountStatusCommand("admin", "DISABLED")));
        assertThrows(BusinessConflictException.class, () -> service.replaceRoles(
                new ReplaceUserRolesCommand("admin", Set.of("OPERATIONS_STAFF"))));
        assertTrue(admin.isActive());
        verify(roles, never()).deleteByUserId(anyLong());
        verifyNoInteractions(audit);
    }

    @Test
    void removingStaffArchivesIdentityAndRetainsRoleProvenance() {
        authenticate("admin", "SYSTEM_ADMIN");
        AppUser staff = user(2L, "staff");
        when(users.findByUsername("staff")).thenReturn(Optional.of(staff));
        service.deleteUser("staff");
        assertNotNull(staff.getArchivedAt());
        assertFalse(staff.isActive());
        verify(users, never()).delete(any());
        verify(roles, never()).deleteByUserId(anyLong());
        ArgumentCaptor<AuditLog> log = ArgumentCaptor.forClass(AuditLog.class);
        verify(audit).save(log.capture());
        assertEquals("USER_ARCHIVED", log.getValue().getActionName());
    }

    @Test
    void userSearchUsesSuppliedKeyword() {
        authenticate("admin", "SYSTEM_ADMIN");
        var pageable = PageRequest.of(0, 10);
        when(users.searchUnarchivedUsers("driver", pageable)).thenReturn(Page.empty());
        assertTrue(service.searchUsers(new UserQuery(" driver "), pageable).isEmpty());
        verify(users).searchUnarchivedUsers("driver", pageable);
    }

    @Test
    void archivedUserSearchUsesSeparateQueryAndSuppliedKeyword() {
        authenticate("admin", "SYSTEM_ADMIN");
        var pageable = PageRequest.of(0, 10);
        when(users.searchArchivedUsers("staff", pageable)).thenReturn(Page.empty());
        assertTrue(service.searchArchivedUsers(new UserQuery(" staff "), pageable).isEmpty());
        verify(users).searchArchivedUsers("staff", pageable);
        verify(users, never()).searchUnarchivedUsers(any(), any());
    }

    @Test
    void authenticationAuditUsesEventActorWithoutSecurityContext() {
        context.getBean(AuditRecordingService.class).recordAuthentication("attempted.user", false,
                "AuthenticationFailureBadCredentialsEvent");
        ArgumentCaptor<AuditLog> log = ArgumentCaptor.forClass(AuditLog.class);
        verify(audit).save(log.capture());
        assertEquals("attempted.user", log.getValue().getPerformedBy());
        assertEquals("FAILURE", log.getValue().getOutcome());
        assertEquals("USER_LOGIN_FAILED", log.getValue().getActionName());
    }

    private static CreateUserCommand command() {
        return new CreateUserCommand("new.staff", "ArcTransit1!", "New Staff", null,
                Set.of("OPERATIONS_STAFF"));
    }

    private static AppUser user(Long id, String username) {
        AppUser user = new AppUser(username, "old-hash", username, null);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static void authenticate(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    @Import(AuthenticationEventListener.class)
    static class TestConfiguration {
        @Bean AppUserRepository users() { return mock(AppUserRepository.class); }
        @Bean UserRoleRepository roles() { return mock(UserRoleRepository.class); }
        @Bean AuditLogRepository audit() { return mock(AuditLogRepository.class); }
        @Bean PasswordEncoder passwords() { return mock(PasswordEncoder.class); }
        @Bean AppAuditRecordingService auditService(AuditLogRepository repository) {
            return new AppAuditRecordingService(repository);
        }
        @Bean AppUserAdministrationService userService(AppUserRepository users, UserRoleRepository roles,
                PasswordEncoder passwords, AuditRecordingService audit) {
            return new AppUserAdministrationService(users, roles, passwords, "admin", audit);
        }
        @Bean AuthorizationEventPublisher authorizationEvents(
                org.springframework.context.ApplicationEventPublisher publisher) {
            return new SpringAuthorizationEventPublisher(publisher);
        }
        @Bean static MethodValidationPostProcessor validation() {
            var processor = new MethodValidationPostProcessor();
            processor.setProxyTargetClass(true);
            return processor;
        }
    }
}
