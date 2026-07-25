package com.transit.arctransit.analytics.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import com.vaadin.flow.router.RouteAlias;

import jakarta.annotation.security.PermitAll;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinServletRequest;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

@Route("")
@RouteAlias("dashboard")
@PageTitle("Arc Transit Systems")
@PermitAll
public class DashboardView extends VerticalLayout {

    public DashboardView() {
        // Added human-understandable comment: This H1 acts as the main title for our dashboard view.
        add(new H1("This is a test for arc transit system vaadin dashboard"));

        // Added human-understandable comment: This button triggers Spring Security's logout process.
        // It securely clears the session and redirects the user back to the root page (which then redirects to /login).
        Button logoutButton = new Button("Logout", click -> {
            new SecurityContextLogoutHandler().logout(
                    VaadinServletRequest.getCurrent().getHttpServletRequest(), null, null);
            UI.getCurrent().getPage().setLocation("/");
        });
        
        add(logoutButton);
    }

}
