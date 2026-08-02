package com.transit.arctransit.analytics.ui;

import com.transit.arctransit.analytics.application.IncidentService;
import com.transit.arctransit.analytics.domain.Incident;
import com.transit.arctransit.common.ui.MainLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@Route(value = "incidents", layout = MainLayout.class)
@PageTitle("Incidents")
@PermitAll
public class IncidentView extends VerticalLayout {

    public IncidentView(IncidentService incidentService) {
        add(new H2("Incidents (Dummy Version)"));
        setSizeFull();

        Grid<Incident> grid = new Grid<>(Incident.class, false);
        grid.addColumn(Incident::getTitle).setHeader("Title");
        grid.addColumn(Incident::getSeverity).setHeader("Severity");
        grid.addColumn(Incident::getStatus).setHeader("Status");
        grid.addColumn(Incident::getReportedAt).setHeader("Reported At");

        grid.setItems(incidentService.getRecentIncidents());
        add(grid);
    }
}
