package com.transit.arctransit.auth.ui;

// auth module
import com.transit.arctransit.auth.CreateUserCommand;
import com.transit.arctransit.auth.CurrentUserView;
import com.transit.arctransit.auth.ReplaceUserRolesCommand;
import com.transit.arctransit.auth.UserAdministrationService;
import com.transit.arctransit.auth.UserQuery;
import com.transit.arctransit.auth.UserSummaryView;

// UI
import com.transit.arctransit.common.ui.MainLayout;

// vaadin components
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
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

import com.vaadin.flow.data.binder.ValidationException;

import jakarta.annotation.security.RolesAllowed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

// user dashboard for SYSTEM_ADMINs
// provides the user-management interface for:
// 		-> searching staff accounts
//		-> creating staff accounts
//		-> resetting passwords
//		-> changing assigned roles
//		-> deleting staff accounts

@Route(value = "admin/users", layout = MainLayout.class)
@PageTitle("User Administration")
@RolesAllowed("SYSTEM_ADMIN")
public class UserAdministrationView extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(UserAdministrationView.class);
    private static final String SYSTEM_ADMIN_ROLE = "SYSTEM_ADMIN";
    private static final String OPERATIONS_STAFF_ROLE = "OPERATIONS_STAFF";
    private static final List<String> SUPPORTED_ROLES = List.of(
													            OPERATIONS_STAFF_ROLE,
													            SYSTEM_ADMIN_ROLE
    );

    private final UserAdministrationService userService;
    private final Grid<UserSummaryView> grid;
    private final SplitLayout splitLayout = new SplitLayout();
    private final VerticalLayout sidePanel = new VerticalLayout();
    private final Div sidePanelContent = new Div();

    private String searchKeyword = "";

    public UserAdministrationView(UserAdministrationService userService) {
        this.userService = userService;
        configureView();

        this.grid = createGrid();
        configureSidePanel();

        SplitLayout mainContent =
                createMainContent();
        add(mainContent);
    }

    // view
    private void configureView() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);
    }

    // grid
    private Grid<UserSummaryView> createGrid() {

        Grid<UserSummaryView> userGrid =
                new Grid<>(UserSummaryView.class, false);

        userGrid.setSizeFull();

        userGrid.addColumn(UserSummaryView::username)
                .setHeader("Username");

        userGrid.addColumn(UserSummaryView::displayName)
                .setHeader("Name");

        userGrid.addColumn(UserSummaryView::accountStatus)
                .setHeader("Status");

        userGrid.addSelectionListener(event ->
                event.getFirstSelectedItem()
                        .ifPresent(this::showUserDetails)
        );

        userGrid.setItemsPageable(pageable ->
        userService.searchUsers(
                new UserQuery(searchKeyword),
                pageable).getContent()
        );

        return userGrid;
    }

// MAIN CONTENT
    private SplitLayout createMainContent() {

        VerticalLayout gridContainer = new VerticalLayout(
							                        createHeader(),
							                        grid
        );

        gridContainer.setSizeFull();
        gridContainer.setPadding(false);
        gridContainer.setSpacing(false);

        splitLayout.addToPrimary(gridContainer);
        splitLayout.addToSecondary(sidePanel);

        splitLayout.setSplitterPosition(100);
        splitLayout.setSizeFull();

        return splitLayout;
    }

// HEADER and SEARCH
    private HorizontalLayout createHeader() {

        HorizontalLayout headerLayout = new HorizontalLayout();

        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(
                FlexComponent.JustifyContentMode.BETWEEN
        );

        headerLayout.setDefaultVerticalComponentAlignment(
                FlexComponent.Alignment.CENTER
        );

        headerLayout.setPadding(true);

        H2 title = new H2("User Administration");
        title.getStyle().set("margin", "0");

        TextField searchField = new TextField();

        searchField.setPlaceholder("Search users...");
        searchField.setPrefixComponent(
                VaadinIcon.SEARCH.create()
        );
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(
                ValueChangeMode.LAZY
        );
        searchField.addValueChangeListener(event -> {

            searchKeyword = normalizeSearchKeyword(
                            				event.getValue()
            );
        refreshGrid();
        });

        Button createButton =
                new Button("Create Staff Account",
                        event -> openCreateDialog()
        );

        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout actions = new HorizontalLayout(
                        searchField,
                        createButton
        );

        headerLayout.add(
                title,
                actions
        );
        return headerLayout;
    }

// USER DETAILS
    private void showUserDetails(UserSummaryView user) {
    	
        sidePanelContent.removeAll();
        
        sidePanelContent.add(
                new Paragraph("Username: " + user.username())
        );

        sidePanelContent.add(
                new Paragraph("Name: " + user.displayName())
        );

        sidePanelContent.add(
                new Paragraph("Status: " + user.accountStatus())
        );

        Button resetPasswordButton =
                new Button("Reset Password",
                        event ->
                                openResetPasswordDialog(user.username())
                );

        resetPasswordButton.addThemeVariants(
                ButtonVariant.LUMO_PRIMARY
        );

        Button changeRoleButton =
                new Button("Update Role",
                        event ->
                                openChangeRoleDialog(user.username())
                );

        changeRoleButton.addThemeVariants(
                ButtonVariant.LUMO_CONTRAST
        );

        Button deleteUserButton =
                new Button("Delete Account",
                        event ->
                                openDeleteUserDialog(user.username())
                );

        deleteUserButton.addThemeVariants(
                ButtonVariant.LUMO_PRIMARY,
                ButtonVariant.LUMO_ERROR
        );

        VerticalLayout actions =
                new VerticalLayout(
                        resetPasswordButton,
                        changeRoleButton,
                        deleteUserButton
                );

        actions.setPadding(false);

        sidePanelContent.add(actions);

        sidePanel.setVisible(true);

        splitLayout.setSplitterPosition(75);
    }

// SIDE PANEL
    private void configureSidePanel() {

        Button closeButton = new Button(
                        VaadinIcon.CLOSE.create(),
                        event -> hideSidePanel()
        );

        closeButton.addThemeVariants(
                ButtonVariant.LUMO_TERTIARY
        );

        HorizontalLayout header = new HorizontalLayout(
                        new H3("Details"),
                        closeButton
        );

        header.setWidthFull();

        header.setJustifyContentMode(
                FlexComponent.JustifyContentMode.BETWEEN
        );

        header.setDefaultVerticalComponentAlignment(
                FlexComponent.Alignment.CENTER
        );

        sidePanel.add(
                header,
                sidePanelContent
        );

        sidePanel.setVisible(false);
        sidePanel.setWidth("350px");

        sidePanel.getStyle()
                .set("background-color", "#ffffff")
                .set("border-left", "1px solid #ddd");
    }

    private void hideSidePanel() {

        sidePanel.setVisible(false);

        splitLayout.setSplitterPosition(100);
    }

// CREATE USER
    private void openCreateDialog() {

        Dialog dialog = new Dialog();

        dialog.setHeaderTitle("Create Staff Account");

        TextField username = new TextField("Username");
        username.setRequired(true);

        PasswordField password = new PasswordField("Password");
        password.setRequired(true);

        password.setHelperText(
                "At least 8 characters with uppercase, " +
                "lowercase, number, and special character."
        );

        TextField displayName = new TextField("Display Name");
        displayName.setRequired(true);

        TextField email =
                new TextField("Email (Optional)");

        ComboBox<String> role =
                createRoleSelector("Role");

        role.setValue(OPERATIONS_STAFF_ROLE);

        FormLayout formLayout =
                new FormLayout(
                        username,
                        password,
                        displayName,
                        email,
                        role
                );

        Button saveButton =
                new Button("Save",
                        event -> {
                            String selectedRole = role.getValue();

                            if (selectedRole == null || selectedRole.isBlank()) {
                                showError("Create user",
                                        new IllegalArgumentException("Select an access role.")
                                );
                                return;
                            }

                            try {
                                userService.createUser(
                                        new CreateUserCommand(
                                                username.getValue(),
                                                password.getValue(),
                                                displayName.getValue(),
                                                normalizeOptionalValue(email.getValue()),
                                                Set.of(selectedRole)
                                        )
                                );

                                dialog.close();
                                refreshGrid();
                                showSuccess("User created successfully.");

                            } catch (Exception exception) {
                                showError("Create user", exception);
                            }
                        }
                );

        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel",
                        event -> dialog.close()
        );

        dialog.getFooter().add(
                cancelButton,
                saveButton
        );

        dialog.add(formLayout);
        dialog.open();
    }

// CHANGE ROLE
    private void openChangeRoleDialog(String username) {
        
    	Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Update Role for " + username);

        ComboBox<String> roleCombo = createRoleSelector("Access Role");
        roleCombo.setRequired(true);

        FormLayout formLayout = new FormLayout(roleCombo);

        Button saveButton = new Button("Update Role",
                        event -> {
                            String selectedRole = roleCombo.getValue();

                            if (selectedRole == null || selectedRole.isBlank()) {

                                showError("Update role",
                                        new IllegalArgumentException("Select an access role.")
                                );
                            return;
                            }

                            try {

                                userService.replaceRoles(
                                        new ReplaceUserRolesCommand(
                                                username,
                                                Set.of(selectedRole)
                                        )
                                );

                                dialog.close();

                                refreshGrid();

                                hideSidePanel();

                                showSuccess("User role updated successfully.");

                            } catch (Exception exception) {

                                showError("Update role",exception);
                            }
                        }
                );

        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel",
                        event -> dialog.close()
        );

        dialog.getFooter().add(
                cancelButton,
                saveButton
        );

        dialog.add(formLayout);
        dialog.open();
    }

// RESET PASSWORD
    private void openResetPasswordDialog(String username) {

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Reset Password for " + username);

        PasswordField password = new PasswordField("New Password");
        password.setRequired(true);

        FormLayout formLayout = new FormLayout(password);

        Button saveButton = new Button("Reset",
                        event -> {
                            try {

                                userService.resetPassword(
                                        username,
                                        password.getValue()
                                );

                                dialog.close();
                                showSuccess("Password reset successfully.");

                            } catch (Exception exception) {
                                showError("Reset password",
                                        exception
                                );
                            }
                        }
                );

        saveButton.addThemeVariants(
                ButtonVariant.LUMO_PRIMARY,
                ButtonVariant.LUMO_ERROR
        );

        Button cancelButton = new Button("Cancel",
                        event -> dialog.close()
        );

        dialog.getFooter().add(
                cancelButton,
                saveButton
        );

        dialog.add(formLayout);
        dialog.open();
    }

// DELETE USER
    private void openDeleteUserDialog(String username) {

        ConfirmDialog dialog = new ConfirmDialog();

        dialog.setHeader(
                "Delete User Account: " +
                username +
                "?"
        );

        dialog.setText(
                "Are you sure you want to permanently " +
                "delete this staff account? " +
                "This action cannot be undone."
        );

        dialog.setCancelable(true);

        dialog.setConfirmText("Delete Account");

        dialog.setConfirmButtonTheme("error primary");

        dialog.addConfirmListener(event -> {

            try {

                userService.deleteUser(username);

                refreshGrid();

                hideSidePanel();

                showSuccess("User deleted successfully.");

            } catch (Exception exception) {

                showError("Delete user",
                        exception
                );
            }
        });

        dialog.open();
    }

// GRID REFRESH
    private void refreshGrid() {

        grid.getDataProvider().refreshAll();
    }

// UI HELPER METHODS

    private ComboBox<String> createRoleSelector(
            String label) {

        ComboBox<String> roleCombo = new ComboBox<>(label);
        roleCombo.setItems(SUPPORTED_ROLES);
        roleCombo.setRequiredIndicatorVisible(true);
        return roleCombo;
    }

    private static String normalizeSearchKeyword(
            String value) {

        if (value == null) {
            return "";
        }

        return value.trim();
    }

    private static String normalizeOptionalValue(String value) {

        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isBlank()
                ? null
                : normalized;
    }

// NOTIFICATIONS / ERROR HANDLING
    private void showSuccess(String message) {

        Notification.show(message)
                .addThemeVariants(
                        NotificationVariant.LUMO_SUCCESS
                );
    }

    private void showError(
            String operation,
            Exception exception) {
    	
        log.error(
                "{} failed.",
                operation,
                exception
        );

        String userMessage;

        if (exception instanceof RuntimeException runtimeException && isSafeApplicationException(
        runtimeException)) {
            userMessage = runtimeException.getMessage();

        } else {
            userMessage =
                    "The operation could not be completed. " +
                    "Please try again.";
        }

        if (userMessage == null || userMessage.isBlank()) {
            userMessage =
                    "The operation could not be completed. " +
                    "Please try again.";
        }

        Notification.show(userMessage).addThemeVariants(
        		NotificationVariant.LUMO_ERROR
        );
    }

    private static boolean isSafeApplicationException(RuntimeException exception) {
        return exception
                instanceof com.transit.arctransit.common.exception.BusinessConflictException
                || exception
                instanceof com.transit.arctransit.common.exception.CommandValidationException
                || exception
                instanceof com.transit.arctransit.common.exception.ResourceNotFoundException;
        
        //TODO
    }
}