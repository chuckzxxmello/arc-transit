package com.transit.arctransit.common.ui;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.shared.Registration;

public class ConfirmationDialog extends Dialog {
    private final Paragraph message = new Paragraph();
    private final Button cancel = new Button("Cancel", event -> close());
    private final Button confirm = new Button("Confirm");

    public ConfirmationDialog() {
        setDraggable(true);
        setResizable(true);
        setWidth("480px");
        setMaxWidth("calc(100vw - 32px)");
        setMaxHeight("calc(100vh - 32px)");
        setCloseOnOutsideClick(false);
        add(message);
        getFooter().add(cancel, confirm);
    }

    public void setHeader(String title) {
        setHeaderTitle(title);
    }

    public void setText(String text) {
        message.setText(text);
    }

    public void setCancelable(boolean value) {
        cancel.setVisible(value);
    }

    public void setConfirmText(String text) {
        confirm.setText(text);
    }

    public void setConfirmButtonTheme(String theme) {
        confirm.getElement().setAttribute("theme", theme);
    }

    public Registration addConfirmListener(ComponentEventListener<ClickEvent<Button>> listener) {
        return confirm.addClickListener(event -> {
            listener.onComponentEvent(event);
            close();
        });
    }
}
