package com.transit.arctransit.auth;

import com.transit.arctransit.common.validation.SafeText;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.validation.Valid;

// public
// app_service contract for user admin

public interface UserAdministrationService {

    // checks current auth session
    CurrentUserView currentUser();

    // search users for management list view
    Page<UserSummaryView> searchUsers(@Valid UserQuery query, Pageable pageable);

    Page<UserSummaryView> searchArchivedUsers(@Valid UserQuery query, Pageable pageable);

    // create new user account
    UserView createUser(@NotNull @Valid CreateUserCommand command);

    UserView getUser(@NotBlank @Size(max = 50) @SafeText String username);

    UserView updateUser(@NotNull @Valid UpdateUserCommand command);

    // change user operational status (e.g., ACTIVE, DISABLED, LOCKED).
    UserView changeAccountStatus(@NotNull @Valid ChangeAccountStatusCommand command);

    // replace user role
    UserView replaceRoles(@NotNull @Valid ReplaceUserRolesCommand command);

    // Reset user password
    void resetPassword(@NotBlank @Size(max = 50) @SafeText String username, String newPassword);

    // archive and disable the account while retaining its identity
    void deleteUser(@NotBlank @Size(max = 50) @SafeText String username);

    void hardDeleteUser(@NotBlank @Size(max = 50) @SafeText String username);
}
