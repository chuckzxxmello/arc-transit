package com.transit.arctransit.auth.ui;

import com.transit.arctransit.auth.*;
import com.transit.arctransit.common.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.data.domain.PageRequest;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * User Administration dashboard for System Administrators.
 */
@Route(value = "admin/users", layout = MainLayout.class)
@PageTitle("User Administration")
@RolesAllowed("SYSTEM_ADMIN")
public class UserAdministrationView extends VerticalLayout {

    private final UserAdministrationService userService;
    private final Grid<UserSummaryView> grid;
    private final SplitLayout splitLayout = new SplitLayout();
    private final VerticalLayout sidePanel = new VerticalLayout();
    private final Div sidePanelContent = new Div();

    public UserAdministrationView(UserAdministrationService userService) {
        this.userService = userService;
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        // Header and Search
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        headerLayout.setPadding(true);

        H2 title = new H2("User Administration");
        title.getStyle().set("margin", "0");

        Button createBtn = new Button("Create Staff Account", e -> openCreateDialog());
        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        TextField searchField = new TextField();
        searchField.setPlaceholder("Search users...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> refreshGrid(e.getValue()));

        HorizontalLayout actionsLayout = new HorizontalLayout(searchField, createBtn);
        headerLayout.add(title, actionsLayout);

        // Grid
        grid = new Grid<>(UserSummaryView.class, false);
        grid.addColumn(UserSummaryView::username).setHeader("Username");
        grid.addColumn(UserSummaryView::displayName).setHeader("Name");
        grid.addColumn(UserSummaryView::accountStatus).setHeader("Status");
        
        grid.addSelectionListener(event -> {
            event.getFirstSelectedItem().ifPresent(this::showUserDetails);
        });

        VerticalLayout gridContainer = new VerticalLayout(headerLayout, grid);
        gridContainer.setSizeFull();
        gridContainer.setPadding(false);

        // Side Panel setup
        Button closeButton = new Button(VaadinIcon.CLOSE.create(), e -> hideSidePanel());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        HorizontalLayout sidePanelHeader = new HorizontalLayout(new H3("Details"), closeButton);
        sidePanelHeader.setWidthFull();
        sidePanelHeader.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        sidePanelHeader.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        
        sidePanel.add(sidePanelHeader, sidePanelContent);
        sidePanel.setVisible(false);
        sidePanel.setWidth("350px");
        sidePanel.getStyle().set("background-color", "#ffffff").set("border-left", "1px solid #ddd");

        splitLayout.addToPrimary(gridContainer);
        splitLayout.addToSecondary(sidePanel);
        splitLayout.setSplitterPosition(100);
        splitLayout.setSizeFull();

        add(splitLayout);
        refreshGrid("");
    }
    
    private void showUserDetails(UserSummaryView user) {
        sidePanelContent.removeAll();
        sidePanelContent.add(new Paragraph("Username: " + user.username()));
        sidePanelContent.add(new Paragraph("Name: " + user.displayName()));
        sidePanelContent.add(new Paragraph("Status: " + user.accountStatus()));
        
        Button resetBtn = new Button("Reset Password", e -> openResetPasswordDialog(user.username()));
        resetBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        
        sidePanelContent.add(new HorizontalLayout(resetBtn));
        
        sidePanel.setVisible(true);
        splitLayout.setSplitterPosition(75);
    }
    
    private void hideSidePanel() {
        sidePanel.setVisible(false);
        splitLayout.setSplitterPosition(100);
    }

    private void openResetPasswordDialog(String username) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Reset Password for " + username);

        PasswordField password = new PasswordField("New Password");
        FormLayout formLayout = new FormLayout(password);

        Button saveBtn = new Button("Reset", e -> {
            try {
                userService.resetPassword(username, password.getValue());
                dialog.close();
                Notification.show("Password reset successfully").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, saveBtn);

        dialog.add(formLayout);
        dialog.open();
    }

    private void openCreateDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Create Staff Account");

        TextField username = new TextField("Username");
        PasswordField password = new PasswordField("Password");
        TextField displayName = new TextField("Display Name");
        TextField email = new TextField("Email (Optional)");
        ComboBox<String> role = new ComboBox<>("Role");
        role.setItems("OPERATIONS_STAFF", "SYSTEM_ADMIN");
        role.setValue("OPERATIONS_STAFF");

        FormLayout formLayout = new FormLayout(username, password, displayName, email, role);

        Button saveBtn = new Button("Save", e -> {
            try {
                userService.createUser(new CreateUserCommand(
                        username.getValue(),
                        password.getValue(),
                        displayName.getValue(),
                        email.getValue().isEmpty() ? null : email.getValue(),
                        Set.of(role.getValue())
                ));
                refreshGrid("");
                dialog.close();
                Notification.show("User created successfully").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, saveBtn);

        dialog.add(formLayout);
        dialog.open();
    }

    private void refreshGrid(String filter) {
        // Basic filtering can be done in-memory if backend search is not implemented,
        // but here we just pass the filter if a backend query is used.
        // For now, we get all and filter in memory since UserQuery doesn't have a search term field yet.
        var users = userService.searchUsers(new UserQuery(null), PageRequest.of(0, 100)).getContent();
        if (filter != null && !filter.isEmpty()) {
            String lowerFilter = filter.toLowerCase();
            users = users.stream()
                .filter(u -> u.username().toLowerCase().contains(lowerFilter) || 
                             u.displayName().toLowerCase().contains(lowerFilter))
                .collect(Collectors.toList());
        }
        grid.setItems(users);
    }
}
