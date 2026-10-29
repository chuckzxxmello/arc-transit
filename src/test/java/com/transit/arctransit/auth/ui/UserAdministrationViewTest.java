package com.transit.arctransit.auth.ui;

import com.transit.arctransit.auth.UserAdministrationService;
import com.transit.arctransit.auth.UserView;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.VaadinSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserAdministrationViewTest {
    private UI ui;
    private UserAdministrationService service;
    private AuthenticationContext authentication;

    @BeforeEach
    void setUp() {
        VaadinSession session = mock(VaadinSession.class, RETURNS_DEEP_STUBS);
        when(session.hasLock()).thenReturn(true);
        VaadinSession.setCurrent(session);
        ui = new UI();
        ui.getInternals().setSession(session);
        UI.setCurrent(ui);
        service = mock(UserAdministrationService.class);
        when(service.searchUsers(any(), any())).thenReturn(Page.empty());
        authentication = mock(AuthenticationContext.class);
        ui.add(new UserAdministrationView(service, authentication));
    }

    @AfterEach
    void tearDown() {
        UI.setCurrent(null);
        VaadinSession.setCurrent(null);
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void weakPasswordShowsValidationReasonAndKeepsCreateDialogOpen() {
        Dialog dialog = openCreateDialog();
        button(dialog, "Save").click();

        PasswordField field = passwordField(dialog);
        String message = field.getErrorMessage();
        assertTrue(field.isInvalid());
        assertTrue(message.contains("uppercase"), message);
        assertTrue(message.contains("special character"), message);
        assertFalse(message.contains("usertest123"));
        assertFalse(message.contains("createUser.command"));
        assertTrue(dialog.isOpened());
        verify(service, never()).createUser(any());
    }

    @Test
    void unexpectedFailureDoesNotExposeInternalDetails() {
        when(service.createUser(any())).thenThrow(new IllegalStateException("private database details"));

        Dialog dialog = openCreateDialog();
        passwordField(dialog).setValue("FleetTest899!@#");
        button(dialog, "Save").click();

        assertEquals("The operation could not be completed. Please try again.", notificationText());
        assertTrue(dialog.isOpened());
    }

    @Test
    void hashSymbolClearsInvalidStateAndSubmitsOperationsStaffAccount() {
        Dialog dialog = openCreateDialog();
        PasswordField field = passwordField(dialog);
        assertEquals(16, field.getMaxLength());
        assertTrue(field.isInvalid());
        field.setValue("FleetTest899!@#");
        assertFalse(field.isInvalid());
        assertEquals("", field.getErrorMessage());
        button(dialog, "Save").click();
        verify(service).createUser(argThat(command -> "FleetTest899!@#".equals(command.password())
                && Set.of("OPERATIONS_STAFF").equals(command.roles())));
        assertFalse(dialog.isOpened());
    }

    @Test
    void strengthUpdatesWhileTypingAndClearingShowsRequiredError() {
        Dialog dialog = openCreateDialog();
        PasswordField field = passwordField(dialog);
        field.setValue("short");
        assertStrength(field, "weak");
        assertTrue(field.isInvalid());
        field.setValue("Lime89!?");
        assertStrength(field, "moderate");
        assertFalse(field.isInvalid());
        field.setValue("Maple7!River2");
        assertStrength(field, "strong");
        field.setValue("Cedar9#Sea4!Moon");
        assertStrength(field, "very strong");
        field.clear();
        assertFalse(strengthIndicator(field).isVisible());
        assertTrue(field.isInvalid());
        assertEquals("Password is required.", field.getErrorMessage());
    }

    @Test
    void resetPasswordUsesTheSameInlineValidationAndStrengthFeedback() {
        UserAdministrationView view = descendants(ui).filter(UserAdministrationView.class::isInstance)
                .map(UserAdministrationView.class::cast).findFirst().orElseThrow();
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(view, "openResetPasswordDialog", "staff");
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        Dialog dialog = descendants(ui).filter(Dialog.class::isInstance).map(Dialog.class::cast)
                .findFirst().orElseThrow();
        assertEquals(16, passwordField(dialog).getMaxLength());
        assertEquals("520px", dialog.getWidth());
        assertEquals("calc(100vw - 48px)", dialog.getMaxWidth());
        assertFalse(strengthIndicator(passwordField(dialog)).isVisible());
        button(dialog, "Reset").click();
        PasswordField field = passwordField(dialog);
        assertEquals(16, field.getMaxLength());
        assertTrue(field.isInvalid());
        verify(service, never()).resetPassword(any(), any());
        field.setValue("Cedar9#Sea4!Moon");
        assertFalse(field.isInvalid());
        assertStrength(field, "very strong");
        button(dialog, "Reset").click();
        verify(service).resetPassword("staff", "Cedar9#Sea4!Moon");
        assertFalse(dialog.isOpened());
    }

    @Test
    void strengthRemainsHiddenUntilTypingAndHidesWhenCleared() {
        Dialog dialog = openEmptyCreateDialog();
        PasswordField field = passwordField(dialog);
        assertFalse(strengthIndicator(field).isVisible());
        field.setValue("short");
        assertTrue(strengthIndicator(field).isVisible());
        assertStrength(field, "weak");
        field.clear();
        assertFalse(strengthIndicator(field).isVisible());
        button(dialog, "Save").click();
        assertTrue(field.isInvalid());
        assertFalse(strengthIndicator(field).isVisible());
    }

    @Test
    void editPrefillsEveryAccountFieldAndSubmitsOneAtomicUpdate() {
        UserView account = account(Set.of("OPERATIONS_STAFF"));
        when(service.getUser("staff")).thenReturn(account);
        when(service.updateUser(any())).thenReturn(new UserView("renamed", "Updated Name", null,
                "ACTIVE", Set.of("SYSTEM_ADMIN"), 2L, 1L));
        Dialog dialog = openEditDialog();
        assertEquals("staff", textField(dialog, "Username").getValue());
        assertEquals("Original Name", textField(dialog, "Display Name").getValue());
        assertEquals("staff@example.com", textField(dialog, "Email (Optional)").getValue());
        assertEquals("OPERATIONS_STAFF", roleField(dialog).getValue());
        assertEquals(50, textField(dialog, "Username").getMaxLength());
        assertEquals(100, textField(dialog, "Display Name").getMaxLength());
        assertEquals(200, textField(dialog, "Email (Optional)").getMaxLength());
        assertEquals(0, descendants(dialog).filter(PasswordField.class::isInstance).count());
        textField(dialog, "Username").setValue("renamed");
        textField(dialog, "Display Name").setValue("Updated Name");
        textField(dialog, "Email (Optional)").clear();
        roleField(dialog).setValue("SYSTEM_ADMIN");
        button(dialog, "Save Changes").click();
        verify(service).updateUser(argThat(command -> command.id().equals(2L) && command.version().equals(0L)
                && command.username().equals("renamed") && command.displayName().equals("Updated Name")
                && command.email() == null && command.roles().equals(Set.of("SYSTEM_ADMIN"))));
        assertFalse(dialog.isOpened());
        verify(authentication, never()).logout();
    }

    @Test
    void editRejectionKeepsTheEnteredValuesAndDialogOpen() {
        when(service.getUser("staff")).thenReturn(account(Set.of("OPERATIONS_STAFF")));
        when(service.updateUser(any())).thenThrow(new com.transit.arctransit.common.exception.BusinessConflictException(
                "Username is already taken or reserved by another account."));
        Dialog dialog = openEditDialog();
        textField(dialog, "Username").setValue("another.user");
        button(dialog, "Save Changes").click();
        assertTrue(dialog.isOpened());
        assertEquals("another.user", textField(dialog, "Username").getValue());
        assertTrue(notificationText().contains("reserved"));
    }

    @Test
    void emptyEditFieldsAndClearedRoleDoNotSubmit() {
        when(service.getUser("staff")).thenReturn(account(Set.of("OPERATIONS_STAFF")));
        Dialog dialog = openEditDialog();
        textField(dialog, "Username").clear();
        textField(dialog, "Display Name").clear();
        roleField(dialog).clear();
        button(dialog, "Save Changes").click();
        assertTrue(textField(dialog, "Username").isInvalid());
        assertTrue(textField(dialog, "Display Name").isInvalid());
        assertTrue(roleField(dialog).isInvalid());
        verify(service, never()).updateUser(any());
        assertTrue(dialog.isOpened());
    }

    @Test
    void unchangedRoleSelectionPreservesMultipleExistingRoles() {
        UserView original = account(Set.of("OPERATIONS_STAFF", "SYSTEM_ADMIN"));
        when(service.getUser("staff")).thenReturn(original);
        when(service.updateUser(any())).thenReturn(original);
        Dialog dialog = openEditDialog();
        button(dialog, "Save Changes").click();
        verify(service).updateUser(argThat(command -> command.roles().equals(original.roles())));
    }

    @Test
    void ownAccountRenameSignsOutTheEditingSession() {
        when(service.getUser("staff")).thenReturn(account(Set.of("SYSTEM_ADMIN")));
        when(service.updateUser(any())).thenReturn(new UserView("renamed", "Original Name", "staff@example.com",
                "ACTIVE", Set.of("SYSTEM_ADMIN"), 2L, 1L));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("staff", null,
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"))));
        Dialog dialog = openEditDialog();
        textField(dialog, "Username").setValue("renamed");
        button(dialog, "Save Changes").click();
        verify(authentication).logout();
        assertFalse(dialog.isOpened());
    }

    private static UserView account(Set<String> roles) {
        return new UserView("staff", "Original Name", "staff@example.com", "ACTIVE", roles, 2L, 0L);
    }

    private Dialog openEditDialog() {
        UserAdministrationView view = descendants(ui).filter(UserAdministrationView.class::isInstance)
                .map(UserAdministrationView.class::cast).findFirst().orElseThrow();
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(view, "openEditDialog", "staff");
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        return descendants(ui).filter(Dialog.class::isInstance).map(Dialog.class::cast).findFirst().orElseThrow();
    }

    private static TextField textField(Dialog dialog, String label) {
        return descendants(dialog).filter(TextField.class::isInstance).map(TextField.class::cast)
                .filter(field -> label.equals(field.getLabel())).findFirst().orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private static ComboBox<String> roleField(Dialog dialog) {
        return (ComboBox<String>) descendants(dialog).filter(ComboBox.class::isInstance).findFirst().orElseThrow();
    }

    private static Component strengthIndicator(PasswordField field) {
        return field.getHelperComponent().getChildren()
                .filter(component -> component instanceof Span span
                        && span.getText().startsWith("Password strength:"))
                .findFirst().orElseThrow();
    }

    private static PasswordField passwordField(Dialog dialog) {
        return descendants(dialog).filter(PasswordField.class::isInstance).map(PasswordField.class::cast)
                .findFirst().orElseThrow();
    }

    private static void assertStrength(PasswordField field, String expected) {
        assertTrue(strengthIndicator(field).isVisible());
        assertTrue(field.getHelperComponent().getElement().getTextRecursively()
                .contains("Password strength: " + expected));
    }

    private Dialog openCreateDialog() {
        Dialog dialog = openEmptyCreateDialog();
        descendants(dialog).filter(TextField.class::isInstance).map(TextField.class::cast)
                .filter(field -> "Username".equals(field.getLabel())).findFirst().orElseThrow()
                .setValue("test.staff");
        descendants(dialog).filter(TextField.class::isInstance).map(TextField.class::cast)
                .filter(field -> "Display Name".equals(field.getLabel())).findFirst().orElseThrow()
                .setValue("Test Staff");
        descendants(dialog).filter(PasswordField.class::isInstance).map(PasswordField.class::cast)
                .findFirst().orElseThrow().setValue("usertest123");
        return dialog;
    }

    private Dialog openEmptyCreateDialog() {
        button(ui, "Create Staff Account").click();
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        return descendants(ui).filter(Dialog.class::isInstance).map(Dialog.class::cast)
                .findFirst().orElseThrow();
    }

    private String notificationText() {
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        return descendants(ui).filter(Notification.class::isInstance).map(Component::getElement)
                .map(element -> element.getProperty("text")).findFirst().orElseThrow();
    }

    private static Button button(Component root, String text) {
        Stream<Component> components = descendants(root);
        if (root instanceof Dialog dialog) {
            components = Stream.concat(components, dialog.getFooter().getElement().getChildren()
                    .flatMap(element -> element.getComponent().stream()));
        }
        return components.filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> text.equals(button.getText())).findFirst().orElseThrow();
    }

    private static Stream<Component> descendants(Component root) {
        return Stream.concat(Stream.of(root), root.getChildren().flatMap(UserAdministrationViewTest::descendants));
    }
}
