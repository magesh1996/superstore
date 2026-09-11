package com.superstore.app.utility;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

public class Notify {

    // prevents instantiation.
    private Notify() {}

    private static void show(String message, NotificationVariant variant) {
        Notification notification = new Notification(message, 5000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(variant);
        notification.open();
    }

    public static void info(String message) {
        Notify.show(message, NotificationVariant.INFO);
    }

    public static void success(String message) {
        Notify.show(message, NotificationVariant.LUMO_SUCCESS);
    }

    public static void error(String message) {
        Notify.show(message, NotificationVariant.LUMO_ERROR);
    }

    public static void error(Exception e) {
        String exceptionClassName = e.getClass().getName().toLowerCase();
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

        // SQL exceptions.
        if (exceptionClassName.contains("sql") || 
            exceptionClassName.contains("dataintegrity") || 
            message.contains("sql") || 
            message.contains("constraint")) {
            Notify.show("database operation failed", NotificationVariant.LUMO_ERROR);
        } 
        // Connection/Timeout exceptions.
        else if (exceptionClassName.contains("connect") || 
                 exceptionClassName.contains("timeout") || 
                 message.contains("connection refused")) {
            Notify.show("service is down", NotificationVariant.LUMO_ERROR);
        } 
        // unhandled fallback.
        else {
            Notify.show("error occurred : " + message, NotificationVariant.LUMO_ERROR);
        }
    }
}