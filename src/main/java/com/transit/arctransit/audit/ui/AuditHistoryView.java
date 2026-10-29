package com.transit.arctransit.audit.ui;

import com.transit.arctransit.common.ui.RefreshButton;
import com.vaadin.flow.component.dialog.Dialog;
import com.transit.arctransit.common.ui.Dialogs;
import com.transit.arctransit.common.ui.FormInputs;

import com.transit.arctransit.audit.AuditEventView;
import com.transit.arctransit.audit.AuditQuery;
import com.transit.arctransit.audit.AuditQueryService;
import com.transit.arctransit.common.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.transit.arctransit.common.ui.DataGrids;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Route(value = "audit", layout = MainLayout.class)
@PageTitle("Audit Log")
@RolesAllowed("SYSTEM_ADMIN")
public class AuditHistoryView extends VerticalLayout {

    public AuditHistoryView(AuditQueryService service) {
        setSizeFull();
        TextField actor = FormInputs.text(new TextField("Actor"), 100);
        TextField action = FormInputs.text(new TextField("Action"), 100);
        TextField entity = FormInputs.text(new TextField("Entity type"), 100);
        ComboBox<String> outcome = FormInputs.choice(new ComboBox<>("Outcome"));
        outcome.setItems("SUCCESS", "FAILURE", "DENIED");
        outcome.setClearButtonVisible(true);
        Grid<AuditEventView> grid = DataGrids.create();
        grid.addItemDoubleClickListener(event -> openDetails(event.getItem()));
        grid.addColumn(AuditEventView::timestamp).setHeader("Time");
        grid.addColumn(AuditEventView::performedBy).setHeader("Actor");
        grid.addColumn(AuditEventView::actionName).setHeader("Action");
        grid.addColumn(AuditEventView::entityName).setHeader("Entity");
        grid.addColumn(AuditEventView::entityId).setHeader("Record");
        grid.addColumn(AuditEventView::outcome).setHeader("Outcome");
        grid.addColumn(AuditEventView::details).setHeader("Details");
        Runnable search = () -> grid.setItemsPageable(pageable -> service.searchAudit(
                new AuditQuery(actor.getValue(), action.getValue(), entity.getValue(),
                        null, outcome.getValue(), null, null),
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                        Sort.by(Sort.Direction.DESC, "timestamp", "id"))).getContent());
        HorizontalLayout filters = new HorizontalLayout(actor, action, entity, outcome,
                new Button("Search", event -> search.run()), new RefreshButton(search));
        filters.setAlignItems(Alignment.END);
        add(new H2("Audit Log"), filters, grid);
        expand(grid);
        search.run();
    }

    private void openDetails(AuditEventView audit) {
        Dialog dialog = Dialogs.details("Audit Record Details");
        Dialogs.field(dialog, "Audit ID", audit.id());
        Dialogs.field(dialog, "Time", audit.timestamp());
        Dialogs.field(dialog, "Actor", audit.performedBy());
        Dialogs.field(dialog, "Action", audit.actionName());
        Dialogs.field(dialog, "Entity type", audit.entityName());
        Dialogs.field(dialog, "Record ID", audit.entityId());
        Dialogs.field(dialog, "Outcome", audit.outcome());
        Dialogs.field(dialog, "Details", audit.details());
        dialog.open();
    }
}
