package com.superstore.app.view;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.superstore.app.facade.ProductFacade;
import com.superstore.app.pojo.ProductPojo;
import com.superstore.app.utility.Notify;
import com.superstore.app.view.filter.ProductFilter;
import com.superstore.app.websocket.BarcodeWebSocketHandler;
import com.superstore.app.websocket.QrCodeService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoIcon;
import com.vaadin.flow.theme.lumo.LumoUtility;

import jakarta.annotation.security.PermitAll;

@Route("product")
// @PageTitle("product")
@Menu(order = 1, title = "product")
@PermitAll
public class ProductView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(ProductView.class);

    private final ProductFacade productFacade;
    private final QrCodeService qrCodeService;
    private final BarcodeWebSocketHandler barcodeWebSocketHandler;

    private final String sessionId = UUID.randomUUID().toString();
    private UI registeredUi;

    // track rows currently being edited simultaneously.
    private final Set<ProductPojo> editingRows = new HashSet<>();

    // these are re-created on every onAttach, so non-final.
    private DatePicker pkdDate;
    private DatePicker expDate;    
    private Button newProductBtn;
    private FormLayout formLayout;
    private HorizontalLayout buttonLayout;    
    private Binder<ProductPojo> formBinder;
    private Grid<ProductPojo> productGrid;
    // private Editor<ProductPojo> productGridEditor;
    private GridListDataView<ProductPojo> productGridDataView;
    private ProductFilter productFilter;
    
    // HEADER LEVEL ACTIONS (always visible, state controlled dynamically)
    private Button saveAllBtn;
    private Button clearAllBtn;
    private Button deleteAllBtn;

    // FORM COMPONENTS - re-created on every onAttach.
    private TextField company;
    private TextField sku;
    private TextField name;
    private TextField department;
    private TextField category;
    private TextField upc;
    private TextField barcode;
    private BigDecimalField mrp;
    private BigDecimalField costPrice;
    private BigDecimalField sellPrice;
    private NumberField stock;
    private NumberField threshold;
    private TextField hsnCode;
    private BigDecimalField taxRate;
    private BigDecimalField cessRate;
    private Checkbox taxInclusive;
    private Checkbox taxExempted;
    private Checkbox active;

    public ProductView(ProductFacade productFacade, QrCodeService qrCodeService, BarcodeWebSocketHandler barcodeWebSocketHandler) {
        this.productFacade = productFacade;
        this.qrCodeService = qrCodeService;
        this.barcodeWebSocketHandler = barcodeWebSocketHandler;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        
        removeAll();
        setSizeFull();

        // FIX : GLOBAL inject a style block for select-all header and inner checkboxes.
        attachEvent.getUI().getPage().executeJs(
            "const id = 'vaadin-checkbox-pointer-style';" +
            "if (!document.getElementById(id)) {" +
            "  const style = document.createElement('style');" +
            "  style.id = id;" +
            "  style.textContent = `\n" +
            "    vaadin-checkbox,\n" +
            "    vaadin-checkbox::part(checkbox),\n" +
            "    vaadin-checkbox::part(label),\n" +
            "    vaadin-grid-flow-selection-column,\n" +
            "    vaadin-grid-flow-selection-column::part(checkbox) {\n" +
            "      cursor: pointer !important;\n" +
            "    }\n" +
            "  `;" +
            "  document.head.appendChild(style);" +
            "}"
        );

        // DATE PICKER
        DatePicker.DatePickerI18n customFormat = new DatePicker.DatePickerI18n();
        customFormat.setDateFormats("dd/MM/yyyy",
            "d", "dd",
            "dM", "dMM", "ddM", "ddMM",
            "dMyy", "dMMyy", "ddMyy", "ddMMyy",
            "dMyyyy", "dMMyyyy", "ddMyyyy", "ddMMyyyy",
            "d-M-yyyy", "d-MM-yyyy", "dd-M-yyyy", "dd-MM-yyyy",
            "d/M/yyyy", "d/MM/yyyy", "dd/M/yyyy", "dd/MM/yyyy");

        pkdDate = createCustomDatePicker("pkd date", customFormat);
        expDate = createCustomDatePicker("exp date", customFormat);

        // FORM COMPONENTS
        company         = new TextField("company");
        sku             = new TextField("sku");
        name            = new TextField("name");
        department      = new TextField("department");
        category        = new TextField("category");
        upc             = new TextField("upc");
        barcode         = new TextField("barcode");
        mrp             = new BigDecimalField("mrp");
        costPrice       = new BigDecimalField("cost price");
        sellPrice       = new BigDecimalField("sell price");
        stock           = new NumberField("stock");
        threshold       = new NumberField("threshold");
        hsnCode         = new TextField("hsn code");
        taxRate         = new BigDecimalField("tax rate (%)");
        cessRate        = new BigDecimalField("cess rate (%)");
        taxInclusive    = new Checkbox("tax inclusive");
        taxExempted     = new Checkbox("tax exempted");
        active          = new Checkbox("active");

        // FORM BINDER
        formBinder = new Binder<>(ProductPojo.class);
        setupFormBinder();

        // FORM LAYOUT
        formLayout = new FormLayout();

        mrp.setWidthFull();
        mrp.getElement().getStyle().set("min-width", "0");

        costPrice.setWidthFull();
        costPrice.getElement().getStyle().set("min-width", "0");

        sellPrice.setWidthFull();
        sellPrice.getElement().getStyle().set("min-width", "0");

        HorizontalLayout groupPrice = new HorizontalLayout(mrp, costPrice, sellPrice);
        //groupPrice.setWidthFull();
        groupPrice.setPadding(false);
        groupPrice.setSpacing(true);
        groupPrice.setFlexGrow(1, mrp);
        groupPrice.setFlexGrow(1, costPrice);
        groupPrice.setFlexGrow(1, sellPrice);
        groupPrice.getElement().getStyle().set("overflow", "visible");

        stock.setWidthFull();
        stock.getElement().getStyle().set("min-width", "0");

        threshold.setWidthFull();
        threshold.getElement().getStyle().set("min-width", "0");

        HorizontalLayout groupInventory = new HorizontalLayout(stock, threshold);
        groupInventory.setWidthFull();
        groupInventory.setPadding(false);
        groupInventory.setSpacing(true);
        groupInventory.setFlexGrow(1, stock);
        groupInventory.setFlexGrow(1, threshold);
        groupInventory.getElement().getStyle().set("overflow", "visible");

        taxRate.setWidthFull();
        taxRate.getElement().getStyle().set("min-width", "0");

        cessRate.setWidthFull();
        cessRate.getElement().getStyle().set("min-width", "0");

        HorizontalLayout groupRate = new HorizontalLayout(taxRate, cessRate);
        groupRate.setWidthFull();
        groupRate.setPadding(false);
        groupRate.setSpacing(true);
        groupRate.setFlexGrow(1, taxRate);
        groupRate.setFlexGrow(1, cessRate);
        groupRate.getElement().getStyle().set("overflow", "visible");

        taxInclusive.setWidthFull();
        taxInclusive.getElement().getStyle().set("min-width", "0");
        // added in onAttach globally.
        // taxInclusive.getElement().getStyle().set("cursor", "pointer");

        taxExempted.setWidthFull();
        taxExempted.getElement().getStyle().set("min-width", "0");
        taxExempted.getElement().getStyle().set("cursor", "pointer");

        active.setWidthFull();
        active.getElement().getStyle().set("min-width", "0");
        active.getElement().getStyle().set("cursor", "pointer");

        HorizontalLayout groupCheckBox = new HorizontalLayout(taxInclusive, taxExempted, active);
        groupCheckBox.setWidthFull();
        groupCheckBox.setPadding(false);
        groupCheckBox.setSpacing(true);
        groupCheckBox.setFlexGrow(1, taxInclusive);
        groupCheckBox.setFlexGrow(1, taxExempted);
        groupCheckBox.setFlexGrow(1, active);
        groupCheckBox.getElement().getStyle().set("overflow", "visible");
        
        formLayout.add(
            company, sku, 
            name, department, category, 
            upc, barcode, 
            // mrp, costPrice, sellPrice, 
            groupPrice, 
            // stock, threshold, 
            groupInventory, 
            pkdDate, expDate, 
            hsnCode, 
            // taxRate, cessRate, 
            groupRate, 
            // taxInclusive, taxExempted, active
            groupCheckBox
        );

        formLayout.setColspan(groupPrice, 2);
        formLayout.setColspan(groupInventory, 1);
        formLayout.setColspan(groupRate, 1);
        formLayout.setColspan(groupCheckBox, 2);

        formLayout.setWidthFull();
        formLayout.setResponsiveSteps(
            new ResponsiveStep("0", 2),
            new ResponsiveStep("500px", 4),
            new ResponsiveStep("800px", 6));
        formLayout.setVisible(false);

        // NEW PRODUCT BUTTON
        newProductBtn = new Button("new product", VaadinIcon.PLUS.create());
        newProductBtn.getElement().getStyle().set("cursor", "pointer");
        newProductBtn.addClickListener(e -> toggleNewProduct());

        // BUTTON LAYOUT
        Button submitButton = new Button("submit", e -> handleSubmit());
        submitButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        submitButton.getElement().getStyle().set("cursor", "pointer");
        Button deleteButton = new Button("delete", e -> handleDelete());
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteButton.getElement().getStyle().set("cursor", "pointer");
        Button resetButton = new Button("reset", e -> {
            formBinder.readBean(new ProductPojo());
            productGrid.deselectAll();
        });
        resetButton.getElement().getStyle().set("cursor", "pointer");
        Button refreshButton = new Button("refresh", e -> {
            refreshGrid();
            formBinder.readBean(new ProductPojo());
            productGrid.deselectAll();
        });
        refreshButton.getElement().getStyle().set("cursor", "pointer");

        buttonLayout = new HorizontalLayout(submitButton, deleteButton, resetButton, refreshButton);
        buttonLayout.setWidthFull();
        buttonLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        buttonLayout.setVisible(false);

        // HEADER ACTIONS (always visible, disabled initially)
        saveAllBtn = new Button(LumoIcon.CHECKMARK.create());
        saveAllBtn.getElement().getStyle().set("cursor", "pointer");
        saveAllBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);
        saveAllBtn.setEnabled(false);

        deleteAllBtn = new Button(VaadinIcon.TRASH.create());
        deleteAllBtn.getElement().getStyle().set("cursor", "pointer");
        deleteAllBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
        deleteAllBtn.setEnabled(false);

        clearAllBtn = new Button(LumoIcon.CROSS.create());
        clearAllBtn.getElement().getStyle().set("cursor", "pointer");
        clearAllBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
        clearAllBtn.setVisible(false);
        clearAllBtn.addClickListener(e -> {
            editingRows.clear();
            // refreshGrid();
            productGrid.getListDataView().refreshAll();
            updateHeaderButtonStates();
        });

        // GRID
        productGrid = new Grid<>(ProductPojo.class, false);
        productGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_COLUMN_BORDERS);
        productGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        productGrid.getElement().executeJs(
        "this.querySelector('vaadin-grid-flow-selection-column').frozen = true;"
        );

        // when a checkbox is checked, clear edit modes and update button states.
        productGrid.addSelectionListener(e -> {
            if (!e.getAllSelectedItems().isEmpty()) {
                if (!editingRows.isEmpty()) {
                    editingRows.clear();
                    productGrid.getListDataView().refreshAll();
                }
            }
            updateHeaderButtonStates();
        });

        setupGridBinder();

        add(newProductBtn, formLayout, buttonLayout, productGrid);

        // WEBSOCKET
        barcodeWebSocketHandler.unregisterConsumer(sessionId);
        connectWebSocket(barcode);

        // LOAD DATA
        refreshGrid();
    }

    private void toggleNewProduct() {
        boolean isVisible = !formLayout.isVisible();
        formLayout.setVisible(isVisible);
        buttonLayout.setVisible(isVisible);
        if (isVisible) {
            newProductBtn.setIcon(LumoIcon.CROSS.create());
            newProductBtn.addClassName(LumoUtility.TextColor.ERROR);
        } else {
            newProductBtn.setIcon(VaadinIcon.PLUS.create());
            newProductBtn.removeClassName(LumoUtility.TextColor.ERROR);
        }
    }

    private DatePicker createCustomDatePicker(String label, DatePicker.DatePickerI18n i18nConfig) {
        DatePicker picker = new DatePicker(label) {
            @Override
            protected void onAttach(AttachEvent attachEvent) {
                super.onAttach(attachEvent);
                this.getElement().executeJs("this.inputElement.maxLength = 10;");
                this.getElement().getStyle().set("cursor", "pointer");
            }
        };
        picker.setI18n(i18nConfig);
        picker.setPlaceholder("DD/MM/YYYY");
        picker.setClearButtonVisible(true);
        return picker;
    }

    private void setupCurrencyField(BigDecimalField field) {
        field.setClearButtonVisible(true);
        Div prefix = new Div();
        prefix.setText("₹");
        field.setPrefixComponent(prefix);
    }

    private Component createSearchHeader(String title, String placeholder, Consumer<String> filterChangeConsumer) {

        // NORMAL HEADER
        Span label = new Span(title);
        label.addClassNames(LumoUtility.FontWeight.MEDIUM, LumoUtility.TextColor.PRIMARY);
        label.getElement().getStyle().set("cursor", "pointer");

        Button searchIconBtn = new Button(VaadinIcon.SEARCH.create());
        searchIconBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        searchIconBtn.getElement().getStyle().set("cursor", "pointer");

        HorizontalLayout labelLayout = new HorizontalLayout(label, searchIconBtn);
        labelLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        labelLayout.setSpacing(true);
        labelLayout.setPadding(false);
        labelLayout.setWidthFull();
        labelLayout.addClassNames(LumoUtility.Height.MEDIUM);

        // SEARCH HEADER
        TextField textField = new TextField();
        textField.setPlaceholder(placeholder);
        textField.setValueChangeMode(ValueChangeMode.EAGER);
        textField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        textField.setWidthFull();
        textField.addClassNames(LumoUtility.Flex.SHRINK); // allow textField to shrink below its default minimum size.
        textField.addValueChangeListener(e -> filterChangeConsumer.accept(e.getValue()));

        Button closeIconBtn = new Button(LumoIcon.CROSS.create());
        closeIconBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        closeIconBtn.getElement().getStyle().set("cursor", "pointer");
        closeIconBtn.addClassNames(LumoUtility.Flex.SHRINK_NONE); // prevent the close button from shrinking when space is tight.

        textField.setSuffixComponent(closeIconBtn);

        HorizontalLayout searchLayout = new HorizontalLayout(textField);
        searchLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        searchLayout.setSpacing(false); // reduce or remove spacing to save premium pixel real estate.
        searchLayout.setPadding(false);
        searchLayout.setWidthFull();
        searchLayout.setVisible(false); // initially hidden.
        searchLayout.addClassNames(LumoUtility.Height.MEDIUM);

        // label TOGGLE.
        label.addClickListener(e -> {
            labelLayout.setVisible(false);
            searchLayout.setVisible(true);
            textField.focus();
        });

        // searchIconBtn TOGGLE.
        searchIconBtn.addClickListener(e -> {
            labelLayout.setVisible(false);
            searchLayout.setVisible(true);
            textField.focus();
        });

        // closeIconBtn TOGGLE.
        closeIconBtn.addClickListener(e -> {
            textField.clear();
            searchLayout.setVisible(false);
            labelLayout.setVisible(true);
        });

        // wrap both states in a root container layout.
        HorizontalLayout colHeaderLayout = new HorizontalLayout(labelLayout, searchLayout);
        colHeaderLayout.setWidthFull();
        colHeaderLayout.setPadding(false);

        return colHeaderLayout;
    }

    @SuppressWarnings("null")
    private void setupFormBinder() {

        company.setClearButtonVisible(true);
        formBinder.forField(company).bind(ProductPojo::getCompany, ProductPojo::setCompany);

        sku.setClearButtonVisible(true);
        formBinder.forField(sku).bind(ProductPojo::getSku, ProductPojo::setSku);

        name.setClearButtonVisible(true);
        formBinder.forField(name).bind(ProductPojo::getName, ProductPojo::setName);

        department.setClearButtonVisible(true);
        formBinder.forField(department).bind(ProductPojo::getDepartment, ProductPojo::setDepartment);

        category.setClearButtonVisible(true);
        formBinder.forField(category).bind(ProductPojo::getCategory, ProductPojo::setCategory);

        upc.setClearButtonVisible(true);
        formBinder.forField(upc).bind(ProductPojo::getUpc, ProductPojo::setUpc);

        barcode.setClearButtonVisible(true);
        formBinder.forField(barcode).bind(ProductPojo::getBarcode, ProductPojo::setBarcode);
        Button scanBarcodeBtn = new Button(VaadinIcon.BARCODE.create(), e -> openScannerDialog(barcode));
        scanBarcodeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        scanBarcodeBtn.getElement().getStyle().set("cursor", "pointer");
        barcode.setSuffixComponent(scanBarcodeBtn);

        setupCurrencyField(mrp);
        formBinder.forField(mrp)
            .withValidator(v -> v != null, "mrp is required")
            .bind(ProductPojo::getMrp, ProductPojo::setMrp);

        setupCurrencyField(costPrice);
        formBinder.forField(costPrice).bind(ProductPojo::getCostPrice, ProductPojo::setCostPrice);

        setupCurrencyField(sellPrice);
        formBinder.forField(sellPrice).bind(ProductPojo::getSellPrice, ProductPojo::setSellPrice);

        stock.setClearButtonVisible(true);
        formBinder.forField(stock)
            .withConverter(
                d -> d != null ? d.longValue() : null,
                l -> l != null ? l.doubleValue() : null,
                "enter a valid stock")
            .bind(ProductPojo::getStock, ProductPojo::setStock);

        threshold.setClearButtonVisible(true);
        formBinder.forField(threshold)
            .withConverter(
                d -> d != null ? d.longValue() : null,
                l -> l != null ? l.doubleValue() : null,
                "enter a valid threshold")
            .bind(ProductPojo::getThreshold, ProductPojo::setThreshold);

        hsnCode.setClearButtonVisible(true);
        formBinder.forField(hsnCode).bind(ProductPojo::getHsnCode, ProductPojo::setHsnCode);

        taxRate.setClearButtonVisible(true);
        formBinder.forField(taxRate).bind(ProductPojo::getTaxRate, ProductPojo::setTaxRate);

        cessRate.setClearButtonVisible(true);
        formBinder.forField(cessRate).bind(ProductPojo::getCessRate, ProductPojo::setCessRate);

        pkdDate.setClearButtonVisible(true);
        pkdDate.getElement().getStyle().set("cursor", "pointer");
        formBinder.forField(pkdDate)
            .withValidator(
                date -> date == null || !date.isAfter(LocalDate.now(ZoneId.systemDefault())),
                "pkd date cannot be future")
            .bind(ProductPojo::getPkdDate, ProductPojo::setPkdDate);

        expDate.setClearButtonVisible(true);
        expDate.getElement().getStyle().set("cursor", "pointer");
        formBinder.forField(expDate)
            .withValidator(
                date -> date == null || !date.isBefore(LocalDate.now(ZoneId.systemDefault())),
                "exp date cannot be past")
            .bind(ProductPojo::getExpDate, ProductPojo::setExpDate);

        formBinder.forField(taxInclusive)
            .bind(ProductPojo::isTaxInclusive,
                (p, v) -> p.setTaxInclusive(v != null && v));

        formBinder.forField(taxExempted)
            .bind(ProductPojo::isTaxExempted,
                (p, v) -> p.setTaxExempted(v != null && v));

        formBinder.forField(active)
            .bind(ProductPojo::isActive,
                (p, v) -> p.setActive(v != null && v));
    }

    @SuppressWarnings("null")
    private void setupGridBinder() {

        productGridDataView = productGrid.setItems(List.of());
        productFilter = new ProductFilter(productGridDataView);

        // HEADER ACTION CONTROLS (listens to checkbox selection updates)
        productGrid.addSelectionListener(e -> updateHeaderButtonStates());

        Grid.Column<ProductPojo> companyCol = productGrid.addColumn(ProductPojo::getCompany).setWidth("10rem");
        // Grid.Column<ProductPojo> nameCol = productGrid.addColumn(ProductPojo::getName).setWidth("10rem");
        // FIXED MULTI-ROW EDITABLE COLUMNS USING SIMPLE DELEGATED DATA-BINDING (no eager refreshes to break typing).
        Grid.Column<ProductPojo> nameCol = productGrid.addComponentColumn(product -> {
            if (editingRows.contains(product)) {
                TextField tf = new TextField();
                tf.addThemeVariants(TextFieldVariant.LUMO_SMALL);
                tf.setWidthFull();
                tf.setValue(product.getName() != null ? product.getName() : "");
                // track typing using standard lazy/eager updates WITHOUT calling refreshItem().
                tf.setValueChangeMode(ValueChangeMode.EAGER);
                tf.addValueChangeListener(e -> product.setName(e.getValue()));
                return tf;
            } else {
                return new Span(product.getName());
            }
        }).setWidth("10rem");
        Grid.Column<ProductPojo> departmentCol = productGrid.addColumn(ProductPojo::getDepartment);
        Grid.Column<ProductPojo> categoryCol = productGrid.addColumn(ProductPojo::getCategory);
        // Grid.Column<ProductPojo> mrpCol = productGrid.addColumn(ProductPojo::getMrp);
        Grid.Column<ProductPojo> mrpCol = productGrid.addComponentColumn(product -> {
            return new Span(product.getMrp() != null ? "₹" + product.getMrp().toString() : "");
        });
        // Grid.Column<ProductPojo> costPriceCol = productGrid.addColumn(ProductPojo::getCostPrice);
        Grid.Column<ProductPojo> costPriceCol = productGrid.addComponentColumn(product -> {
            return new Span(product.getCostPrice() != null ? "₹" + product.getCostPrice().toString() : "");
        });
        // Grid.Column<ProductPojo> sellPriceCol = productGrid.addColumn(ProductPojo::getSellPrice);
        Grid.Column<ProductPojo> sellPriceCol = productGrid.addComponentColumn(product -> {
            if (editingRows.contains(product)) {
                BigDecimalField bdf = new BigDecimalField();
                bdf.addThemeVariants(TextFieldVariant.LUMO_SMALL);
                bdf.setWidthFull();
                bdf.setValue(product.getSellPrice());
                bdf.addValueChangeListener(e -> product.setSellPrice(e.getValue()));
                return bdf;
            } else {
                return new Span(product.getSellPrice() != null ? "₹" + product.getSellPrice().toString() : "");
            }
        });
        // Grid.Column<ProductPojo> taxInclusiveCol = productGrid.addColumn(p -> p.isTaxInclusive() ? "yes" : "no");
        Grid.Column<ProductPojo> taxInclusiveCol = productGrid.addComponentColumn(product -> {
            boolean isInclusive = product.isTaxInclusive();
            Span badge = new Span(isInclusive ? "YES" : "NO");
            // built-in Lumo badge variants for background and text color.
            badge.getElement().getThemeList().add(isInclusive ? "badge success" : "badge error");
            badge.addClassNames(
                LumoUtility.Padding.Horizontal.SMALL,
                LumoUtility.Padding.Vertical.SMALL,
                LumoUtility.BorderRadius.SMALL,
                LumoUtility.FontWeight.MEDIUM,
                LumoUtility.FontSize.XXSMALL
            );
            return badge;
        }).setKey("keyTaxInclusive");
        // Grid.Column<ProductPojo> taxExemptedCol = productGrid.addColumn(p -> p.isTaxExempted() ? "yes" : "no");
        Grid.Column<ProductPojo> taxExemptedCol = productGrid.addComponentColumn(product -> {
            boolean isExempted = product.isTaxExempted();
            Span badge = new Span(isExempted ? "YES" : "NO");
            // built-in Lumo badge variants for background and text color.
            badge.getElement().getThemeList().add(isExempted ? "badge success" : "badge error");
            badge.addClassNames(
                LumoUtility.Padding.Horizontal.SMALL,
                LumoUtility.Padding.Vertical.SMALL,
                LumoUtility.BorderRadius.SMALL,
                LumoUtility.FontWeight.MEDIUM,
                LumoUtility.FontSize.XXSMALL
            );
            return badge;
        }).setKey("keyTaxExempted");
        // Grid.Column<ProductPojo> skuCol = productGrid.addColumn(ProductPojo::getSku).setWidth("10rem");
        Grid.Column<ProductPojo> skuCol = productGrid.addColumn(ProductPojo::getSku).setWidth("10rem");
        Grid.Column<ProductPojo> upcCol = productGrid.addColumn(ProductPojo::getUpc);
        Grid.Column<ProductPojo> barcodeCol = productGrid.addColumn(ProductPojo::getBarcode);
        // Grid.Column<ProductPojo> stockCol = productGrid.addColumn(ProductPojo::getStock);
        Grid.Column<ProductPojo> stockCol = productGrid.addComponentColumn(product -> {
            if (editingRows.contains(product)) {
                NumberField nf = new NumberField();
                nf.addThemeVariants(TextFieldVariant.LUMO_SMALL);
                nf.setWidthFull();
                nf.setValue(product.getStock() != null ? product.getStock().doubleValue() : null);
                nf.addValueChangeListener(e -> product.setStock(e.getValue() != null ? e.getValue().longValue() : null));
                return nf;
            } else {
                return new Span(product.getStock() != null ? product.getStock().toString() : "");
            }
        });
        Grid.Column<ProductPojo> thresholdCol = productGrid.addColumn(ProductPojo::getThreshold);
        Grid.Column<ProductPojo> hsnCodeCol = productGrid.addColumn(ProductPojo::getHsnCode);
        Grid.Column<ProductPojo> taxRateCol = productGrid.addColumn(ProductPojo::getTaxRate);
        Grid.Column<ProductPojo> cessRateCol = productGrid.addColumn(ProductPojo::getCessRate);
        Grid.Column<ProductPojo> pkdDateCol = productGrid.addColumn(p -> p.getPkdDate() == null ? "" : p.getPkdDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        Grid.Column<ProductPojo> expDateCol = productGrid.addColumn(p -> p.getExpDate() == null ? "" : p.getExpDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));        
        // Grid.Column<ProductPojo> activeCol       = productGrid.addColumn(p -> p.isActive() ? "active" : "inactive").setAutoWidth(true);
        Grid.Column<ProductPojo> activeCol = productGrid.addComponentColumn(product -> {
            if (editingRows.contains(product)) {
                Checkbox cb = new Checkbox();                
                cb.setValue(product.isActive());
                cb.addValueChangeListener(e -> product.setActive(e.getValue() != null && e.getValue()));
                return cb;
            } else {
                // return new Span(product.isActive() ? "active" : "inactive");
                boolean isActive = product.isActive();
                Span badge = new Span(isActive ? "ACTIVE" : "INACTIVE");
                // built-in Lumo badge variants for background and text color.
                badge.getElement().getThemeList().add(isActive ? "badge success" : "badge error");
                badge.addClassNames(
                    LumoUtility.Padding.Horizontal.SMALL,
                    LumoUtility.Padding.Vertical.SMALL,
                    LumoUtility.BorderRadius.SMALL,
                    LumoUtility.FontWeight.MEDIUM,
                    LumoUtility.FontSize.XXSMALL
                );
                return badge;
            }
        }).setAutoWidth(true);

        HeaderRow headerRow = productGrid.appendHeaderRow();
        headerRow.getCell(companyCol).setComponent(createSearchHeader("company", "search company", productFilter::setCompany));
        headerRow.getCell(nameCol).setComponent(createSearchHeader("name", "search name", productFilter::setName));
        headerRow.getCell(departmentCol).setText("department");
        headerRow.getCell(categoryCol).setText("category");
        headerRow.getCell(mrpCol).setText("mrp (₹)");
        headerRow.getCell(costPriceCol).setText("cost price (₹)");
        headerRow.getCell(sellPriceCol).setText("sell price (₹)");
        headerRow.getCell(taxInclusiveCol).setText("tax inclusive");
        headerRow.getCell(taxExemptedCol).setText("tax exempted");
        headerRow.getCell(skuCol).setComponent(createSearchHeader("sku", "search sku", productFilter::setSku));
        headerRow.getCell(upcCol).setText("upc");
        headerRow.getCell(barcodeCol).setText("barcode");
        headerRow.getCell(stockCol).setText("stock");
        headerRow.getCell(thresholdCol).setText("threshold");
        headerRow.getCell(pkdDateCol).setText("pkd date");
        headerRow.getCell(expDateCol).setText("exp date");
        headerRow.getCell(hsnCodeCol).setText("hsn code");
        headerRow.getCell(taxRateCol).setText("tax rate");
        headerRow.getCell(cessRateCol).setText("cess rate");        
        headerRow.getCell(activeCol).setText("status");

        // productGrid.getColumns().get(0).setFrozen(true);
        // productGrid.getColumns().get(1).setFrozen(true);
        // productGrid.getColumns().get(2).setFrozen(true);
        companyCol.setFrozen(true);
        nameCol.setFrozen(true);
        activeCol.setFrozenToEnd(true);

        // HEADER ACTION BUTTON LISTENERS (BULK MULTI-ROW TRIGGER)
        saveAllBtn.addClickListener(e -> {
            try {
                for (ProductPojo product : editingRows) {
                    productFacade.saveProduct(product);
                }
                Notify.success("product(s) updated successfully!");
                editingRows.clear();
                refreshGrid();            
            } catch (Exception ex) {
                logger.error("bulk edit save failed", ex);
                Notify.error("update failed : " + ex.getMessage());
            }
        });

        deleteAllBtn.addClickListener(e -> handleDelete());

        HorizontalLayout actionHeaderLayout = new HorizontalLayout(saveAllBtn, clearAllBtn, deleteAllBtn);
        actionHeaderLayout.setPadding(false);
        actionHeaderLayout.setSpacing(true);
        actionHeaderLayout.setAlignItems(FlexComponent.Alignment.CENTER);        

        // DYNAMIC MULTI-ROW ACTION COLUMN CONFIGURATION
        productGrid.addComponentColumn(product -> {

            Button editBtn = new Button(LumoIcon.EDIT.create());
            editBtn.getElement().getStyle().set("cursor", "pointer");
            editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);

            Button clearBtn = new Button(LumoIcon.CROSS.create());
            clearBtn.getElement().getStyle().set("cursor", "pointer");
            clearBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);

            boolean isEditing = editingRows.contains(product);
            
            // TOGGLE enablement state contextually.
            editBtn.setEnabled(!isEditing);
            clearBtn.setEnabled(isEditing);

            editBtn.addClickListener(clickEvent -> {
                // clicking EDIT deselects row to prevent state collisions.
                productGrid.deselect(product);
                editingRows.add(product);
                productGrid.getListDataView().refreshAll();
                updateHeaderButtonStates();
            });

            clearBtn.addClickListener(clickEvent -> {
                editingRows.remove(product);
                // FIXED: Only target the specific row without refreshing full items from backend database
                // refreshGrid();
                productGrid.getListDataView().refreshAll();
                updateHeaderButtonStates();
            });

            HorizontalLayout actions = new HorizontalLayout(editBtn, clearBtn);
            actions.setPadding(false);
            actions.setSpacing(true);
            return actions;

        }).setHeader(actionHeaderLayout).setFrozenToEnd(true).setWidth("5.5rem").setFlexGrow(0);
    }

    // controls visibility swaps and permissions for HEADER buttons.
    private void updateHeaderButtonStates() {

        boolean linesBeingEdited = !editingRows.isEmpty();
        
        // SAVE button enabled if at least 1 row is edited, otherwise disabled.
        // DELETE button disabled if editing.
        saveAllBtn.setEnabled(linesBeingEdited);
        
        int selectedCount = productGrid.getSelectedItems().size();

        // DELETE button enabled only when checkbox(es) are selected AND no lines are currently in an active edit state.
        // replaces deleteAllBtn with clearAllBtn contextually when in edit mode.
        if (linesBeingEdited) {
            deleteAllBtn.setVisible(false);
            clearAllBtn.setVisible(true);
            clearAllBtn.setEnabled(true);
        } else {
            clearAllBtn.setVisible(false);
            clearAllBtn.setEnabled(false);
            deleteAllBtn.setVisible(true);
            deleteAllBtn.setEnabled(selectedCount > 0);
        }
    }

    private void handleSubmit() {
        var validationResult = formBinder.validate();
        if (validationResult.hasErrors()) {
            validationResult.getFieldValidationErrors().forEach(error ->
                error.getMessage().ifPresent(Notify::error));
            return;
        }
        ProductPojo productPojo = new ProductPojo();
        try {
            formBinder.writeBean(productPojo);
            productFacade.saveProduct(productPojo);
            Notify.success("created " + productPojo.getSku());
            refreshGrid();
            formBinder.readBean(new ProductPojo());
        } catch (ValidationException ex) {
            logger.error("form validation failed", ex);
        } catch (Exception e) {
            Notify.error(e);
        }
    }

    private void handleDelete() {
        Set<ProductPojo> selectedProduct = productGrid.getSelectedItems();
        int count = selectedProduct.size();

        if (count == 0) {
            Notify.error("please select at least one product to delete");
            return;
        }

        Button deleteBtn = new Button("delete");
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        deleteBtn.getElement().getStyle().set("cursor", "pointer");

        Button cancelBtn = new Button("cancel");
        cancelBtn.getElement().getStyle().set("cursor", "pointer");

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("confirm delete");
        dialog.setText(String.format("do you want to delete selected %d product(s)?", count));
        dialog.setCancelable(true);
        dialog.setConfirmButton(deleteBtn);
        dialog.setCancelButton(cancelBtn);

        dialog.addOpenedChangeListener(event -> {
            if (event.isOpened()) {
                cancelBtn.getElement().executeJs("setTimeout(() => this.focus(), 100)");
            }
        });

        dialog.addConfirmListener(confirmEvent -> {
            productFacade.deleteMultiProduct(selectedProduct);
            refreshGrid();
            Notify.success(count + " item(s) deleted successfully!");
            dialog.close();
        });

        dialog.open();
    }

    private void refreshGrid() {
        try {
            // if (productGridEditor.isOpen()) {
            //     productGridEditor.cancel();
            //     editAllBtn.setVisible(true);
            //     saveAllBtn.setVisible(false);
            //     cancelAllBtn.setVisible(false);
            // }
            List<ProductPojo> fresh = productFacade.getAllProduct();
            productGridDataView = productGrid.setItems(fresh);
            productFilter.setDataView(productGridDataView);
        } catch (Exception e) {
            logger.error("failed to refresh product grid", e);
            Notify.error("service-product is down");
        }
    }

    private void connectWebSocket(TextField skuField) {
        registeredUi = UI.getCurrent();
        barcodeWebSocketHandler.registerConsumer(sessionId, barcodeValue -> {
            UI ui = registeredUi;
            if (ui != null) {
                ui.access(() -> {
                    skuField.setValue(barcodeValue);
                    skuField.focus();
                });
            }
        });
    }

    private void openScannerDialog(TextField skuField) {
        Dialog dialog = new Dialog();
        dialog.setWidth("350px");

        H2 title = new H2("scan this QR with mobile");
        title.addClassNames(LumoUtility.TextAlignment.CENTER, LumoUtility.Width.FULL,
            LumoUtility.Margin.LARGE, LumoUtility.FontSize.LARGE);
        dialog.getHeader().add(title);

        try {
            String serverIp = java.net.InetAddress.getLocalHost().getHostAddress();
            String mobileUrl = "http://" + serverIp + ":8080/scan/" + sessionId;
            byte[] qrBytes = qrCodeService.generateQrCode(mobileUrl, 220, 220);
            String base64 = java.util.Base64.getEncoder().encodeToString(qrBytes);

            Image qrImage = new Image("data:image/png;base64," + base64, "QR");
            qrImage.setWidth("250px");

            VerticalLayout hintsContainer = new VerticalLayout();
            hintsContainer.setPadding(false);
            hintsContainer.setSpacing(false);
            hintsContainer.setAlignItems(FlexComponent.Alignment.START);
            hintsContainer.addClassNames(LumoUtility.Margin.Horizontal.SMALL);
            hintsContainer.setWidth("200px");

            Paragraph hint1 = new Paragraph("1. scan this with mobile");
            Paragraph hint2 = new Paragraph("2. scan product barcode");
            Paragraph hint3 = new Paragraph("3. barcode field will auto-fill");
            hint1.addClassNames(LumoUtility.Margin.Vertical.SMALL);
            hint2.addClassNames(LumoUtility.Margin.Vertical.SMALL);
            hint3.addClassNames(LumoUtility.Margin.Vertical.SMALL);
            hintsContainer.add(hint1, hint2, hint3);

            VerticalLayout content = new VerticalLayout(qrImage, hintsContainer);
            content.setAlignItems(FlexComponent.Alignment.CENTER);
            content.setPadding(false);

            Button closeBtn = new Button("close", e -> dialog.close());
            closeBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            closeBtn.getElement().getStyle().set("cursor", "pointer");

            dialog.add(content);
            dialog.getFooter().add(closeBtn);

        } catch (Exception ex) {
            dialog.add(new Paragraph("could not generate QR: " + ex.getMessage()));
        }

        dialog.open();
    }

    @Override
    protected void onDetach(com.vaadin.flow.component.DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        barcodeWebSocketHandler.unregisterConsumer(sessionId);
    }

    // private Span createStyledHeader(String title) {
    //     Span headerText = new Span(title);
    //     headerText.addClassNames(
    //         LumoUtility.FontWeight.MEDIUM,
    //         // LumoUtility.Background.SHADE_10,
    //         LumoUtility.TextColor.PRIMARY,
    //         // stretches the Span over the default padding boundaries.
    //         // FIX : SKU and search box overlap.
    //         // LumoUtility.Position.ABSOLUTE,
    //         // LumoUtility.Position.Top.NONE,
    //         // LumoUtility.Position.Bottom.NONE,
    //         // LumoUtility.Position.Start.NONE,
    //         // LumoUtility.Position.End.NONE,
            
    //         // centers text layout horizontally and vertically.
    //         LumoUtility.Display.FLEX,
    //         LumoUtility.AlignItems.CENTER,
    //         LumoUtility.Padding.Horizontal.SMALL,

    //         // FIX : SKU and search box overlap.
    //         LumoUtility.Width.FULL,
    //         LumoUtility.Display.FLEX,
    //         LumoUtility.AlignItems.CENTER,
    //         LumoUtility.Padding.Horizontal.SMALL
    //     );
    //     return headerText;
    // }
}