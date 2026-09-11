package com.superstore.app.view;

import java.math.BigDecimal;
import java.time.LocalDate;
// import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// import com.superstore.app.facade.OrderFacade;
// import com.superstore.app.facade.ProductFacade;
import com.superstore.app.layout.MainLayout;
import com.superstore.app.pojo.ProductForecastResult;
import com.superstore.app.service.XGBoostForecastService;
import com.superstore.app.utility.Notify;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;

import jakarta.annotation.security.PermitAll;

@Route(value = "", layout = MainLayout.class)
// @PageTitle("dashboard")
@Menu(order = 0, title = "dashboard")
@PermitAll
// @AnonymousAllowed
public class DashboardView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(DashboardView.class);

    // private final ProductFacade productFacade;
    // private final OrderFacade orderFacade;
    private final XGBoostForecastService forecastService;

    private String tabSessionKey;
    private FlexLayout cardLayout;

    // component references for dynamic rendering onAttach
    private Span revenueValue;
    private Span salesValue;
    private Span inventoryValue;

    // XGBoost Forecasting Component References
    private final Grid<ProductForecastResult> forecastGrid = new Grid<>(ProductForecastResult.class);

    public DashboardView(
        // ProductFacade productFacade, OrderFacade orderFacade
        XGBoostForecastService forecastService
    ) {
        // this.productFacade = productFacade;
        // this.orderFacade = orderFacade;
        this.forecastService = forecastService;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        // fetch unique window/tab identifier from Vaadin to prevent tab cross-talk.
        attachEvent.getUI().getPage().executeJs("return window.name;")
            .then(String.class, windowName -> {
                if (windowName == null || windowName.isEmpty()) {
                    String newWindowId = UUID.randomUUID().toString();
                    attachEvent.getUI().getPage().executeJs("window.name = '" + newWindowId + "';");
                    windowName = newWindowId;
                }
                
                this.tabSessionKey = "DASHBOARD_METRICS_" + windowName;
                
                // load asynchronous backend data and pull state.
                refreshDashboardData();
            });

        removeAll(); // VERY IMPORTANT: prevents layout doubling on re-attach.
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // responsive grid wrap container using FlexLayout.
        cardLayout = new FlexLayout();
        cardLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        // cardLayout.setGap("20px"); // ERROR
        // cardLayout.getStyle().set("gap", "20px");
        cardLayout.addClassNames(LumoUtility.Gap.LARGE);
        cardLayout.setWidthFull();

        // initialize empty containers with placeholders; data binds strictly on dynamic execution.
        Div revenueCard = createCardPlaceholder("today's revenue", "#d7e9f7", "#286bd1"); // #286bd1 // #fff5cc // #eeeeee
        revenueValue = (Span) revenueCard.getComponentAt(1);

        Div salesCard = createCardPlaceholder("today's order count", "#d7e9f7", "#286bd1");
        salesValue = (Span) salesCard.getComponentAt(1);

        Div inventoryCard = createCardPlaceholder("total product sold", "#d7e9f7", "#286bd1");
        inventoryValue = (Span) inventoryCard.getComponentAt(1);

        cardLayout.add(revenueCard, salesCard, inventoryCard);
        
        // XGBoost forecast components setup.
        // H2 forecastHeader = new H2("⚡demand forecast (XGBoost)");

        forecastGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_COLUMN_BORDERS);
        
        forecastGrid.setColumns("productId", "productName", "currentStock", "predictedDemand30Days", "recommendedRestock");
        
        forecastGrid.getColumnByKey("productId").setHeader("company");
        forecastGrid.getColumnByKey("productName").setHeader("product");
        forecastGrid.getColumnByKey("currentStock").setHeader("available stock");
        forecastGrid.getColumnByKey("predictedDemand30Days").setHeader("forecast demand (30 DAYS)");
        forecastGrid.getColumnByKey("recommendedRestock").setHeader("recommended restock");

        forecastGrid.addComponentColumn(item -> {
            Span badge = new Span(item.statusAlert());
            if ("CRITICAL".equals(item.statusAlert())) {
                badge.getElement().getThemeList().add("badge error primary");
            } else if ("REORDER".equals(item.statusAlert())) {
                badge.getElement().getThemeList().add("badge warning");
            } else {
                badge.getElement().getThemeList().add("badge success");
            }
            badge.addClassNames(
                LumoUtility.Padding.Horizontal.SMALL,
                LumoUtility.Padding.Vertical.SMALL,
                LumoUtility.BorderRadius.SMALL,
                LumoUtility.FontWeight.MEDIUM,
                LumoUtility.FontSize.MEDIUM
            );
            return badge;
        })
        .setHeader("stock status")
        .setSortable(true) // turns header interaction ON for this specific custom column.
        .setComparator((item1, item2) -> item1.statusAlert().compareToIgnoreCase(item2.statusAlert()));

        Button runForecastBtn = new Button("run demand forecast", e -> runForecastEngine());
        runForecastBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        runForecastBtn.addClassNames(
            LumoUtility.Padding.Vertical.MEDIUM, 
            LumoUtility.Padding.Horizontal.MEDIUM
        );
        runForecastBtn.getElement().getStyle().set("cursor", "pointer");

        add(cardLayout, runForecastBtn, forecastGrid);

        // addClickListener(e -> {
        //     throw new RuntimeException("verify global exception handler");
        // });
    }

    private Div createCardPlaceholder(String title, String bgColor, String textColor) {
        Div card = new Div();
        card.setWidth("280px");
        card.getStyle()
            .set("background-color", bgColor)
            .set("padding", "20px")
            .set("border-radius", "12px")
            .set("box-shadow", "0 4px 6px -1px rgba(0,0,0,0.1)");

        Span titleLabel = new Span(title);
        titleLabel.getStyle()
            .set("font-size", "18px")
            .set("font-weight", "500")
            .set("color", "#6b7280") // #6b7280
            .set("display", "block")
            .set("margin-bottom", "8px");
        titleLabel.addClassName("dashboard_card_title");

        Span valueLabel = new Span("loading...");
        valueLabel.getStyle()
            .set("font-size", "28px")
            .set("font-weight", "bold")
            // .set("font-family", "monospace")
            .set("color", textColor);
        valueLabel.addClassName("dashboard_card_value");

        card.add(titleLabel, valueLabel);
        return card;
    }

    //interrogates facades safely, caches state inside the session instance, and mutates strings.
    @SuppressWarnings("unchecked")
    private void refreshDashboardData() {
        try {
            BigDecimal todayRevenue = BigDecimal.ZERO;
            long todaySalesCount = 0L;
            long totalProducts = 0L;

            // attempt session retrieval fallback first.
            Map<String, Object> cachedMetrics = (Map<String, Object>) VaadinSession.getCurrent().getAttribute(tabSessionKey);
            
            if (cachedMetrics != null) {
                logger.info("restoring dashboard metric state from VaadinSession context cache wrapper.");
                todayRevenue = (BigDecimal) cachedMetrics.getOrDefault("REVENUE", BigDecimal.ZERO);
                todaySalesCount = (long) cachedMetrics.getOrDefault("SALES_COUNT", 0L);
                totalProducts = (long) cachedMetrics.getOrDefault("PRODUCT_COUNT", 0L);
            } else {
                logger.info("executing active pipeline retrieval for store telemetry.");
                // todayRevenue = orderFacade.calculateRevenueSince(LocalDate.now()); 
                // todaySalesCount = orderFacade.countOrdersSince(LocalDate.now());
                // totalProducts = productFacade.countTotalProducts();
                todayRevenue = new BigDecimal("10000.00");
                todaySalesCount = 200L;
                totalProducts = 150L;

                // save parameters locally to session map state.
                Map<String, Object> metricMap = new HashMap<>();
                metricMap.put("REVENUE", todayRevenue);
                metricMap.put("SALES_COUNT", todaySalesCount);
                metricMap.put("PRODUCT_COUNT", totalProducts);
                VaadinSession.getCurrent().setAttribute(tabSessionKey, metricMap);
            }

            // render numeric metrics gracefully to UI contexts.
            revenueValue.setText("₹ " + todayRevenue.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
            salesValue.setText(String.valueOf(todaySalesCount));
            inventoryValue.setText(String.valueOf(totalProducts));

        } catch (Exception ex) {
            logger.error("failed to compile layout metric computations natively : ", ex);
            Notify.error("error loading dashboard metrics : " + ex.getMessage());
            
            revenueValue.setText("₹ 0.00");
            salesValue.setText("0");
            inventoryValue.setText("0");
        }
    }

    private record SampleProduct(String id, String name, int currentStock, float avgDailySales, float reorderPoint, float leadTime) {}

    private void runForecastEngine() {
        long startTime = System.currentTimeMillis();
        int currentMonth = LocalDate.now().getMonthValue();

        // sample products (replace with repository call : productRepository.findAll())
        List<SampleProduct> products = List.of(
            new SampleProduct("P-101", "Logitech MX Master 3S", 12, 4.5f, 20, 7),
            new SampleProduct("P-102", "Dell UltraSharp 27 Monitor", 45, 1.2f, 15, 5),
            new SampleProduct("P-103", "Ergonomic Standing Desk", 4, 8.0f, 25, 14),
            new SampleProduct("P-104", "Microsoft Surface Pro 7", 200, 2.0f, 10, 3)
        );

        List<ProductForecastResult> results = products.stream().map(p -> {
            // run prediction via ONNX
            float predicted = forecastService.predict30DayDemand(
                p.currentStock(), p.avgDailySales(), p.reorderPoint(), p.leadTime(), currentMonth
            );
            
            int predictedDemand = Math.round(predicted);
            int restockQty = Math.max(0, predictedDemand - p.currentStock());

            String status = "OPTIMAL";
            if (p.currentStock() < (predictedDemand * 0.3)) {
                status = "CRITICAL";
            } else if (restockQty > 0) {
                status = "REORDER";
            }

            return new ProductForecastResult(
                p.id(), p.name(), p.currentStock(), predictedDemand, restockQty, status
            );
        }).toList();

        forecastGrid.setItems(results);

        long executionTime = System.currentTimeMillis() - startTime;
        Notify.success("forecast calculated for " + results.size() + " products in " + executionTime + " ms!");
    }
}