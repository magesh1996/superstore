package com.superstore.app.layout;

import com.vaadin.flow.component.ReconnectDialogConfiguration;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@AnonymousAllowed
public class AuthLayout extends VerticalLayout implements RouterLayout {
    public AuthLayout() {
        setSizeFull();

        ReconnectDialogConfiguration configuration = UI.getCurrent().getReconnectDialogConfiguration();
        configuration.setDialogText("connection lost, reconnecting...");
        configuration.setDialogTextGaveUp("connection lost, server down.");
        configuration.setReconnectAttempts(3);
    }
}