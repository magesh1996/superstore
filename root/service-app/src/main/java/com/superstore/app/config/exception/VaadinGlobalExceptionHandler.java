package com.superstore.app.config.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.superstore.app.utility.Notify;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;

@Component
public class VaadinGlobalExceptionHandler implements VaadinServiceInitListener {

    private static final Logger logger = LoggerFactory.getLogger(VaadinGlobalExceptionHandler.class);

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addSessionInitListener(sessionInitEvent ->
                sessionInitEvent.getSession().setErrorHandler(this::handleError));
    }

    private void handleError(ErrorEvent errorEvent) {
        Throwable throwable = errorEvent.getThrowable();
        Throwable rootCause = findRootCause(throwable);
        logger.error("unhandled Vaadin exception", rootCause);

        try {
            UI ui = UI.getCurrent();
            if (ui != null) {
                ui.access(() -> {
                    if (rootCause instanceof Exception exception) {
                        Notify.error(exception);
                    } else {
                        Notify.error(new RuntimeException(rootCause));
                    }
                });
            } else {
                logger.warn("no current UI available to display error notification");
            }
        } catch (Exception ex) {
            logger.error("failed to display Vaadin notification for exception", ex);
        }
    }

    private Throwable findRootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}