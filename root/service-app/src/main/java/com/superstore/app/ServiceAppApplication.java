package com.superstore.app;

import org.springframework.boot.SpringApplication;
// import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.session.autoconfigure.SessionAutoConfiguration;
import org.springframework.boot.session.data.redis.autoconfigure.SessionDataRedisAutoConfiguration;

// import com.superstore.app.config.log.BeanLoggingInitializer;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.lumo.Lumo;

// @SpringBootApplication
@SpringBootApplication(exclude = { 
    SessionAutoConfiguration.class, 
    SessionDataRedisAutoConfiguration.class
})
// @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
// @ComponentScan(basePackages = {"com.superstore.payment", "com.superstore.notification"})
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet(Lumo.COMPACT_STYLESHEET)
@StyleSheet(Lumo.UTILITY_STYLESHEET)
@StyleSheet("styles.css")
@Push
@PWA(
    name = "superstore",
    shortName = "superstore",
    iconPath = "images/superstore.png"
)
public class ServiceAppApplication implements AppShellConfigurator{
	public static void main(String[] args) {
		SpringApplication.run(ServiceAppApplication.class, args);
        
        // register initializer here 
        // [OR] application.properties context.initializer.classes=com.superstore.app.config.log.BeanLoggingInitializer
        // SpringApplication app = new SpringApplication(ServiceAppApplication.class);
        // app.addInitializers(new BeanLoggingInitializer());
        // app.run(args);
	}

	@Override
    public void configurePage(AppShellSettings settings) {
        // default tab name.
        settings.setPageTitle("superstore");

        // default tab logo (favicon).
        // settings.addFavIcon("icon", "images/favicon.ico", "192x192");
        
        // standard shortcut icon for bookmarks/older browsers.
        // settings.addLink("shortcut icon", "images/favicon.ico");
    }
}