package com.transit.arctransit.common.ui;

import com.transit.arctransit.auth.UserAdministrationService;
import com.transit.arctransit.auth.ui.UserAdministrationView;
import com.transit.arctransit.dispatch.DispatchService;
import com.transit.arctransit.dispatch.ui.DispatchView;
import com.transit.arctransit.driver.DriverManagementService;
import com.transit.arctransit.driver.ui.DriverManagementView;
import com.transit.arctransit.fleet.FleetManagementService;
import com.transit.arctransit.fleet.ui.FleetManagementView;
import com.transit.arctransit.incident.IncidentManagementService;
import com.transit.arctransit.incident.ui.IncidentManagementView;
import com.transit.arctransit.maintenance.MaintenanceService;
import com.transit.arctransit.maintenance.ui.MaintenanceManagementView;
import com.transit.arctransit.reporting.OperationalReportService;
import com.transit.arctransit.reporting.ui.OperationalReportingView;
import com.transit.arctransit.route.RouteManagementService;
import com.transit.arctransit.route.ui.RouteManagementView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.security.AuthenticationContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ModuleFormValidationTest {
    private UI ui;
    private Object service;

    @BeforeEach
    void setup() {
        VaadinSession session = mock(VaadinSession.class, RETURNS_DEEP_STUBS);
        when(session.hasLock()).thenReturn(true);
        VaadinSession.setCurrent(session);
        ui = new UI();
        ui.getInternals().setSession(session);
        UI.setCurrent(ui);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "form-test", null, List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"))));
    }

    @AfterEach
    void cleanup() {
        UI.setCurrent(null);
        VaadinSession.setCurrent(null);
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @CsvSource({"fleet,Add Bus,Save", "driver,Register Driver,Save", "route,Create Route,Save",
            "dispatch,Create Assignment,Create Assignment", "incident,Report Incident,Report",
            "maintenance,Request Maintenance,Request", "reporting,Record Report,Record",
            "auth,Create Staff Account,Save"})
    void blankSubmissionHighlightsFieldsAndDoesNotCallMutation(String module, String open, String submit) {
        ui.add(view(module));
        button(ui, open).click();
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        Dialog dialog = descendants(ui).filter(Dialog.class::isInstance).map(Dialog.class::cast)
                .findFirst().orElseThrow();
        clearInvocations(service);
        button(dialog, submit).click();
        assertTrue(dialog.isOpened());
        assertTrue(descendants(dialog).anyMatch(component ->
                component instanceof TextField field && field.isInvalid()
                        || component instanceof TextArea area && area.isInvalid()
                        || component instanceof ComboBox<?> choice && choice.isInvalid()
                        || component instanceof com.vaadin.flow.component.textfield.IntegerField number && number.isInvalid()));
        verifyNoInteractions(service);
        descendants(dialog).filter(TextField.class::isInstance).map(TextField.class::cast)
                .forEach(field -> assertTrue(field.getMaxLength() > 0 && field.getMaxLength() <= 500));
        descendants(dialog).filter(TextArea.class::isInstance).map(TextArea.class::cast)
                .forEach(field -> assertEquals(500, field.getMaxLength()));
        descendants(dialog).filter(ComboBox.class::isInstance).map(ComboBox.class::cast)
                .forEach(field -> assertFalse(field.isAllowCustomValue()));
    }

    private Component view(String module) {
        AuthenticationContext authentication = mock(AuthenticationContext.class);
        when(authentication.hasRole(anyString())).thenReturn(true);
        return switch (module) {
            case "fleet" -> new FleetManagementView(service(FleetManagementService.class), authentication);
            case "driver" -> new DriverManagementView(service(DriverManagementService.class));
            case "route" -> new RouteManagementView(service(RouteManagementService.class), authentication);
            case "dispatch" -> new DispatchView(service(DispatchService.class), stub(FleetManagementService.class),
                    stub(DriverManagementService.class), stub(RouteManagementService.class));
            case "incident" -> new IncidentManagementView(service(IncidentManagementService.class), stub(FleetManagementService.class));
            case "maintenance" -> new MaintenanceManagementView(service(MaintenanceService.class), stub(FleetManagementService.class));
            case "reporting" -> new OperationalReportingView(service(OperationalReportService.class), stub(FleetManagementService.class), stub(DriverManagementService.class), stub(RouteManagementService.class), stub(DispatchService.class));
            case "auth" -> new UserAdministrationView(service(UserAdministrationService.class), authentication);
            default -> throw new IllegalArgumentException(module);
        };
    }

    private <T> T service(Class<T> type) {
        T mock = stub(type);
        service = mock;
        return mock;
    }

    private static <T> T stub(Class<T> type) {
        return mock(type, invocation -> invocation.getMethod().getReturnType() == Page.class
                ? Page.empty() : RETURNS_DEFAULTS.answer(invocation));
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
        return Stream.concat(Stream.of(root), root.getChildren().flatMap(ModuleFormValidationTest::descendants));
    }
}
