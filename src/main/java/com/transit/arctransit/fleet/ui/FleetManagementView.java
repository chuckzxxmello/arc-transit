package com.transit.arctransit.fleet.ui;

import com.transit.arctransit.fleet.CreateFleetUnitCommand;
import com.transit.arctransit.fleet.FleetManagementService;
import com.transit.arctransit.fleet.FleetUnitQuery;
import com.transit.arctransit.fleet.FleetUnitSummaryView;
import com.transit.arctransit.fleet.FleetUnitView;
import com.transit.arctransit.fleet.UpdateFleetUnitCommand;

import com.transit.arctransit.common.exception.BusinessConflictException;
import com.transit.arctransit.common.exception.CommandValidationException;
import com.transit.arctransit.common.exception.ResourceNotFoundException;
import com.transit.arctransit.common.ui.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
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
import com.vaadin.flow.component.splitlayout.SplitLayout;
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

    private static final java.util.List<String>
								            OPERATIONAL_STATUSES = java.util.List.of(
															                    "ACTIVE",
															                    "INACTIVE",
															                    "UNDER_MAINTENANCE",
															                    "OUT_OF_SERVICE"
								            );

    private final FleetManagementService fleetService;

  
    private final transient AuthenticationContext authenticationContext;

    private final Grid<FleetUnitSummaryView> grid = new Grid<>();

    private final SplitLayout splitLayout = new SplitLayout();

    private final VerticalLayout sidePanel = new VerticalLayout();

    private final Div sidePanelContent = new Div();

    private String currentSearchFilter = "";

    public FleetManagementView(
				            FleetManagementService fleetService,
				            AuthenticationContext authenticationContext) {

    this.fleetService = fleetService;
    this.authenticationContext = authenticationContext;

																	configureView();
																	configureSidePanel();
																	configureGrid();

	HorizontalLayout header = createHeader();

	VerticalLayout gridContainer = new VerticalLayout(
							                        header,
							                        grid
	);
															gridContainer.setSizeFull();
															gridContainer.setPadding(false);
															gridContainer.setSpacing(false);

        splitLayout.addToPrimary(gridContainer);

        splitLayout.addToSecondary(sidePanel);

        splitLayout.setSplitterPosition(100);
        splitLayout.setSizeFull();

        add(splitLayout);

        expand(splitLayout);
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

        TextField searchField = new TextField();

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
							                        addButton
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

        grid.addColumn(FleetUnitSummaryView::operationalStatus)
											        .setKey("operationalStatus")
											        .setHeader("Status")
											        .setSortable(true);

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

        grid.addSelectionListener(event ->
                						event.getFirstSelectedItem()
                									.ifPresent(this::showDetails)
        );
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
    private void configureSidePanel() {

        Button closeButton = new Button(
			                        VaadinIcon.CLOSE.create(),
			                        event -> hideSidePanel()
        );

			        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
			
			        HorizontalLayout header = new HorizontalLayout(
										                        new H3("Bus Details"),
										                        				closeButton
			        );

        header.setWidthFull();

        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        header.setDefaultVerticalComponentAlignment(
                								FlexComponent.Alignment.CENTER
        );

									        sidePanel.add(
										                header,
										                sidePanelContent
									        );
									
									        sidePanel.setVisible(false);
									        sidePanel.setWidth("350px");
									
									        sidePanel.getStyle()
											                .set(
											                        "background-color",
											                        "#ffffff"
											                )
											                .set(
											                        "border-left",
											                        "1px solid #ddd"
											                );
    }

    private void showDetails(FleetUnitSummaryView unit) {

        sidePanelContent.removeAll();

        sidePanelContent.add(
			                new Paragraph(
			                        "Unit Number: "
			                                + unit.unitNumber()
			                )
        );

        sidePanelContent.add(
			                new Paragraph(
			                        "Plate Number: "
			                                + unit.plateNumber()
			                )
        );

        sidePanelContent.add(
			                new Paragraph(
			                        "Capacity: "
			                                + unit.capacity()
			                )
        );

        sidePanelContent.add(
			                new Paragraph(
			                        "Status: "
			                                + unit.operationalStatus()
			                )
        );

        sidePanel.setVisible(true);

        splitLayout.setSplitterPosition(75);
    }

    private void hideSidePanel() {

				        sidePanel.setVisible(false);
				
				        splitLayout.setSplitterPosition(100);
    }

    // create
    private void openCreateDialog() {

        					Dialog dialog = new Dialog();

        											dialog.setHeaderTitle("Register New Bus");

        TextField unitNumber = new TextField("Unit Number");

        TextField plateNumber = new TextField("Plate Number");

        IntegerField capacity = new IntegerField("Capacity");

										        unitNumber.setRequiredIndicatorVisible(true);
										        plateNumber.setRequiredIndicatorVisible(true);
										        capacity.setRequiredIndicatorVisible(true);
										
										        capacity.setMin(1);
										        capacity.setMax(
										                	(int) Short.MAX_VALUE
										        );
										        capacity.setValue(40);

        FormLayout form = new FormLayout(
				                        unitNumber,
				                        plateNumber,
				                        capacity
        );

        Button saveButton = new Button("Save",
                        				event -> {

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

        Dialog dialog = new Dialog();

							        dialog.setHeaderTitle(
							                "Edit Bus: "
							                        + unit.unitNumber()
							        );

        TextField unitNumber = new TextField("Unit Number");

											        unitNumber.setValue(
											                unit.unitNumber()
											        );

        TextField plateNumber = new TextField("Plate Number");

											        plateNumber.setValue(
											                unit.plateNumber()
											        );

        IntegerField capacity = new IntegerField("Capacity");

											        capacity.setMin(1);
											        capacity.setMax(
											                	(int) Short.MAX_VALUE
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
    private void openStatusDialog(
					            Long unitId,
					            String currentStatus) {

        Dialog dialog = new Dialog();

							        dialog.setHeaderTitle(
							                			"Change Operational Status"
							        );

        ComboBox<String> statusCombo = new ComboBox<>("New Status");

										        statusCombo.setItems(
										                OPERATIONAL_STATUSES
										        );
										
										        statusCombo.setValue(
										                currentStatus
										        );
										
										        statusCombo.setRequiredIndicatorVisible(
										                true
										        );

										        					dialog.add(statusCombo);

        Button saveButton = new Button(
			                       "Update Status",
			                       event -> {

				                   String status = statusCombo.getValue();
				
				                            if (status == null || status.isBlank()) {
				
					                                showError(
					                                        "Update status",
								                                        new CommandValidationException(
								                                                "Operational status must be selected."
								                                        )
					                                );
				
				                                return;
				                            }

		                            try {
		
		                                fleetService.changeStatus(
						                                        unitId,
						                                        status
		                                );
		
								                                dialog.close();
								                                refreshGrid();
		
					                                showSuccess(
					                                        "Status updated successfully."
					                                );
		
		                            } catch (RuntimeException exception) {
		
					                                showError(
					                                        "Update status",
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
												
												        dialog.add(
												                statusCombo
												        );
												
												        dialog.getFooter().add(
															                cancelButton,
															                saveButton
												        );
												
												        dialog.open();
    }

    // archive
    private void openArchiveDialog(FleetUnitSummaryView unit) {

        ConfirmDialog dialog = new ConfirmDialog();

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
													                        hideSidePanel();
													
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
					
					        if (value > Short.MAX_VALUE) {
					
								            showError(
								                    "Validate capacity",
											                    new CommandValidationException(
											                            "Capacity exceeds the supported limit."
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


        if (exception instanceof BusinessConflictException
				                || exception instanceof CommandValidationException
				                || exception instanceof ResourceNotFoundException) {

								            log.warn(
								                    "{} failed: {}",
								                    operation,
								                    exception.getMessage()
								            );
								
												            showErrorNotification(
												                    exception.getMessage()
												            );

            return;
        }

		        log.error(
		                "{} failed unexpectedly.",
								                operation,
								                exception
		        );
		
		        showErrorNotification(
		                "The operation could not be completed. "
		                								+ "Please try again."
		        );
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