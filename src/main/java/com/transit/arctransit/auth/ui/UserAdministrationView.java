package com.transit.arctransit.auth.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

/**
 * User Administration dashboard for System Administrators.
 */
@Route("admin/users")
@PageTitle("User Administration")
@RolesAllowed("SYSTEM_ADMIN")
public class UserAdministrationView extends VerticalLayout {

    public UserAdministrationView() {
        // Added human-understandable comment: This view is strictly protected by @RolesAllowed.
        // If a normal Operations Staff tries to navigate to /admin/users, Spring Security
        // will automatically block them, fulfilling the role-aware navigation requirement.
        add(new H1("User Administration (System Admins Only)"));
    }
}
