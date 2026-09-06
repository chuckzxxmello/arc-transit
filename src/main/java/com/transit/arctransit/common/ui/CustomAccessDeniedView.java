package com.transit.arctransit.common.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AccessDeniedException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.ParentLayout;

import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletResponse;

// common access denied view

@ParentLayout(MainLayout.class)
@PageTitle("Access Denied — Arc Transit")
@PermitAll
public class CustomAccessDeniedView
        extends VerticalLayout
        implements HasErrorParameter<AccessDeniedException> {

    public CustomAccessDeniedView() {

        configureLayout();

        H1 title =
                new H1("Access Denied");

        Paragraph message =
                new Paragraph(
                        "You do not have permission "
                                + "to access this page."
                );

        message.getStyle()
                .set("max-width", "500px")
                .set("text-align", "center");

        Button backButton =
                new Button(
                        "Go Back to Dashboard",
                        VaadinIcon.HOME.create(),
                        event ->
                                UI.getCurrent()
                                        .navigate("")
                );

        backButton.addThemeVariants(
                ButtonVariant.LUMO_PRIMARY
        );

        add(
                title,
                message,
                backButton
        );
    }

    // configure page layout

    private void configureLayout() {

        setSizeFull();

        setJustifyContentMode(
                FlexComponent.JustifyContentMode.CENTER
        );

        setDefaultHorizontalComponentAlignment(
                FlexComponent.Alignment.CENTER
        );
    }

    // return HTTP 403

    @Override
    public int setErrorParameter(
            BeforeEnterEvent event,
            ErrorParameter<AccessDeniedException> parameter) {

        return HttpServletResponse.SC_FORBIDDEN;
    }
}