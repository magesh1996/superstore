package com.superstore.app.view;

// import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import com.superstore.app.client.SecurityClient;
import com.superstore.app.config.security.User;
import com.superstore.app.config.security.UserService;
import com.superstore.app.dto.AuthLoginRequest;
import com.superstore.app.dto.AuthLoginResponse;
import com.superstore.app.layout.AuthLayout;
import com.superstore.app.utility.Notify;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

@Route(value = "auth", layout = AuthLayout.class)
@AnonymousAllowed
// @PageTitle("auth")
public class AuthView extends VerticalLayout {

    Binder<User> binder = new Binder<>(User.class);

    private final UserService userService;
    // private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    private final SecurityClient securityClient;

    private boolean isLoginMode = true;

    private final Span brandSpan = new Span("superstore");
    private final H4 title = new H4(" ");
    private final TextField usernameField = new TextField("username");
    // private final EmailField emailField = new EmailField("email");
    private final TextField mobileField = new TextField("mobile");
    private final PasswordField passwordField = new PasswordField("password");
    private final PasswordField confirmPasswordField = new PasswordField("confirm password");
    private final Button submitButton = new Button(" ");
    private final Span toggleLabel = new Span(" ");
    private final Button toggleButton = new Button(" ");

    @SuppressWarnings("null")
    public AuthView(
        UserService userService, 
        // AuthenticationManager authenticationManager
        SecurityClient securityClient) {
        
        this.userService = userService;
        // this.authenticationManager = authenticationManager;
        this.securityClient = securityClient;

        setSizeFull();
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        setAlignItems(FlexComponent.Alignment.CENTER);
        // getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        addClassNames(LumoUtility.Background.CONTRAST_5);

        // CARD UI
        VerticalLayout card = new VerticalLayout();
        // card.setWidth("350px"); // banish px and embrace fluid design for better responsiveness.
        card.setMaxWidth("21.875rem"); // 1 rem = 16 px so, 21.875 rem = 350 px.
        card.setPadding(true);
        card.setSpacing(true);
        card.addClassNames(
            LumoUtility.Background.BASE,
            LumoUtility.BorderRadius.MEDIUM,
            LumoUtility.BoxShadow.MEDIUM,
            LumoUtility.Padding.XLARGE
        );

        // SETTING COMPONENT SIZES AND STYLES
        brandSpan.setWidthFull();
        brandSpan.addClassNames(
            LumoUtility.TextAlignment.CENTER,
            LumoUtility.FontSize.XXXLARGE,
            LumoUtility.FontWeight.BOLD,
            LumoUtility.TextColor.PRIMARY
        );
        brandSpan.addClassName("superstore");
        title.setWidthFull();
        title.addClassNames(LumoUtility.TextAlignment.CENTER);
        title.addClassName("googlesansflex");
        usernameField.setWidthFull();
        mobileField.setWidthFull();
        mobileField.setAllowedCharPattern("[0-9]");
        mobileField.setMaxLength(10);
        passwordField.setWidthFull();
        confirmPasswordField.setWidthFull();
        submitButton.setWidthFull();
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submitButton.getElement().getStyle().set("cursor", "pointer");
        submitButton.addClickShortcut(Key.ENTER).allowBrowserDefault();

        binder.forField(usernameField).bind(User::getUsername, User::setUsername);
        binder.forField(mobileField)
            .asRequired("mobile number is required")
            .withValidator(
                value -> value.matches("\\d{10}"),
                "must be exactly 10 digits"
            )
            .bind(User::getMobile, User::setMobile);
        binder.forField(passwordField).bind(User::getPassword, User::setPassword);

        // TOGGLE STYLES
        toggleButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        toggleButton.getElement().getStyle().set("cursor", "pointer");
        VerticalLayout toggleLayout = new VerticalLayout(new Span(toggleLabel, toggleButton));
        toggleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        toggleLayout.setPadding(false);
        toggleLayout.setSpacing(false);

        // ADD TO CARD
        card.add(brandSpan, title, usernameField, mobileField, passwordField, confirmPasswordField, submitButton, toggleLayout);
        add(card);

        // INIT MODE
        updateUI();

        // EVENT LISTENER
        toggleButton.addClickListener(e -> {
            isLoginMode = !isLoginMode;
            updateUI();
        });

        submitButton.addClickListener(e -> handleSubmit());
    }

    private void updateUI() {
        if (isLoginMode) {            
            title.setText("welcome back!");
            mobileField.setVisible(false);
            confirmPasswordField.setVisible(false);
            submitButton.setText("login");
            toggleLabel.setText("don't have an account? ");
            toggleButton.setText("register");
        } 
        else {
            title.setText("register now!");
            mobileField.setVisible(true);
            confirmPasswordField.setVisible(true);
            submitButton.setText("signup");
            toggleLabel.setText("already have an account? ");
            toggleButton.setText("login");
        }
    }

    private void handleSubmit() {
        
        // String username = usernameField.getValue();
        // String password = passwordField.getValue();
        // replaced with Binder.

        User user = new User();
        try {
            binder.writeBean(user);
        }
        catch (ValidationException ex) {
            //System.getLogger(AuthView.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            Notify.error("fix errors");
            Notify.error(ex);
            return;
        }

        if (user.getUsername() == null || user.getUsername().isBlank() || 
            user.getPassword() == null || user.getPassword().isBlank()) {
            // Notification.show("please fill in all required fields", 3000, Notification.Position.TOP_CENTER);
            Notify.info("please fill in all required fields");
            return;
        }

        if (isLoginMode) {
            try {
                // Authentication authentication = authenticationManager.authenticate(
                //     new UsernamePasswordAuthenticationToken(
                //         user.getUsername(),
                //         user.getPassword()
                //         //,user.getAuthorities()
                //     )
                // );

                AuthLoginResponse response = securityClient.login(new AuthLoginRequest(user.getUsername(), user.getPassword()));

                String jwt = response.accessToken();
                VaadinSession.getCurrent().setAttribute("jwtToken", jwt);

                // build an ALREADY-authenticated token by service-security
                // so we don't call authenticationManager.authenticate() again.
                var authorities = java.util.List.<org.springframework.security.core.GrantedAuthority>of(
                    new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER") // derive real roles from response/JWT claims if available                    
                );

                Authentication authentication = new UsernamePasswordAuthenticationToken(
                    user.getUsername(), 
                    jwt,
                    authorities
                ); // 3-arg constructor marks isAuthenticated() = true automatically

                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);

                var currentRequest = VaadinServletRequest.getCurrent().getHttpServletRequest();
                var currentResponse = VaadinServletResponse.getCurrent().getHttpServletResponse();
                securityContextRepository.saveContext(context, currentRequest, currentResponse);

                // if (VaadinSession.getCurrent() != null)
                //     VaadinSession.getCurrent().setAttribute("rawPassword", user.getPassword());

                // System.out.println("authentication.isAuthenticated() : ["+authentication.isAuthenticated()+"]");
                // System.out.println("authentication.getAuthorities() : ["+authentication.getAuthorities()+"]");
                // System.out.println("authentication.getPrincipal() : ["+authentication.getPrincipal()+"]");
                // System.out.println("SecurityContextHolder.getContext().getAuthentication() : ["+SecurityContextHolder.getContext().getAuthentication()+"]");

                // navigate to dashboard or primary view.
                getUI().ifPresent(ui -> ui.navigate(DashboardView.class));

                // Notify.info("welcome back, " + authentication.getName() + "!");
                Notify.info("welcome back, " + user.getUsername() + "!");
            }
            catch (UsernameNotFoundException | BadCredentialsException e) {
                String message;
                if (e instanceof UsernameNotFoundException) message = "invalid username";
                else message = "invalid password";
                Notify.error(message);
            }
            catch (RuntimeException ex) {
                Notify.error(ex);
            }
        }
        else {
            String confirmPassword = confirmPasswordField.getValue();

            if (user.getMobile() == null || user.getMobile().isBlank() || 
                user.getPassword() == null || user.getPassword().isBlank()) {
                // Notification.show("please fill in all fields", 3000, Notification.Position.TOP_CENTER);
                Notify.info("please fill in all fields");
                return;
            }
            if (!user.getPassword().equals(confirmPassword)) {
                // Notification.show("passwords do not match!", 3000, Notification.Position.TOP_CENTER);
                Notify.error("passwords do not match!");
                return;
            }

            userService.register(user.getUsername(), user.getMobile(), user.getPassword());
            Notify.success("account created for " + user.getUsername() + "!");
            
            // clear form and switch back to login after successful registration.
            // binder.setBean(null);
            binder.readBean(new User());
            confirmPasswordField.clear(); // no binder.forField(confirmPasswordField).
            isLoginMode = true;
            updateUI();
        }
    }
}