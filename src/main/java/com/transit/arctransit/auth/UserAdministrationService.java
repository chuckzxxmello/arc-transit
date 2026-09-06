package com.transit.arctransit.auth;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.validation.Valid;

// public
// app_service contract for user admin

public interface UserAdministrationService {

    // checks current auth session
    CurrentUserView currentUser();

    // search users for management list view
    Page<UserSummaryView> searchUsers(UserQuery query, Pageable pageable);

    // create new user account
    UserView createUser(@Valid CreateUserCommand command);

    // change user operational status (e.g., ACTIVE, DISABLED, LOCKED).
    UserView changeAccountStatus(@Valid ChangeAccountStatusCommand command);

    // replace user role
    UserView replaceRoles(@Valid ReplaceUserRolesCommand command);

    // Reset user password
    void resetPassword(String username, String newPassword);

    // delete user account
    void deleteUser(String username);
}
