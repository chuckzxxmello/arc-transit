package com.transit.arctransit.auth.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Public login page for Arc Transit staff accounts.
 *
 * The form submits the username and password to Spring Security's
 * standard POST /login authentication endpoint.
 */
@Route(value = "login", autoLayout = false)
@PageTitle("Login | Arc Transit System")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm loginForm = new LoginForm();

    public LoginView() {
        configureLayout();
        configureLoginForm();

        add(
                new H1("Arc Transit Systems"),
                loginForm);
    }

    private void configureLayout() {
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
    }

    private void configureLoginForm() {
        /*
         * Spring Security listens to POST /login.
         *
         * LoginForm automatically sends the entered username
         * and password to this endpoint.
         */
        loginForm.setAction("login");

        loginForm.setForgotPasswordButtonVisible(false);
    }

    /**
     * Spring Security redirects failed logins to:
     *
     * /login?error
     *
     * When that parameter exists, Vaadin displays its generic
     * invalid-credentials message.
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean authenticationFailed = event
                .getLocation()
                .getQueryParameters()
                .getParameters()
                .containsKey("error");

        loginForm.setError(authenticationFailed);
    }
}