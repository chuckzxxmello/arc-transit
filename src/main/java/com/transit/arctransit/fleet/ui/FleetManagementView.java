package com.transit.arctransit.fleet.ui;

import com.transit.arctransit.common.ui.RefreshButton;
import com.transit.arctransit.common.ui.Dialogs;
import com.transit.arctransit.common.ui.FormInputs;

import com.transit.arctransit.fleet.CreateFleetUnitCommand;
import com.transit.arctransit.fleet.ChangeVehicleAvailabilityCommand;
import com.transit.arctransit.fleet.VehicleAvailabilityHistoryView;
import com.vaadin.flow.component.textfield.TextArea;
import java.time.Instant;
import com.transit.arctransit.fleet.FleetManagementService;
import com.transit.arctransit.fleet.FleetUnitQuery;
import com.transit.arctransit.fleet.FleetUnitSummaryView;
import com.transit.arctransit.fleet.FleetUnitView;
import com.transit.arctransit.fleet.UpdateFleetUnitCommand;
import com.transit.arctransit.fleet.UpdateFleetStateCommand;

import com.transit.arctransit.common.exception.BusinessConflictException;
import com.transit.arctransit.common.exception.CommandValidationException;
import com.transit.arctransit.common.exception.ResourceNotFoundException;
import com.transit.arctransit.common.ui.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.transit.arctransit.common.ui.ConfirmationDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.transit.arctransit.common.ui.DataGrids;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;

import jakarta.annotation.security.RolesAllowed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//  fleet Management dashboard

@Route(
        value = "fleet",
        layout = MainLayout.class
)
@PageTitle("Fleet Management")
@RolesAllowed({
        "SYSTEM_ADMIN",
        "OPERATIONS_STAFF"
})
public class FleetManagementView extends VerticalLayout {

    private static final Logger log =
            					LoggerFactory.getLogger(FleetManagementView.class);

    private static final String SYSTEM_ADMIN_ROLE = "SYSTEM_ADMIN";
    private static final String OPERATIONS_STAFF_ROLE = "OPERATIONS_STAFF";

    private final FleetManagementService fleetService;

  
    private final transient AuthenticationContext authenticationContext;

    private final Grid<FleetUnitSummaryView> grid = DataGrids.create();


    private final Dialog detailsDialog = Dialogs.details("Bus Details");


    private String currentSearchFilter = "";

    public FleetManagementView(
				            FleetManagementService fleetService,
				            AuthenticationContext authenticationContext) {

    this.fleetService = fleetService;
    this.authenticationContext = authenticationContext;

																	configureView();
																	configureGrid();

	HorizontalLayout header = createHeader();

	VerticalLayout gridContainer = new VerticalLayout(
							                        header,
							                        grid
	);
															gridContainer.setSizeFull();
															gridContainer.setPadding(false);
															gridContainer.setSpacing(false);

        add(gridContainer);
        expand(gridContainer);
    }

    // fleet dashbboard view
    private void configureView() {
						        setSizeFull();
						        setPadding(false);
						        setSpacing(false);
    }

    // header
    private HorizontalLayout createHeader() {

        HorizontalLayout header = new HorizontalLayout();

										        header.setWidthFull();
										        header.setPadding(true);
										
										        header.setJustifyContentMode(
										        						FlexComponent.JustifyContentMode.BETWEEN
										        );
										
										        header.setDefaultVerticalComponentAlignment(
										        						FlexComponent.Alignment.CENTER
										        );

        H2 title = new H2("Fleet Management");

        								title.getStyle().set("margin", "0");

        TextField searchField = FormInputs.text(new TextField(), 200);

			        searchField.setPlaceholder("Search unit or plate...");
			
			        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
			
			        searchField.setClearButtonVisible(true);
			
			        searchField.setValueChangeMode(ValueChangeMode.LAZY);
			
			        searchField.addValueChangeListener(event -> {
													        currentSearchFilter = normalizeSearchFilter(
													                            					event.getValue()
													        );
													            refreshGrid();
													   });

        Button addButton = new Button(
			                        "Add Bus",
			                        event ->
			                                openCreateDialog()
        );

        						addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout actions = new HorizontalLayout(
							                        searchField,
							                        addButton,
                        new RefreshButton(this::refreshGrid)
                );

		        actions.setDefaultVerticalComponentAlignment(
		                								FlexComponent.Alignment.CENTER
		        );

											        header.add(
											                title,
											                actions
											        );

        return header;
    }

    // grid
    private void configureGrid() {

        grid.setSizeFull();

        grid.addColumn(FleetUnitSummaryView::unitNumber)
											        .setKey("unitNumber")
											        .setHeader("Unit Number")
											        .setSortable(true);

        grid.addColumn(FleetUnitSummaryView::plateNumber)
											        .setKey("plateNumber")
											        .setHeader("Plate Number")
											        .setSortable(true);

        grid.addColumn(FleetUnitSummaryView::capacity)
											        .setKey("capacity")
											        .setHeader("Capacity")
											        .setSortable(true);

        grid.addColumn(FleetUnitSummaryView::administrativeStatus)
											        .setKey("administrativeStatus")
											        .setHeader("Administrative Status")
											        .setSortable(true);

        grid.addColumn(FleetUnitSummaryView::availability).setHeader("Reported Availability");

        grid.setItemsPageable(pageable -> fleetService
							                        .searchUnits(
							                                new FleetUnitQuery(
								                                        currentSearchFilter,
								                                        null
							                                ),
							                                pageable
							                        )
							                        .getContent()
        );

        grid.addComponentColumn(this::createActionButtons)
        											.setHeader("Actions");

        grid.addItemDoubleClickListener(event -> showDetails(event.getItem()));
    }

    private HorizontalLayout createActionButtons(FleetUnitSummaryView unit) {

        Button editButton = new Button(
			                        "Edit",
			                        event -> openEditDialog(unit.id())
        );

        Button statusButton = new Button(
			                        "Status",
			                        event -> openStatusDialog(
				                                        unit.id(),
				                                        unit.operationalStatus()
			                                 )
        );

        HorizontalLayout actions = new HorizontalLayout(
							                        editButton,
							                        statusButton
        );

        if (authenticationContext.hasRole(SYSTEM_ADMIN_ROLE)) {

            Button archiveButton = new Button(
				                            "Archive",
				                            		event -> openArchiveDialog(unit)
            );

            archiveButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
            												actions.add(archiveButton);
        }
        return actions;
    }

    // side panel


    private void showDetails(FleetUnitSummaryView unit) {

        detailsDialog.removeAll();
        Dialogs.field(detailsDialog, "Record ID", unit.id());

        detailsDialog.add(
			                new Paragraph(
			                        "Unit Number: "
			                                + unit.unitNumber()
			                )
        );

        detailsDialog.add(
			                new Paragraph(
			                        "Plate Number: "
			                                + unit.plateNumber()
			                )
        );

        detailsDialog.add(
			                new Paragraph(
			                        "Capacity: "
			                                + unit.capacity()
			                )
        );

        detailsDialog.add(
			                new Paragraph(
			                        "Administrative status: "
                                + unit.administrativeStatus()
			                )
        );

        Dialogs.field(detailsDialog, "Reported availability", unit.availability());
        detailsDialog.open();
    }

    private void closeDetails() {
        detailsDialog.close();
    }

    // create
    private void openCreateDialog() {

        Dialog dialog = Dialogs.create();

        											dialog.setHeaderTitle("Register New Bus");

        TextField unitNumber = FormInputs.text(new TextField("Unit Number"), 30);

        TextField plateNumber = FormInputs.text(new TextField("Plate Number"), 20);

        IntegerField capacity = FormInputs.integer(new IntegerField("Capacity"), 1, 80);

										        unitNumber.setRequiredIndicatorVisible(true);
										        plateNumber.setRequiredIndicatorVisible(true);
										        capacity.setRequiredIndicatorVisible(true);
										
										        capacity.setMin(1);
										        capacity.setMax(
                80
										        );
										        capacity.setValue(40);

        FormLayout form = new FormLayout(
				                        unitNumber,
				                        plateNumber,
				                        capacity
        );

        Button saveButton = new Button("Save",
                        				event -> {
                            if (!FormInputs.validate(dialog)) {
                                return;
                            }


					                            Short value = getCapacity(capacity);
					
					                            if (value == null) {
					                                			return;
					                            }

                            try {
                                fleetService.createUnit(
			                                        new CreateFleetUnitCommand(
						                                                unitNumber.getValue(),
						                                                plateNumber.getValue(),
						                                                				value
			                                        )
                                );

                                dialog.close();
                                refreshGrid();

                                showSuccess("Bus registered successfully.");

                            } catch (RuntimeException exception) {
							                                showError(
							                                        "Register bus",
							                                        			exception
							                                );
                            }
                        }
        );

        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button(
			                        "Cancel",
			                        event -> dialog.close()
        );

							        dialog.getFooter().add(
										                cancelButton,
										                saveButton
							        );
							
							        dialog.add(form);
							        dialog.open();
    }

    // edit dialog
    private void openEditDialog(Long unitId) {

        final FleetUnitView unit;

        try {
            unit = fleetService.getUnit(unitId);

        } catch (RuntimeException exception) {
								            showError("Load fleet unit",
								                    				exception
								            );

            return;
        }

        Dialog dialog = Dialogs.create();

							        dialog.setHeaderTitle(
							                "Edit Bus: "
							                        + unit.unitNumber()
							        );

        TextField unitNumber = FormInputs.text(new TextField("Unit Number"), 30);

											        unitNumber.setValue(
											                unit.unitNumber()
											        );

        TextField plateNumber = FormInputs.text(new TextField("Plate Number"), 20);

											        plateNumber.setValue(
											                unit.plateNumber()
											        );

        IntegerField capacity = FormInputs.integer(new IntegerField("Capacity"), 1, 80);

											        capacity.setMin(1);
											        capacity.setMax(
                80
											        );
											        capacity.setValue(
											                	(int) unit.capacity()
											        );

        FormLayout form = new FormLayout(
				                        unitNumber,
				                        plateNumber,
				                        capacity
        );

        Button saveButton = new Button(
			                        "Save",
			                        event -> {
                            if (!FormInputs.validate(dialog)) {
                                return;
                            }

			
			                            Short value = getCapacity(
			                                            		capacity
			                                    );
			
			                            if (value == null) {
			                                return;
			                            }
			
			                            try {
			                                fleetService.updateUnit(
						                                        new UpdateFleetUnitCommand(
										                                                unitId,
										                                                unitNumber.getValue(),
										                                                plateNumber.getValue(),
										                                                value
										                        )
			                                );
			
			                                dialog.close();
					                                refreshGrid();
					
					                                showSuccess(
					                                        "Bus updated successfully."
					                                );
			
			                            } catch (RuntimeException exception) {
			
					                                showError(
					                                        "Update bus",
					                                        		exception
					                                );
			                            }
			                        }
                );

									        saveButton.addThemeVariants(
									                				ButtonVariant.LUMO_PRIMARY
									        );

        Button cancelButton = new Button(
				                        "Cancel",
				                        event -> dialog.close()
        );

											        dialog.getFooter().add(
											                cancelButton,
											                saveButton
											        );
											
											        dialog.add(form);
											        dialog.open();
    }

    // status dialog
    private void openStatusDialog(Long unitId, String currentStatus) {
        final FleetUnitView unit;
        try {
            unit = fleetService.getUnit(unitId);
        } catch (RuntimeException exception) {
            showError("Load fleet unit", exception);
            return;
        }
        Dialog dialog = Dialogs.create();
        dialog.setHeaderTitle("Bus Status: " + unit.unitNumber());
        dialog.setWidth("560px");
        ComboBox<String> administrativeStatus = FormInputs.choice(new ComboBox<>("Administrative Status"));
        administrativeStatus.setRequiredIndicatorVisible(true);
        administrativeStatus.setItems("ACTIVE", "INACTIVE");
        administrativeStatus.setValue(unit.administrativeStatus());
        ComboBox<String> availability = FormInputs.choice(new ComboBox<>("Reported Availability"));
        availability.setRequiredIndicatorVisible(true);
        availability.setItems("AVAILABLE", "MAINTENANCE", "INSPECTION_HOLD", "OUT_OF_SERVICE", "UNKNOWN");
        availability.setValue(unit.availability());
        TextArea reason = FormInputs.notes(new TextArea("Reason for Availability Change"));
        reason.setWidthFull();
        availability.addValueChangeListener(event -> {
            reason.setRequiredIndicatorVisible(!unit.availability().equals(event.getValue()));
            reason.setInvalid(false);
        });
        Button save = new Button("Save", event -> {
            if (!FormInputs.validate(dialog)) {
                return;
            }
            try {
                fleetService.updateState(new UpdateFleetStateCommand(unitId,
                        administrativeStatus.getValue(), availability.getValue(), reason.getValue()));
                dialog.close();
                closeDetails();
                refreshGrid();
                showSuccess("Administrative status and reported availability saved.");
            } catch (RuntimeException exception) {
                showError("Update bus status", exception);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.add(new VerticalLayout(administrativeStatus, availability, reason));
        dialog.getFooter().add(new Button("History", event -> openAvailabilityHistory(unitId)),
                new Button("Cancel", event -> dialog.close()), save);
        dialog.open();
    }

    private void openAvailabilityHistory(Long unitId) {
        Dialog dialog = Dialogs.create();
        dialog.setHeaderTitle("Vehicle Availability History");
        dialog.setWidth("1000px");
        Grid<VehicleAvailabilityHistoryView> history = DataGrids.create();
        history.addColumn(VehicleAvailabilityHistoryView::previousAvailability).setHeader("Previous");
        history.addColumn(VehicleAvailabilityHistoryView::availability).setHeader("Reported Availability");
        history.addColumn(VehicleAvailabilityHistoryView::effectiveAt).setHeader("Effective Time");
        history.addColumn(VehicleAvailabilityHistoryView::recordedAt).setHeader("Recorded Time");
        history.addColumn(VehicleAvailabilityHistoryView::recordedBy).setHeader("Recorded By");
        history.addColumn(VehicleAvailabilityHistoryView::sourceType).setHeader("Source");
        history.addColumn(VehicleAvailabilityHistoryView::verificationStatus).setHeader("Verification");
        history.addColumn(VehicleAvailabilityHistoryView::reason).setHeader("Reason");
        history.setItemsPageable(pageable -> fleetService.availabilityHistory(unitId, pageable).getContent());
        dialog.add(history);
        dialog.getFooter().add(new Button("Close", event -> dialog.close()));
        dialog.open();
    }

    // archive
    private void openArchiveDialog(FleetUnitSummaryView unit) {

        ConfirmationDialog dialog = new ConfirmationDialog();

							        dialog.setHeader(
							                	"Archive fleet unit?"
							        );
							
							        dialog.setText(
								                "Archive unit "
								                        + unit.unitNumber()
								                        + "? It will be removed from "
								                        + "the active fleet list."
							        );
							
							        dialog.setCancelable(true);
							
							        dialog.setConfirmText(
							                			"Archive Unit"
							        );
							
							        dialog.setConfirmButtonTheme(
							                				"error primary"
							        );
							
							        dialog.addConfirmListener(
										                event -> {
										
										                    try {
										
										                        fleetService.archiveUnit(
										                                				unit.id()
										                        );
										
													                        refreshGrid();
													                        closeDetails();
													
													                        showSuccess(
													                                "Fleet unit archived successfully."
													                        );
										
										                    } catch (RuntimeException exception) {
										
													                        showError(
													                                "Archive fleet unit",
													                                				exception
													                        );
										                    }
										                }
							        );
							        																dialog.open();
    }

    // grid
    private void refreshGrid() {
					        grid.getDataProvider()
					                .refreshAll();
    }

    // validation
    private Short getCapacity(IntegerField field) {

        Integer value = field.getValue();

					        if (value == null || value < 1) {
					
								            showError(
								                    "Validate capacity",
											                    new CommandValidationException(
											                            "Capacity must be greater than zero."
											                    )
								            );
					
					            return null;
					        }
					
					        if (value > 80) {
					
								            showError(
								                    "Validate capacity",
											                    new CommandValidationException(
											                            "Capacity must not exceed 80 passengers."
											                    )
								            );
					
					            return null;
					        }

        return value.shortValue();
    }

    private static String normalizeSearchFilter(String value) {

												        if (value == null) {
												            return "";
												        }

        return value.trim();
    }

    // feedback
    private void showSuccess(String message) {

        Notification.show(message)
				                .addThemeVariants(
				                        NotificationVariant.LUMO_SUCCESS
				                );
    }

    private void showError(
			            String operation,
			            RuntimeException exception) {
        com.transit.arctransit.common.ui.ApplicationErrors.show(exception);
    }

    private void showErrorNotification(String message) {

        Notification.show(message == null || message.isBlank()
						                                ? "The operation could not be completed."
						                                : message
                	)
	                .addThemeVariants(
			                        NotificationVariant.LUMO_ERROR
	                );
    }
}
