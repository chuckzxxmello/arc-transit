package com.transit.arctransit.common.ui;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.springframework.stereotype.Component;

@Component
public class ApplicationErrorHandler implements VaadinServiceInitListener {
    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addSessionInitListener(session -> session.getSession().setErrorHandler(error -> {
            Throwable cause = error.getThrowable();
            while ((cause instanceof java.lang.reflect.InvocationTargetException
                    || cause instanceof java.util.concurrent.CompletionException)
                    && cause.getCause() != null) {
                cause = cause.getCause();
            }
            if (com.vaadin.flow.component.UI.getCurrent() != null) {
                ApplicationErrors.show(cause);
            } else {
                ApplicationErrors.message(cause);
            }
        }));
    }
}
