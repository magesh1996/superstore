package com.superstore.app.layout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ReconnectDialogConfiguration;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.ScrollerVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoIcon;
import com.vaadin.flow.theme.lumo.LumoUtility;

import jakarta.annotation.security.PermitAll;

@Layout
@PermitAll
//@AnonymousAllowed
public final class MainLayout extends AppLayout implements AfterNavigationObserver {

    private final transient AuthenticationContext authenticationContext;
    private HorizontalLayout breadcrumbLabel;
    private Button toggleButton;

    public MainLayout(AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;
        setPrimarySection(Section.NAVBAR);
        addToNavbar(createTopHeaderLayout());
        addToDrawer(createSideNavLayout());

        ReconnectDialogConfiguration configuration = UI.getCurrent().getReconnectDialogConfiguration();
        configuration.setDialogText("connection lost, reconnecting...");
        configuration.setDialogTextGaveUp("connection lost, server down.");
        configuration.setReconnectAttempts(3);
    }

    // LAYOUT 1 : Top Header with dynamic breadcrumb and toggle button.
    
    private Component createTopHeaderLayout() {
        // var toggle = new DrawerToggle();
        // using a standard button instead of DrawerToggle for custom icon switching control.
        toggleButton = new Button(VaadinIcon.MENU.create());
        toggleButton.getElement().getStyle().set("cursor", "pointer");
        toggleButton.addClassNames(
            LumoUtility.Background.TRANSPARENT,
            LumoUtility.TextColor.PRIMARY
        );

        toggleButton.addClickListener(e -> setDrawerOpened(!isDrawerOpened()));
        
        breadcrumbLabel = new HorizontalLayout();
        breadcrumbLabel.setPadding(false);
        breadcrumbLabel.setSpacing(true);

        // TOP LEFT SIDE (toggle + breadcrumb)
        var topLeftWrapper = new HorizontalLayout(toggleButton, breadcrumbLabel);
        topLeftWrapper.setAlignItems(FlexComponent.Alignment.CENTER);
        topLeftWrapper.setSpacing(true);

        // TOP RIGHT SIDE (logout)
        Button logoutButton = new Button("logout",VaadinIcon.SIGN_OUT.create(), click -> {
            // natively logout, clears JSESSIONID, and routes to /auth safely.
            this.authenticationContext.logout();
        });
        logoutButton.getElement().getStyle().set("cursor", "pointer");
        logoutButton.addThemeVariants(ButtonVariant.ERROR);
        //logoutButton.setTooltipText("logout");
        logoutButton.addClassNames(
            LumoUtility.Margin.Right.MEDIUM
        );

        // TOP HEADER LAYOUT
        var topHeaderLayout = new HorizontalLayout(topLeftWrapper, logoutButton);
        topHeaderLayout.setWidthFull();
        topHeaderLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        // sets topLeftWrapper to far left and logoutButton to far right.
        topHeaderLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        topHeaderLayout.addClassNames(
            LumoUtility.Border.BOTTOM,
            LumoUtility.Background.BASE
        );
        
        return topHeaderLayout;
    }

    // LAYOUT 2 : Side Navigation with header branding, nav links, and footer.

    private Component createSideNavLayout() {
        var sideNavLayout = new VerticalLayout();
        sideNavLayout.setHeight("100%");
        sideNavLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        
        // SideNav BRANDING
        var brandingSpan = new Span("menu");
        brandingSpan.addClassNames(
            LumoUtility.FontSize.XLARGE,
            LumoUtility.FontWeight.MEDIUM,
            LumoUtility.TextColor.PRIMARY,
            LumoUtility.Padding.MEDIUM
        );

        // SideNav CLOSE BUTTON for Drawer Layout.
        var closeDrawerButton = new Button(LumoIcon.CROSS.create());
        closeDrawerButton.getElement().getStyle().set("cursor", "pointer");
        closeDrawerButton.addClassNames(
            LumoUtility.Background.TRANSPARENT,
            LumoUtility.TextColor.ERROR
        );
        closeDrawerButton.addClickListener(e -> setDrawerOpened(false));

        // BRANDING + CLOSE BUTTON.
        var drawerHeader = new HorizontalLayout(brandingSpan, closeDrawerButton);
        drawerHeader.setWidthFull();
        drawerHeader.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        drawerHeader.setAlignItems(FlexComponent.Alignment.CENTER);
        drawerHeader.addClassNames(
            LumoUtility.Padding.Horizontal.MEDIUM,
            LumoUtility.Padding.Vertical.SMALL
        );

        // nav links container
        var navScroller = new Scroller(createSideNavLinks());
        navScroller.setWidthFull();
        navScroller.getStyle().set("min-height", "0");
        navScroller.addThemeVariants(ScrollerVariant.OVERFLOW_INDICATORS);

        var footer = createApplicationFooter();

        sideNavLayout.add(drawerHeader, navScroller, footer);
        sideNavLayout.setFlexGrow(1, navScroller);

        return sideNavLayout;
    }

    private SideNav createSideNavLinks() {
        var nav = new SideNav();
        nav.setWidthFull();
        nav.addClassNames(
            LumoUtility.MinWidth.NONE,
            LumoUtility.MaxWidth.FULL,
            LumoUtility.BoxSizing.BORDER,
            LumoUtility.Padding.Horizontal.MEDIUM,
            LumoUtility.Padding.Vertical.MEDIUM,
            LumoUtility.Gap.MEDIUM,
            LumoUtility.Overflow.HIDDEN
        );

        MenuConfiguration.getMenuEntries().forEach(entry -> nav.addItem(createSideNavItem(entry)));
        return nav;
    }

    // MENU : dynamically creates SideNavItems based on the MenuEntry configuration.

    private SideNavItem createSideNavItem(MenuEntry menuEntry) {
        SideNavItem item;
        
        if(menuEntry.icon() != null)
        {
            Component icon = menuEntry.icon().contains(".svg")
                ? new SvgIcon(menuEntry.icon())
                : new Icon(menuEntry.icon());
            item = new SideNavItem(menuEntry.title(), menuEntry.path(), icon);
        }
        else
        {
            item = new SideNavItem(menuEntry.title(), menuEntry.path());
        }

        item.addClassNames(
            LumoUtility.Padding.Vertical.NONE,
            LumoUtility.LineHeight.MEDIUM // ensures cut off text (half letter 'y') is handled properly.
        );

        return item;
    }

    // SideNav FOOTER

    private Component createApplicationFooter() {
        Span footerLine1 = new Span("built on Java ☕︎");
        footerLine1.addClassNames(
            LumoUtility.FontSize.MEDIUM,
            LumoUtility.FontWeight.MEDIUM,
            LumoUtility.TextColor.PRIMARY
        );
        footerLine1.addClassName("footer");

        Span footerLine2 = new Span("(Spring Boot + Vaadin)");
        footerLine2.addClassNames(
            LumoUtility.FontSize.SMALL,
            LumoUtility.TextColor.SECONDARY
        );
        
        VerticalLayout footer = new VerticalLayout(footerLine1, footerLine2);
        footer.setWidthFull();
        footer.setAlignItems(FlexComponent.Alignment.CENTER);
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        footer.addClassNames(
            LumoUtility.Border.TOP,
            LumoUtility.Background.BASE,
            LumoUtility.Padding.MEDIUM
        );

        /* footer.getStyle()
            .set("font-size", "1.5em")
            .set("font-weight", "500")
            .set("color", "var(--lumo-secondary-text-color)"); */
        // inline styles take precedence over CSS classes in Vaadin.
        // cleanest approach in modern Vaadin is to ditch the .getStyle() inline styling entirely and use LumoUtility class.
                
        return footer;
    }

    // DYNAMIC PATH in header breadcrumb.
    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        Class<?> activeView = event.getActiveChain().get(0).getClass();
        
        String title;
        
        // if(activeView.isAnnotationPresent(PageTitle.class))
        //     title = activeView.getAnnotation(PageTitle.class).value();
        if(activeView.isAnnotationPresent(Menu.class))
            title = activeView.getAnnotation(Menu.class).title();
        else
            title = activeView.getSimpleName().replace("View", "");
        
        breadcrumbLabel.removeAll();
        
        Span prefixSpan = new Span("superstore");
        prefixSpan.addClassNames(
            LumoUtility.FontSize.XXLARGE,
            LumoUtility.FontWeight.BOLD,
            LumoUtility.TextColor.PRIMARY
        );
        prefixSpan.addClassName("superstore");
        // prefixSpan.getStyle().set("font-style", "italic");

        Span titleSpan = new Span("> " + title);
        titleSpan.addClassNames(
            LumoUtility.FontSize.LARGE,
            LumoUtility.FontWeight.MEDIUM,
            LumoUtility.TextColor.SECONDARY
        );
        titleSpan.addClassName("breadcrumb");

        breadcrumbLabel.add(prefixSpan, titleSpan);
        breadcrumbLabel.setAlignItems(FlexComponent.Alignment.CENTER);

        // closes the drawer automatically after navigating to a view.
        setDrawerOpened(false);
    }
}