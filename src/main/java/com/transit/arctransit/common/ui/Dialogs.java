package com.transit.arctransit.common.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;

public final class Dialogs {
    private Dialogs() {
    }

    public static Dialog create() {
        Dialog dialog = new Dialog();
        dialog.setDraggable(true);
        dialog.setResizable(true);
        dialog.setMaxWidth("calc(100vw - 32px)");
        dialog.setMaxHeight("calc(100vh - 32px)");
        return dialog;
    }

    public static Dialog details(String title) {
        Dialog dialog = create();
        dialog.setHeaderTitle(title);
        dialog.setWidth("680px");
        dialog.getFooter().add(new Button("Close", event -> dialog.close()));
        return dialog;
    }

    public static void field(Dialog dialog, String label, Object value) {
        Paragraph paragraph = new Paragraph(label + ": " + (value == null ? "Not recorded" : value));
        paragraph.getStyle().set("white-space", "pre-wrap").set("overflow-wrap", "anywhere");
        dialog.add(paragraph);
    }
}
