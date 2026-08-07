package com.transit.arctransit.common.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.ParentLayout;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Custom error view rendered when an invalid URL route is accessed.
 */
@ParentLayout(MainLayout.class)
@PageTitle("Invalid Page — Arc Transit")
@PermitAll
public class CustomNotFoundView extends VerticalLayout implements HasErrorParameter<NotFoundException> {

    public CustomNotFoundView() {
        setSizeFull();
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);

        H1 title = new H1("Invalid Page");
        title.getStyle().set("color", "#1F497D").set("margin-bottom", "0.5rem");

        Paragraph message = new Paragraph("The page you are looking for does not exist or has been moved.");
        message.getStyle().set("color", "#555").set("font-size", "1.1rem");

        Button backButton = new Button("Go Back to Dashboard", VaadinIcon.HOME.create(), click -> {
            UI.getCurrent().navigate("");
        });
        backButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        backButton.getStyle().set("margin-top", "1.5rem");

        add(title, message, backButton);
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<NotFoundException> parameter) {
        return HttpServletResponse.SC_NOT_FOUND;
    }
}
