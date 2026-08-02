package com.transit.arctransit.common.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import jakarta.annotation.security.PermitAll;

/**
 * Main application layout serving as the app shell with header and sidebar navigation.
 */
@PermitAll
public class MainLayout extends AppLayout {

    public MainLayout() {
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 logo = new H1("ARC TRANSIT SYSTEMS");
        logo.addClassNames(
                LumoUtility.FontSize.LARGE,
                LumoUtility.Margin.MEDIUM);

        Span subtitle = new Span("Operations Control Center");
        subtitle.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        VerticalLayout titles = new VerticalLayout(logo, subtitle);
        titles.setSpacing(false);
        titles.setPadding(false);



        // Live Clock using simple executeJs (HH:mm:ss a) format like "20:13:13 PM"
        Span clockSpan = new Span();
        clockSpan.setId("live-clock");
        clockSpan.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.Margin.Right.LARGE);
        clockSpan.getStyle().set("white-space", "nowrap");

        UI.getCurrent().getPage().executeJs(
                "setInterval(function() {" +
                        "  var d = new Date();" +
                        "  var options = { weekday: 'short', month: 'short', day: 'numeric' };" +
                        "  var dateStr = d.toLocaleDateString('en-US', options);" +
                        "  var timeStr = d.toLocaleTimeString('en-US', { hour12: true, hour: '2-digit', minute: '2-digit', second: '2-digit' });" +
                        "  document.getElementById('live-clock').innerText = dateStr + ' • ' + timeStr;" +
                        "}, 1000);"
        );

        HorizontalLayout leftContainer = new HorizontalLayout(titles);
        leftContainer.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        leftContainer.setWidthFull();

        HorizontalLayout centerContainer = new HorizontalLayout();
        centerContainer.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        centerContainer.setWidthFull();

        HorizontalLayout rightContainer = new HorizontalLayout(clockSpan);
        rightContainer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        rightContainer.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        rightContainer.setWidthFull();

        HorizontalLayout header = new HorizontalLayout(
                leftContainer,
                centerContainer,
                rightContainer
        );

        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.addClassNames(LumoUtility.Padding.Vertical.NONE, LumoUtility.Padding.Horizontal.MEDIUM);

        addToNavbar(header);
        
        // Remove drawer toggle button to keep drawer permanently open
        setDrawerOpened(true);
    }

    private void createDrawer() {
        SideNav nav = new SideNav();

        // Using string paths instead of class references to avoid cyclic dependencies in Modulith
        nav.addItem(new SideNavItem("Dashboard Overview", "", VaadinIcon.DASHBOARD.create()));
        nav.addItem(new SideNavItem("Live Fleet Monitor", "fleet", VaadinIcon.BUS.create()));
        nav.addItem(new SideNavItem("Driver Management", "drivers", VaadinIcon.USER_CARD.create()));
        nav.addItem(new SideNavItem("Route Schedule", "routes", VaadinIcon.MAP_MARKER.create()));
        nav.addItem(new SideNavItem("Dispatch & Assignment", "dispatch", VaadinIcon.CLIPBOARD_CHECK.create()));
        nav.addItem(new SideNavItem("Archived Data", "archive", VaadinIcon.ARCHIVE.create()));

        // Check Roles for User Administration
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SYSTEM_ADMIN"))) {
            nav.addItem(new SideNavItem("User Administration", "admin/users", VaadinIcon.USERS.create()));
        }

        Scroller scroller = new Scroller(nav);
        scroller.setClassName(LumoUtility.Padding.SMALL);

        // Logout Button at the bottom of the drawer
        Button logoutBtn = new Button("Logout", VaadinIcon.SIGN_OUT.create(), click -> {
            new SecurityContextLogoutHandler().logout(
                    VaadinServletRequest.getCurrent().getHttpServletRequest(), null, null);
            UI.getCurrent().getPage().setLocation("/");
        });
        logoutBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        logoutBtn.setWidthFull();

        VerticalLayout drawerLayout = new VerticalLayout(scroller, logoutBtn);
        drawerLayout.setSizeFull();
        drawerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        addToDrawer(drawerLayout);
    }
}
