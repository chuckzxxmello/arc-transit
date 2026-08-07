package com.transit.arctransit.auth;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.validation.Valid;

/**
 * Public application service contract for User Administration.
 * Forms the boundary between Vaadin views and the Auth module internals.
 */
public interface UserAdministrationService {

    /**
     * Reads the current authenticated session principal.
     */
    CurrentUserView currentUser();

    /**
     * Searches users for management list views.
     */
    Page<UserSummaryView> searchUsers(UserQuery query, Pageable pageable);

    /**
     * Creates a new staff account.
     */
    UserView createUser(@Valid CreateUserCommand command);

    /**
     * Changes an account's operational status (e.g., ACTIVE, DISABLED, LOCKED).
     */
    UserView changeAccountStatus(@Valid ChangeAccountStatusCommand command);

    /**
     * Replaces the roles assigned to a user.
     */
    UserView replaceRoles(@Valid ReplaceUserRolesCommand command);

    /**
     * Resets a user's password.
     */
    void resetPassword(String username, String newPassword);

    /**
     * Deletes a staff account permanently.
     */
    void deleteUser(String username);
}
