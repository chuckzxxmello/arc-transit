package com.transit.arctransit.auth.ui;

import com.transit.arctransit.common.ui.RefreshButton;
// auth module
import com.transit.arctransit.common.ui.FormInputs;

import com.transit.arctransit.auth.CreateUserCommand;
import com.transit.arctransit.auth.PasswordPolicy;
import com.transit.arctransit.auth.CurrentUserView;
import com.transit.arctransit.auth.UpdateUserCommand;
import com.transit.arctransit.auth.UserView;
import com.vaadin.flow.spring.security.AuthenticationContext;
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
import com.transit.arctransit.common.ui.DataGrids;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
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
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// user dashboard for SYSTEM_ADMINs
// provides the user-management interface for:
// 		-> searching staff accounts
//		-> creating staff accounts
//		-> resetting passwords
//		-> changing assigned roles
//		-> archiving staff accounts

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
    private final AuthenticationContext authenticationContext;
    private final Grid<UserSummaryView> grid;
    private final SplitLayout splitLayout = new SplitLayout();
    private final VerticalLayout sidePanel = new VerticalLayout();
    private final Div sidePanelContent = new Div();

    private String searchKeyword = "";

    public UserAdministrationView(UserAdministrationService userService, AuthenticationContext authenticationContext) {
        this.userService = userService;
        this.authenticationContext = authenticationContext;
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
                DataGrids.create(UserSummaryView.class, false);

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

        TextField searchField = FormInputs.text(new TextField(), 200);

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
                        createButton,
                        new RefreshButton(this::refreshGrid)
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
                new Button("Edit Account",
                        event ->
                                openEditDialog(user.username())
                );

        changeRoleButton.addThemeVariants(
                ButtonVariant.LUMO_CONTRAST
        );

        Button deleteUserButton =
                new Button("Archive Account",
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

        TextField username = FormInputs.text(new TextField("Username"), 50);
        username.setRequired(true);

        PasswordField password = createPasswordField("Password");

        TextField displayName = FormInputs.text(new TextField("Display Name"), 100);
        displayName.setRequired(true);

        TextField email =
                FormInputs.text(new TextField("Email (Optional)"), 200);

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
                            if (!FormInputs.validate(dialog)) {
                                return;
                            }

                            if (!validatePasswordField(password)) {
                                return;
                            }
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

// EDIT ACCOUNT
    private void openEditDialog(String selectedUsername) {
        UserView account;
        try {
            account = userService.getUser(selectedUsername);
        } catch (Exception exception) {
            showError("Load account", exception);
            return;
        }
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Staff Account");
        dialog.setWidth("760px");
        dialog.setMaxWidth("calc(100vw - 48px)");
        TextField username = FormInputs.text(new TextField("Username"), 50);
        username.setRequiredIndicatorVisible(true);
        username.setValue(account.username());
        username.setHelperText("Previous usernames remain reserved for account history.");
        TextField displayName = FormInputs.text(new TextField("Display Name"), 100);
        displayName.setRequiredIndicatorVisible(true);
        displayName.setValue(account.displayName());
        TextField email = FormInputs.text(new TextField("Email (Optional)"), 200);
        email.setValue(account.email() == null ? "" : account.email());
        ComboBox<String> roleCombo = createRoleSelector("Access Role");
        String initialRole = account.roles().contains(SYSTEM_ADMIN_ROLE)
                ? SYSTEM_ADMIN_ROLE : account.roles().stream().findFirst().orElse(null);
        if (initialRole != null) {
            roleCombo.setValue(initialRole);
        }
        if (account.roles().size() > 1) {
            roleCombo.setHelperText("Changing this selection replaces the account's existing roles.");
        }
        FormLayout form = new FormLayout(username, displayName, email, roleCombo);
        form.setWidthFull();
        dialog.add(form, new Paragraph("Changing your own username or role requires signing in again."));
        Button save = new Button("Save Changes", event -> {
            if (!FormInputs.validate(dialog)) {
                return;
            }
            Set<String> roles = java.util.Objects.equals(initialRole, roleCombo.getValue())
                    ? account.roles() : Set.of(roleCombo.getValue());
            try {
                UserView updated = userService.updateUser(new UpdateUserCommand(account.id(), account.version(),
                        username.getValue(), displayName.getValue(), normalizeOptionalValue(email.getValue()), roles));
                dialog.close();
                var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                boolean ownAccount = authentication != null && account.username().equalsIgnoreCase(authentication.getName());
                if (ownAccount && (!account.username().equals(updated.username()) || !account.roles().equals(updated.roles()))) {
                    authenticationContext.logout();
                    return;
                }
                refreshGrid();
                hideSidePanel();
                showSuccess("Account updated successfully.");
            } catch (Exception exception) {
                showError("Update account", exception);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(new Button("Cancel", event -> dialog.close()), save);
        dialog.open();
    }

// RESET PASSWORD
    private void openResetPasswordDialog(String username) {

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Reset Password for " + username);
        dialog.setWidth("520px");
        dialog.setMaxWidth("calc(100vw - 48px)");

        PasswordField password = createPasswordField("New Password");

        FormLayout formLayout = new FormLayout(password);
        formLayout.setWidthFull();
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button saveButton = new Button("Reset",
                        event -> {
                            if (!FormInputs.validate(dialog)) {
                                return;
                            }

                            if (!validatePasswordField(password)) {
                                return;
                            }
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

    private PasswordField createPasswordField(String label) {
        PasswordField password = new PasswordField(label);
        password.setMaxLength(PasswordPolicy.MAX_LENGTH);
        password.setWidthFull();
        password.setRequiredIndicatorVisible(true);
        password.setManualValidation(true);
        password.setValueChangeMode(ValueChangeMode.EAGER);

        Span strength = new Span("Password strength: weak");
        strength.setVisible(false);
        strength.getStyle().set("display", "block").set("font-weight", "600");
        strength.getElement().setAttribute("role", "status");
        strength.getElement().setAttribute("aria-live", "polite");
        strength.getElement().setAttribute("title",
                "Estimate based on length, character variety and common patterns.");
        password.setHelperComponent(new Div(new Span(PasswordPolicy.MESSAGE), strength));
        password.addValueChangeListener(event -> {
            strength.setVisible(!event.getValue().isEmpty());
            String level = PasswordPolicy.estimatedStrength(event.getValue());
            strength.setText("Password strength: " + level);
            String color = switch (level) {
                case "moderate" -> "var(--lumo-warning-text-color, #935d00)";
                case "strong", "very strong" -> "var(--lumo-success-text-color, #18713c)";
                default -> "var(--lumo-error-text-color, #b42318)";
            };
            strength.getStyle().set("color", color);
            validatePasswordField(password);
        });
        password.addBlurListener(event -> validatePasswordField(password));
        return password;
    }

    private boolean validatePasswordField(PasswordField password) {
        String message = PasswordPolicy.validationMessage(password.getValue());
        password.setErrorMessage(message == null ? "" : message);
        password.setInvalid(message != null);
        return message == null;
    }

// DELETE USER
    private void openDeleteUserDialog(String username) {

        ConfirmDialog dialog = new ConfirmDialog();

        dialog.setHeader(
                "Archive User Account: " +
                username +
                "?"
        );

        dialog.setText(
                "Archive and disable this staff account? " +
                "Its identity and role history will be retained."
        );

        dialog.setCancelable(true);

        dialog.setConfirmText("Archive Account");

        dialog.setConfirmButtonTheme("error primary");

        dialog.addConfirmListener(event -> {

            try {

                userService.deleteUser(username);

                refreshGrid();

                hideSidePanel();

                showSuccess("User archived successfully.");

            } catch (Exception exception) {

                showError("Archive user",
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

        ComboBox<String> roleCombo = FormInputs.choice(new ComboBox<>(label));
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
        com.transit.arctransit.common.ui.ApplicationErrors.show(exception);
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
