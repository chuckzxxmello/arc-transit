package com.transit.arctransit.common.ui;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.renderer.ComponentRenderer;

public final class DataGrids {
    private DataGrids() {
    }

    public static <T> Grid<T> create() {
        return configure(new Grid<>());
    }

    public static <T> Grid<T> create(Class<T> type, boolean autoCreateColumns) {
        return configure(new Grid<>(type, autoCreateColumns));
    }

    private static <T> Grid<T> configure(Grid<T> grid) {
        grid.setWidthFull();
        grid.getStyle().set("min-width", "0");
        grid.addAttachListener(event -> grid.getColumns().forEach(column -> {
            column.setResizable(true);
            column.setFlexGrow(0);
            if (column.getRenderer() instanceof ComponentRenderer) {
                column.setAutoWidth(true);
            } else if (column.getWidth() == null || column.getWidth().isBlank()) {
                column.setWidth("180px");
            }
        }));
        return grid;
    }
}
