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

// public login view for Arc Transit staff users

@Route(value = "login", autoLayout = false)
@PageTitle("Login to Arc Transit System")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm loginForm = new LoginForm();

    public LoginView() {
		        configureLayout();
		        configureLoginForm();
		
		        add(
		        		new H1("Arc Transit"),
		                loginForm
		        );
    }

    private void configureLayout() {

        setSizeFull();

        setAlignItems(
                FlexComponent.Alignment.CENTER
        );

        setJustifyContentMode(
                FlexComponent.JustifyContentMode.CENTER
        );
    }

    private void configureLoginForm() {

        loginForm.setAction("login");
        loginForm.setForgotPasswordButtonVisible(false);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {

        boolean authenticationFailed = event.getLocation()
							                .getQueryParameters()
							                .getParameters()
							                .containsKey("error");

        loginForm.setError(authenticationFailed);
    }
}