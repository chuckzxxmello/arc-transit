package com.transit.arctransit.common.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.theme.lumo.LumoUtility;

import jakarta.annotation.security.PermitAll;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

// main application shell

@PermitAll
public class MainLayout extends AppLayout {

    // security authorities

    private static final String SYSTEM_ADMIN_ROLE =
            "ROLE_SYSTEM_ADMIN";

    private static final String OPERATIONS_STAFF_ROLE =
            "ROLE_OPERATIONS_STAFF";

    // constructor

    public MainLayout() {

        createHeader();
        createDrawer();
    }

    // application header

    private void createHeader() {

        H1 logo =
                new H1("ARC TRANSIT");

        logo.addClassNames(
                LumoUtility.FontSize.LARGE,
                LumoUtility.Margin.MEDIUM
        );

        Span subtitle =
                new Span(
                        "Operations Control Center"
                );

        subtitle.addClassNames(
                LumoUtility.FontSize.SMALL,
                LumoUtility.TextColor.SECONDARY
        );

        VerticalLayout titles =
                new VerticalLayout(
                        logo,
                        subtitle
                );

        titles.setSpacing(false);
        titles.setPadding(false);

        Span clock =
                createClock();

        HorizontalLayout left =
                new HorizontalLayout(
                        titles
                );

        left.setWidthFull();

        left.setDefaultVerticalComponentAlignment(
                FlexComponent.Alignment.CENTER
        );

        HorizontalLayout center =
                new HorizontalLayout();

        center.setWidthFull();

        center.setJustifyContentMode(
                FlexComponent.JustifyContentMode.CENTER
        );

        HorizontalLayout right =
                new HorizontalLayout(
                        clock
                );

        right.setWidthFull();

        right.setJustifyContentMode(
                FlexComponent.JustifyContentMode.END
        );

        right.setDefaultVerticalComponentAlignment(
                FlexComponent.Alignment.CENTER
        );

        HorizontalLayout header =
                new HorizontalLayout(
                        left,
                        center,
                        right
                );

        header.setWidthFull();

        header.setDefaultVerticalComponentAlignment(
                FlexComponent.Alignment.CENTER
        );

        header.addClassNames(
                LumoUtility.Padding.Vertical.NONE,
                LumoUtility.Padding.Horizontal.MEDIUM
        );

        addToNavbar(header);

        // keep the drawer permanently open

        setDrawerOpened(true);
    }

    // live clock

    private Span createClock() {

        Span clock =
                new Span();

        clock.setId(
                "live-clock"
        );

        clock.addClassNames(
                LumoUtility.FontWeight.BOLD,
                LumoUtility.Margin.Right.LARGE
        );

        clock.getStyle().set(
                "white-space",
                "nowrap"
        );

        UI.getCurrent()
                .getPage()
                .executeJs(
                        """
                        setInterval(function () {
                            const element =
                                document.getElementById('live-clock');

                            if (!element) {
                                return;
                            }

                            const now = new Date();

                            const dateText =
                                now.toLocaleDateString(
                                    'en-US',
                                    {
                                        weekday: 'short',
                                        month: 'short',
                                        day: 'numeric'
                                    }
                                );

                            const timeText =
                                now.toLocaleTimeString(
                                    'en-US',
                                    {
                                        hour12: true,
                                        hour: '2-digit',
                                        minute: '2-digit',
                                        second: '2-digit'
                                    }
                                );

                            element.innerText =
                                dateText + ' • ' + timeText;
                        }, 1000);
                        """
                );

        return clock;
    }

    // application navigation drawer

    private void createDrawer() {

        SideNav navigation =
                new SideNav();

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        boolean isAdmin =
                hasAuthority(
                        authentication,
                        SYSTEM_ADMIN_ROLE
                );

        boolean isOperationsStaff =
                hasAuthority(
                        authentication,
                        OPERATIONS_STAFF_ROLE
                );

        boolean isStaff =
                isAdmin || isOperationsStaff;

        // available to authenticated staff

        if (isStaff) {

            navigation.addItem(
                    new SideNavItem(
                            "Dashboard Overview",
                            "",
                            VaadinIcon.DASHBOARD.create()
                    )
            );

            navigation.addItem(
                    new SideNavItem(
                            "Dispatch & Assignment",
                            "dispatch",
                            VaadinIcon.CLIPBOARD_CHECK.create()
                    )
            );

            navigation.addItem(
                    new SideNavItem(
                            "Live Fleet Monitor",
                            "fleet",
                            VaadinIcon.BUS.create()
                    )
            );

            navigation.addItem(
                    new SideNavItem(
                            "Driver Management",
                            "drivers",
                            VaadinIcon.USER_CARD.create()
                    )
            );

            navigation.addItem(
                    new SideNavItem(
                            "Route Schedule",
                            "routes",
                            VaadinIcon.MAP_MARKER.create()
                    )
            );
        }

        // system administrator only

        if (isAdmin) {

            navigation.addItem(
                    new SideNavItem(
                            "Incident Management",
                            "incidents",
                            VaadinIcon.WARNING.create()
                    )
            );

            navigation.addItem(
                    new SideNavItem(
                            "Maintenance Management",
                            "maintenance",
                            VaadinIcon.WRENCH.create()
                    )
            );

            navigation.addItem(
                    new SideNavItem(
                            "Exception Queue",
                            "exceptions",
                            VaadinIcon.STETHOSCOPE.create()
                    )
            );

            navigation.addItem(
                    new SideNavItem(
                            "Archived Data",
                            "archive",
                            VaadinIcon.ARCHIVE.create()
                    )
            );

            navigation.addItem(
                    new SideNavItem(
                            "User Administration",
                            "admin/users",
                            VaadinIcon.USERS.create()
                    )
            );
        }

        Scroller scroller =
                new Scroller(navigation);

        scroller.setClassName(
                LumoUtility.Padding.SMALL
        );

        // logout

        Button logoutButton =
                new Button(
                        "Logout",
                        VaadinIcon.SIGN_OUT.create(),
                        event -> logout()
                );

        logoutButton.addThemeVariants(
                ButtonVariant.LUMO_TERTIARY,
                ButtonVariant.LUMO_ERROR
        );

        logoutButton.setWidthFull();

        VerticalLayout drawer =
                new VerticalLayout(
                        scroller,
                        logoutButton
                );

        drawer.setSizeFull();

        drawer.setPadding(false);

        drawer.setJustifyContentMode(
                FlexComponent.JustifyContentMode.BETWEEN
        );

        addToDrawer(drawer);
    }

    // check security authority

    private boolean hasAuthority(
            Authentication authentication,
            String authority) {

        if (authentication == null
                || !authentication.isAuthenticated()) {

            return false;
        }

        return authentication
                .getAuthorities()
                .stream()
                .anyMatch(grantedAuthority ->
                        authority.equals(
                                grantedAuthority.getAuthority()
                        )
                );
    }

    // logout

    private void logout() {

        var request =
                VaadinServletRequest
                        .getCurrent()
                        .getHttpServletRequest();

        new SecurityContextLogoutHandler()
                .logout(
                        request,
                        null,
                        null
                );

        UI.getCurrent()
                .getPage()
                .setLocation("/login");
    }
}