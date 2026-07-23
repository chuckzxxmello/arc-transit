package com.transit.arctransit.analytics.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import com.vaadin.flow.router.RouteAlias;

import jakarta.annotation.security.PermitAll;

@Route("")
@RouteAlias("dashboard")
@PageTitle("Arc Transit Systems")
@PermitAll
public class DashboardView extends VerticalLayout {

    public DashboardView() {
        add(new H1("This is a test for arc transit system vaadin dashboard"));
    }

}
