package com.superstore.app.view;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.superstore.app.facade.OrderFacade;
import com.superstore.app.facade.ProductFacade;
import com.superstore.app.pojo.OrderPojo;
import com.superstore.app.pojo.ProductPojo;
import com.superstore.app.pojo.OrderPojo.OrdDtl;
import com.superstore.app.pojo.OrderPojo.OrdHdr;
import com.superstore.app.utility.Notify;
import com.superstore.app.websocket.BarcodeWebSocketHandler;
import com.superstore.app.websocket.QrCodeService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
// import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
// import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoIcon;
import com.vaadin.flow.theme.lumo.LumoUtility;

import jakarta.annotation.security.PermitAll;

@Route("bill")
// @PageTitle("bill")
@Menu(order = 2, title = "bill")
@PermitAll
public class BillView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(BillView.class);

    // GST slabs used in India for retail goods.
    // private static final BigDecimal GST_5   = new BigDecimal("0.05");
    // private static final BigDecimal GST_12  = new BigDecimal("0.12");
    private static final BigDecimal GST_18  = new BigDecimal("0.18");
    // private static final BigDecimal GST_28  = new BigDecimal("0.28");
    // default slab (18% CGST 9% + SGST 9%) – kept for backward-compat.
    // private static final BigDecimal GST_RATE = GST_18;

    private final ProductFacade productFacade;
    private final OrderFacade orderFacade;
    private final QrCodeService qrCodeService;
    private final BarcodeWebSocketHandler barcodeWebSocketHandler;

    private final String sessionId = UUID.randomUUID().toString();
    private UI registeredUi;

    private String lastProductSearchText = "";
    private List<ProductPojo> redisCachedProductResult = new ArrayList<>();

    private String tabSessionKey;
    private boolean isRestoring = false;
    private TextField mobileField;

    // BillRow is a UI-only view model that merges OrdDtl + MRP for display.
    // it is NOT persisted directly; on save we build an OrderPojo from it.
    public static class BillRow {
        private Long       lineNo;
        private String     company;
        private String     name;
        private Long       qty;
        private BigDecimal mrp;      // pulled from ProductPojo at add-time.
        private BigDecimal discount; // 0–100 %, default 0.
        private BigDecimal gstRate;  // like 0.05 / 0.12 / 0.18 / 0.28, default GST_18.

        public Long       getLineNo()                  { return lineNo;       }
        public void       setLineNo(Long v)            { lineNo = v;          }
        public String     getCompany()                 { return company;      }
        public void       setCompany(String v)         { company = v;         }
        public String     getName()                    { return name;         }
        public void       setName(String v)            { name = v;            }
        public Long       getQty()                     { return qty;          }
        public void       setQty(Long v)               { qty = v;             }
        public BigDecimal getMrp()                     { return mrp;          }
        public void       setMrp(BigDecimal v)         { mrp = v;             }
        public BigDecimal getDiscount()                { return discount;     }
        public void       setDiscount(BigDecimal v)    { discount = v;        }
        public BigDecimal getGstRate()                 { return gstRate;      }
        public void       setGstRate(BigDecimal v)     { gstRate = v;         }

        // effective slab rate (never null).
        public BigDecimal resolvedGstRate() {
            return gstRate != null ? gstRate : GST_18;
        }

        // MRP × qty (before discount).
        public BigDecimal getGrossAmount() {
            if (mrp == null || qty == null) return BigDecimal.ZERO;
            return mrp.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
        }

        // discount amount = gross × discount / 100.
        public BigDecimal getDiscountAmount() {
            if (discount == null || discount.compareTo(BigDecimal.ZERO) == 0)
                return BigDecimal.ZERO;
            return getGrossAmount()
                .multiply(discount)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }

        // subTotal = gross − discount (MRP - inclusive of GST).
        public BigDecimal getSubTotal() {
            return getGrossAmount().subtract(getDiscountAmount());
        }

        // taxable base = subTotal / (1 + gstRate).
        public BigDecimal getTaxableBase() {
            return getSubTotal().divide(BigDecimal.ONE.add(resolvedGstRate()), 2, RoundingMode.HALF_UP);
        }

        // total GST on this line.
        // public BigDecimal getLineTotalGst() {
        //     return getSubTotal().subtract(getTaxableBase());
        // }

        // separate CGST and SGST at the row level
        public BigDecimal getLineCgst() {
            BigDecimal halfRate = resolvedGstRate().divide(BigDecimal.TWO);
            return getTaxableBase().multiply(halfRate).setScale(2, RoundingMode.HALF_UP);
        }

        public BigDecimal getLineSgst() {
            return getLineCgst(); // Ensure complete symmetry
        }

        public BigDecimal getLineTotalGst() {
            return getLineCgst().add(getLineSgst());
        }        

        // display helper – GST % label.
        public String getGstLabel() {
            BigDecimal pct = resolvedGstRate().multiply(new BigDecimal("100")).stripTrailingZeros();
            return pct.toPlainString() + "%";
        }
    }

    private final List<BillRow> listBillRow = new ArrayList<>();
    private ListDataProvider<BillRow> dataProvider;
    private Long currentOrdNo  = null;
    private Long currentMobile = null;
    private Grid<BillRow> billGrid;
    // private Editor<BillRow> editor;
    private Span grossTotalSpan;
    private Span discountSpan;
    private Span cgstSpan;
    private Span sgstSpan;
    private Span netPayableSpan;

    // GRID HEADER trash button.
    private Button headerTrashBtn;    

    public BillView(ProductFacade productFacade,
                    OrderFacade orderFacade,
                    QrCodeService qrCodeService,
                    BarcodeWebSocketHandler barcodeWebSocketHandler) {
        this.productFacade = productFacade;
        this.orderFacade = orderFacade;
        this.qrCodeService = qrCodeService;
        this.barcodeWebSocketHandler = barcodeWebSocketHandler;
    }

    @SuppressWarnings("null")
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        // fetch a unique window/tab identifier from Vaadin.
        attachEvent.getUI().getPage().executeJs("return window.name;")
            .then(String.class, windowName -> {
                // If window.name is empty (like a brand new tab), generate a unique ID for it
                if (windowName == null || windowName.isEmpty()) {
                    String newWindowId = UUID.randomUUID().toString();
                    attachEvent.getUI().getPage().executeJs("window.name = '" + newWindowId + "';");
                    windowName = newWindowId;
                }
                
                this.tabSessionKey = "BILL_ITEMS_" + windowName;
                
                // restore previous items for THIS tab from the session.
                restoreTabState();
            });

        removeAll(); // VERY IMPORTANT (prevents duplicate UI on re-attach)
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // INPUT LAYOUT (inputs + actions) combined.
        // moved to class level for session restore on refresh.
        // TextField mobileField = new TextField("mobile");
        mobileField = new TextField("mobile");
        mobileField.setPlaceholder("customer mobile");
        mobileField.setClearButtonVisible(true);
        mobileField.setWidth("8.5rem");
        mobileField.addValueChangeListener(e -> {
            // if we are currently restoring from session, do nothing and don't overwrite.
            if (isRestoring) {
                return;
            }
            String val = e.getValue();
            if (val != null && !val.isBlank()) {
                try { 
                    currentMobile = Long.parseLong(val.trim()); 
                } catch (NumberFormatException ex) { 
                    currentMobile = null;
                }
            } else {
                currentMobile = null;
            }
            saveTabStateToSession();
        });

        ComboBox<ProductPojo> productSearch = new ComboBox<>("product");
        productSearch.setPlaceholder("search company (or) name (or) barcode");
        productSearch.setClearButtonVisible(true);
        // productSearch.setWidthFull();
        productSearch.setWidth("18.5rem");
        // display : company | name | stock
        productSearch.setItemLabelGenerator(p -> 
                p.getCompany() + "  |  " + 
                p.getName() + "  |  " + 
                "stock:" + (p.getStock() != null ? p.getStock() : "—"));

        // LAZY fetch.
        // productSearch.addCustomValueSetListener(e -> {
        //     String searchText = e.getDetail() == null ? "" : e.getDetail().toLowerCase();
        //     try {
        //         List<ProductPojo> listProduct = productFacade.searchProduct(searchText);
        //         productSearch.setItems(listProduct);
        //     } catch (Exception ex) {
        //         Notify.error(ex);
        //     }
        // });

        // EAGER fetch.
        // Vaadin sends query with :
        // - offset = 0   (start from index 0)
        // - limit  = 10  (give me 10 items)
        // our stream MUST call .skip(offset).limit(limit)
        // otherwise Vaadin throws IllegalStateException ❌
        // productSearch.setItems(
        //     // fetch callback
        //     query -> {
        //         String searchText = query.getFilter().orElse("").toLowerCase().trim();
        //         if (searchText.isEmpty()) return java.util.stream.Stream.empty();
        //         try {
        //             return productFacade.searchProduct(searchText).stream()
        //                 .skip(query.getOffset())
        //                 .limit(query.getLimit());
        //         } catch (Exception ex) {
        //             Notify.error(ex);
        //             return java.util.stream.Stream.empty();
        //         }
        //     },
        //     // count callback
        //     query -> {
        //         String searchText = query.getFilter().orElse("").toLowerCase().trim();
        //         if (searchText.isEmpty()) return 0;
        //         try {
        //             return productFacade.searchProduct(searchText).size();
        //         } catch (Exception ex) {
        //             return 0;
        //         }
        //     }
        // );
        productSearch.setItems(
            // fetch callback
            query -> {
                String searchText = query.getFilter().orElse("").toLowerCase().trim();
                List<ProductPojo> results = getRedisCachedProduct(searchText);
                
                return results.stream()
                    .skip(query.getOffset())
                    .limit(query.getLimit());
            },
            // count callback (runs first, populates/checks cache safely)
            query -> {
                String searchText = query.getFilter().orElse("").toLowerCase().trim();
                return getRedisCachedProduct(searchText).size();
            }
        );

        // not available on ComboBox.
        // productSearch.setMinLength(1); // start searching after 1 character.
        productSearch.setPageSize(10); // limit dropdown items shown.
        productSearch.setClearButtonVisible(true);
        productSearch.setAutoOpen(true);

        // add product to grid.
        productSearch.addValueChangeListener(e -> {
            ProductPojo product = e.getValue();
            if (product != null) {
                addProductToGrid(product);
                productSearch.clear();
            }
        });

        // BARCODE scan button.
        Button barcodeBtn = new Button(VaadinIcon.BARCODE.create());
        barcodeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        barcodeBtn.getElement().getStyle().set("cursor", "pointer");
        barcodeBtn.addClickListener(e -> openScannerDialog(productSearch));

        HorizontalLayout productSearchWithBarcode = new HorizontalLayout(productSearch, barcodeBtn);
        //productSearchWithBarcode.setWidthFull();
        productSearchWithBarcode.setSpacing(true);
        productSearchWithBarcode.setAlignItems(FlexComponent.Alignment.BASELINE);

        connectWebSocket(productSearch); // BARCODE scanner (USB/Bluetooth)

        // TOP LEFT LAYOUT : mobile | search | barcodeBtn
        HorizontalLayout inputLayout = new HorizontalLayout(mobileField, productSearchWithBarcode);
        inputLayout.setJustifyContentMode(JustifyContentMode.START);
        inputLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        inputLayout.setWidthFull();
        inputLayout.setFlexGrow(1, productSearch);

        // TOP RIGHT LAYOUT : delete | clear | save | print

        // Button deleteSelectedBtn = new Button("delete", VaadinIcon.TRASH.create());
        // deleteSelectedBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
        // deleteSelectedBtn.getElement().getStyle().set("cursor", "pointer");
        // deleteSelectedBtn.addClickListener(e -> handleDeleteSelected());

        Button clearBillBtn = new Button("clear", LumoIcon.CROSS.create());
        clearBillBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
        clearBillBtn.getElement().getStyle().set("cursor", "pointer");
        clearBillBtn.addClickListener(e -> handleClearBill());

        Button saveOrderBtn = new Button("save", LumoIcon.DOWNLOAD.create());
        saveOrderBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
        saveOrderBtn.getElement().getStyle().set("cursor", "pointer");
        saveOrderBtn.addClickListener(e -> handleSaveOrder());

        Button printBillBtn = new Button("print", VaadinIcon.PRINT.create());
        printBillBtn.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL);
        printBillBtn.getElement().getStyle().set("cursor", "pointer");
        printBillBtn.addClickListener(e -> handlePrintBill());

        HorizontalLayout buttonLayout = new HorizontalLayout(clearBillBtn, saveOrderBtn, printBillBtn);
        buttonLayout.setWidthFull();
        buttonLayout.setJustifyContentMode(JustifyContentMode.END);
        buttonLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        // TOP HEADER LAYOUT (MASTER)
        HorizontalLayout topHeaderLayout = new HorizontalLayout(inputLayout, buttonLayout);
        topHeaderLayout.setWidthFull();
        topHeaderLayout.setFlexGrow(1, inputLayout);
        topHeaderLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);

        // BODY LAYOUT (BILLGRID + TOTAL)
        HorizontalLayout bodyLayout = new HorizontalLayout();
        bodyLayout.setSizeFull();
        bodyLayout.setSpacing(true);
        
        // BILLGRID
        billGrid = new Grid<>(BillRow.class, false);
        billGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_COLUMN_BORDERS);
        billGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        billGrid.setWidthFull();
        billGrid.setHeightFull();
        billGrid.setAllRowsVisible(false); // STOP PAGE SCROLL.

        // HEADER TRASH ICON
        billGrid.addSelectionListener(event -> {
            boolean hasSelection = !event.getAllSelectedItems().isEmpty();
            if (headerTrashBtn != null) {
                headerTrashBtn.setEnabled(hasSelection);
            }
        });

        // BILLGRID COLUMNS

        // Grid.Column<BillRow> lineNoCol = billGrid
        billGrid
            .addColumn(BillRow::getLineNo)
            .setHeader("line")
            .setWidth("2.5rem")
            .setFlexGrow(0);

        // Grid.Column<BillRow> companyCol = billGrid
        billGrid
            .addColumn(BillRow::getCompany)
            .setHeader("company");

        // Grid.Column<BillRow> nameCol = billGrid
        billGrid
            .addColumn(BillRow::getName)
            .setHeader("name");

        // ALWAYS EDITABLE qty column (OrdDtl.qty)
        // Grid.Column<BillRow> qtyCol = billGrid
        //     .addColumn(BillRow::getQty)
        //     .setHeader("qty");
        billGrid.addComponentColumn(row -> {
            IntegerField qtyField = new IntegerField();
            qtyField.setMin(1);
            qtyField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
            qtyField.setWidthFull();
            // // ADD THIS LINE TO ENABLE INSTANT UPDATES WHILE TYPING.
            // qtyField.setValueChangeMode(com.vaadin.flow.data.value.ValueChangeMode.LAZY);
            // qtyField.setValueChangeMode(com.vaadin.flow.data.value.ValueChangeMode.EAGER);
            // qtyField.setValueChangeMode(com.vaadin.flow.data.value.ValueChangeMode.ON_CHANGE);
            // qtyField.setValueChangeTimeout(300); // 300ms debounce window
            qtyField.setValue(row.getQty() != null ? row.getQty().intValue() : 1);
            qtyField.addValueChangeListener(event -> {
                Integer val = event.getValue();
                if (val != null && val >= 1) {
                    row.setQty(val.longValue());
                }
                else {
                    row.setQty(1L);
                }
                // refresh current row non-editable columns (like subtotal) and total panel.
                billGrid.getDataProvider().refreshItem(row);
                refreshTotal();
                Notify.success("updated [" + row.getName() + "] qty : " + row.getQty());
                saveTabStateToSession();
            });
            return qtyField;
        })
        .setHeader("quantity")
        .setWidth("4rem")
        .setFlexGrow(0);

        // Grid.Column<BillRow> mrpCol = billGrid
        billGrid
            .addColumn(r -> r.getMrp() != null ? r.getMrp() : "—")
            .setHeader("MRP (₹)")
            .setWidth("5rem")
            .setFlexGrow(0);

        // ALWAYS EDITABLE discount % column.
        // Grid.Column<BillRow> discountCol = billGrid
        //     .addColumn(r -> r.getDiscount() != null
        //         ? r.getDiscount().setScale(1, RoundingMode.HALF_UP).toPlainString() + "%" : "0%")
        //     .setHeader("discount (%)");
        billGrid.addComponentColumn(row -> {
            NumberField discountField = new NumberField();
            discountField.setMin(0);
            discountField.setMax(100);
            discountField.setStep(0.5);
            discountField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
            discountField.setWidthFull();
            discountField.setPlaceholder("0-100");
            discountField.setValue(row.getDiscount() != null ? row.getDiscount().doubleValue() : 0.0);
            discountField.addValueChangeListener(event -> {
                Double val = event.getValue();
                if (val != null && val >= 0 && val <= 100) {
                    row.setDiscount(BigDecimal.valueOf(val));
                } else {
                    row.setDiscount(BigDecimal.ZERO);
                }
                billGrid.getDataProvider().refreshItem(row);
                refreshTotal();
                Notify.success("updated [" + row.getName() + "] discount : " + row.getDiscount());
                saveTabStateToSession();
            });
            return discountField;
        })
        .setHeader("discount (%)")
        .setWidth("5.5rem")
        .setFlexGrow(0);

        // GST slab column (display only).
        billGrid
            .addColumn(BillRow::getGstLabel)
            .setHeader("GST (%)")
            .setWidth("4rem")
            .setFlexGrow(0);

        // Grid.Column<BillRow> subTotalCol = billGrid
        billGrid
            .addColumn(r -> r.getSubTotal())
            .setHeader("subtotal (₹)")
            .setWidth("5rem")
            .setFlexGrow(0);

        // ACTION COLUMN (header trash icon + row level trash icon).
        headerTrashBtn = new Button(VaadinIcon.TRASH.create());
        headerTrashBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);
        headerTrashBtn.getElement().getStyle().set("cursor", "pointer");
        headerTrashBtn.setEnabled(false); // Disabled initially
        headerTrashBtn.addClickListener(e -> handleDeleteSelected());

        // Grid.Column<BillRow> actionCol = billGrid
        billGrid
            .addComponentColumn(row -> {
                Button removeBtn = new Button(VaadinIcon.TRASH.create());
                removeBtn.addThemeVariants(
                    ButtonVariant.LUMO_ERROR,
                    ButtonVariant.LUMO_TERTIARY_INLINE,
                    ButtonVariant.LUMO_SMALL);
                removeBtn.getElement().getStyle().set("cursor", "pointer");
                removeBtn.addClickListener(e -> removeLine(row));
                return removeBtn;
            })
            // .setHeader("action")
            .setHeader(headerTrashBtn)
            .setWidth("2rem")
            .setFlexGrow(0);

        // IN-GRID editor (same buffered-editor pattern).
        // editor = billGrid.getEditor();

        // Binder<BillRow> editorBinder = new Binder<>(BillRow.class);
        
        // editor.setBinder(editorBinder);
        // editor.setBuffered(true);

        // IntegerField editedQty = new IntegerField();
        // editedQty.setMin(1);
        // editedQty.setWidthFull();
        
        // editorBinder
        //     .forField(editedQty)
        //     .withConverter(
        //         intVal -> intVal == null ? null : intVal.longValue(),
        //         longVal -> longVal == null ? null : longVal.intValue())
        //     .withValidator(v -> v != null && v >= 1L, "qty must be ≥ 1")
        //     .bind(BillRow::getQty, BillRow::setQty);

        // qtyCol.setEditorComponent(editedQty);

        // inline editor for discount.
        // NumberField editedDiscount = new NumberField();
        // editedDiscount.setMin(0);
        // editedDiscount.setMax(100);
        // editedDiscount.setStep(0.5);
        // editedDiscount.setWidthFull();
        // editedDiscount.setPlaceholder("0–100");

        // editorBinder
        //     .forField(editedDiscount)
        //     .withConverter(
        //         d -> d == null ? BigDecimal.ZERO : BigDecimal.valueOf(d),
        //         bd -> bd == null ? 0.0 : bd.doubleValue())
        //     .withValidator(v -> v != null && v.compareTo(BigDecimal.ZERO) >= 0
        //             && v.compareTo(new BigDecimal("100")) <= 0, "0–100 only")
        //     .bind(BillRow::getDiscount, BillRow::setDiscount);

        // discountCol.setEditorComponent(editedDiscount);

        // double-click row -> open inline editor.
        // billGrid.addItemDoubleClickListener(event -> {
        //     editor.editItem(event.getItem());
        //     editedQty.focus();
        // });

        // Enter -> save editor.
        // editedQty.getElement().addEventListener("keydown", e -> {
        //     if (editor.isOpen()) {
        //         editor.save();
        //     }
        // }).setFilter("event.key === 'Enter'");

        // editor.addSaveListener(e -> {
        //     dataProvider.refreshAll();
        //     refreshTotal();
        // });

        // editor.addCancelListener(e -> {
        //     //dataProvider.refreshAll();
        //     // NO-OP : Vaadin closes the row editor on cancel automatically.
        // });

        dataProvider = new ListDataProvider<>(listBillRow);
        billGrid.setDataProvider(dataProvider);

        // TOTAL SPAN
        grossTotalSpan  = monoSpan("gross total",         "₹ 0.00", false);
        discountSpan    = monoSpan("discount (-)",        "₹ 0.00", false);
        cgstSpan        = monoSpan("CGST (½ GST)",        "₹ 0.00", false);
        sgstSpan        = monoSpan("SGST (½ GST)",        "₹ 0.00", false);
        netPayableSpan  = monoSpan("net payable",         "₹ 0.00", true);

        VerticalLayout totalLayout = new VerticalLayout(grossTotalSpan, discountSpan, cgstSpan, sgstSpan, netPayableSpan);
        totalLayout.setWidth("350px");
        totalLayout.setAlignItems(FlexComponent.Alignment.END);
        totalLayout.setPadding(true);
        totalLayout.setSpacing(false);
        totalLayout.getStyle()
            .set("border", "1px solid var(--lumo-contrast-20pct)");

        // ASSEMBLE BODY LAYOUT (BILLGRID + TOTAL)
        bodyLayout.add(billGrid, totalLayout);
        bodyLayout.setFlexGrow(1, billGrid); // billGrid consumes all left-hand room dynamically.
        // ensures two components stretch evenly across the exact same height boundary.
        bodyLayout.setAlignItems(FlexComponent.Alignment.STRETCH);

        add(topHeaderLayout, bodyLayout);
        setFlexGrow(1, bodyLayout); // locks bodyLayout directly to view-port bottom limits.
    }

    // ADD/UPDATE line.
    private void addProductToGrid(ProductPojo product) {
        // if same company+name already in bill -> increase qty by 1.
        listBillRow.stream()
                .filter(r -> r.getName().equals(product.getName()) &&
                             r.getCompany().equals(product.getCompany()))
                .findFirst()
                .ifPresentOrElse(productExist -> {
                    productExist.setQty(productExist.getQty() + 1L);
                    dataProvider.refreshAll();
                    refreshTotal();
                    Notify.success("updated [" + product.getName() + "] qty : " + productExist.getQty());
                    saveTabStateToSession();
                }, () -> {
                    BillRow row = new BillRow();
                    row.setCompany(product.getCompany());
                    row.setName(product.getName());
                    row.setMrp(product.getMrp() != null ? product.getMrp() : BigDecimal.ZERO);
                    row.setQty(1L);
                    row.setDiscount(BigDecimal.ZERO); // default : no discount.
                    row.setGstRate(GST_18);           // default : 18% (CGST 9% + SGST 9%).
                    listBillRow.add(row);
                    renumberLines();
                    dataProvider.refreshAll();
                    refreshTotal();
                    Notify.success("added [" + product.getName()+"]");
                    saveTabStateToSession();
                });
    }

    private void renumberLines() {
        for (int i = 0; i < listBillRow.size(); i++) {
            listBillRow.get(i).setLineNo((long) (i + 1));
        }
    }

    private void removeLine(BillRow row) {
        listBillRow.remove(row);
        // if it was the last line, explicitly clear the grid's selection state.
        if (listBillRow.isEmpty()) {
            billGrid.deselectAll();
            if (headerTrashBtn != null) {
                headerTrashBtn.setEnabled(false);
            }
        }
        renumberLines();
        dataProvider.refreshAll();
        refreshTotal();
        Notify.success("removed [" + row.getName() + "]");
        saveTabStateToSession();
    }    

    private void handleDeleteSelected() {
        Set<BillRow> selected = billGrid.getSelectedItems();
        int count = selected.size();

        if (count == 0) {
            Notify.error("select at least one product to delete");
            return;
        }

        Button deleteBtn = new Button("delete");
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        deleteBtn.getElement().getStyle().set("cursor", "pointer");

        Button cancelBtn = new Button("cancel");
        cancelBtn.getElement().getStyle().set("cursor", "pointer");

        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader("confirm delete");
        confirmDialog.setText(String.format("remove selected %d line(s) from the bill?", count));
        confirmDialog.setCancelable(true);
        confirmDialog.setConfirmButton(deleteBtn);
        confirmDialog.setCancelButton(cancelBtn);

        confirmDialog.addOpenedChangeListener(event -> {
            if (event.isOpened()) {
                cancelBtn.getElement().executeJs("setTimeout(() => this.focus(), 100)");
            }
        });

        confirmDialog.addConfirmListener(confirmEvent -> {
            listBillRow.removeAll(selected);
            // sometimes fails to trigger a visual header refresh if the items it is trying to clear have already been detached or removed from the list.
            // billGrid.asMultiSelect().clear();
            // forces Vaadin to reset the internal client-side state machine of the grid component, which explicitly unchecks the top "Select All" master box every single time.
            billGrid.deselectAll();
            if (headerTrashBtn != null) headerTrashBtn.setEnabled(false);
            renumberLines();
            dataProvider.refreshAll();
            refreshTotal();
            Notify.success(count + " product(s) removed");
            confirmDialog.close();
            saveTabStateToSession();
        });

        confirmDialog.open();
    }

    private void handleClearBill() {        

        if (listBillRow.isEmpty()) {
            // clearFields();
            saveTabStateToSession(); // persists the cleared mobile number to the session wrapper.
            Notify.error("no product(s) to clear");
            return;
        }

        Button clearBtn = new Button("clear");
        clearBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        clearBtn.getElement().getStyle().set("cursor", "pointer");

        Button cancelBtn = new Button("cancel");
        cancelBtn.getElement().getStyle().set("cursor", "pointer");

        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader("clear bill");
        confirmDialog.setText("remove all " + listBillRow.size() + " line(s), continue?");
        confirmDialog.setCancelable(true);
        confirmDialog.setConfirmButton(clearBtn);
        confirmDialog.setCancelButton(cancelBtn);

        confirmDialog.addOpenedChangeListener(event -> {
            if (event.isOpened()) {
                cancelBtn.getElement().executeJs("setTimeout(() => this.focus(), 100)");
            }
        });

        confirmDialog.addConfirmListener(confirmEvent -> {
            listBillRow.clear();            
            currentOrdNo = null;
            // clear the grid selection (this resets the header checkbox).
            billGrid.deselectAll();
            if (headerTrashBtn != null) headerTrashBtn.setEnabled(false);
            dataProvider.refreshAll();
            refreshTotal();
            Notify.success("bill cleared");
            confirmDialog.close();
            // clearFields();
            saveTabStateToSession();
        });

        confirmDialog.open();
    }

    private void handleSaveOrder() {
        if (listBillRow.isEmpty()) {
            Notify.error("bill is empty, add product(s)");
            return;
        }

        try {
            OrdHdr ordHdr = new OrdHdr();
            // ordNo : generate if new bill, reuse if editing existing order.
            if (currentOrdNo == null) {
                // currentOrdNo = orderFacade.generateNextOrdNo(); // implement in OrderFacade
                currentOrdNo = 1001L;
                logger.info("orderFacade.generateNextOrdNo() : [" + currentOrdNo + "]");
            }
            ordHdr.setOrdNo(currentOrdNo);
            ordHdr.setMobile(currentMobile != null ? currentMobile : 0L);

            List<OrdDtl> listOrdDtl = new ArrayList<>();
            for (BillRow row : listBillRow) {
                OrdDtl ordDtl = new OrdDtl();
                ordDtl.setLineNo(row.getLineNo());
                ordDtl.setCompany(row.getCompany());
                ordDtl.setName(row.getName());
                ordDtl.setQty(row.getQty());
                listOrdDtl.add(ordDtl);
            }

            OrderPojo orderPojo = new OrderPojo();
            orderPojo.setOrdHdr(ordHdr);
            orderPojo.setOrdDtl(listOrdDtl);

            orderFacade.saveOrder(orderPojo);

            Notify.success("created order # " + currentOrdNo + " with " + listBillRow.size() + " product(s)");

            // FLUSH COMPLETED DATA.
            listBillRow.clear();
            currentOrdNo = null;
            billGrid.deselectAll();
            dataProvider.refreshAll();
            refreshTotal();
            clearFields();
            saveTabStateToSession(); // persists the freshly wiped grid state to session cache.
        } catch (Exception e) {
            Notify.error(e);
        }
    }

    private void handlePrintBill() {
        if (listBillRow.isEmpty()) {
            Notify.error("bill is empty, add product(s)");
            return;
        }

        BigDecimal grossTotal    = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalGst      = BigDecimal.ZERO;

        for (BillRow r : listBillRow) {
            grossTotal    = grossTotal.add(r.getGrossAmount());
            totalDiscount = totalDiscount.add(r.getDiscountAmount());
            totalGst      = totalGst.add(r.getLineTotalGst());
        }

        BigDecimal netPayable  = grossTotal.subtract(totalDiscount);
        // BigDecimal taxableBase = netPayable.subtract(totalGst);
        BigDecimal taxableBase = netPayable.divide(BigDecimal.ONE.add(GST_18), 2, RoundingMode.HALF_UP);
        // BigDecimal cgst        = totalGst.divide(BigDecimal.TWO, 2, RoundingMode.HALF_UP);        
        // BigDecimal sgst        = totalGst.subtract(cgst);
        // calculate CGST and SGST individually at half the total rate (9% each).
        BigDecimal halfRate = GST_18.divide(BigDecimal.TWO);
        BigDecimal cgst = taxableBase.multiply(halfRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal sgst = cgst; // formally forces CGST and SGST to be mathematically identical.

        String billDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
        Long   ordNo    = currentOrdNo != null ? currentOrdNo : 0L;
        Long   mobile   = currentMobile != null ? currentMobile : 0L;

        // build HTML bill.
        StringBuilder rows = new StringBuilder();
        int sno = 1;
        for (BillRow r : listBillRow) {
            String discStr = r.getDiscount() != null && r.getDiscount().compareTo(BigDecimal.ZERO) > 0
                ? fmt(r.getDiscount()) + "%" : "—";
            rows.append("<tr>")
                .append("<td>").append(sno++).append("</td>")
                .append("<td>").append(r.getCompany()).append("</td>")
                .append("<td>").append(r.getName()).append("</td>")
                .append("<td class='num'>").append(r.getQty()).append("</td>")
                .append("<td class='num'>₹ ").append(fmt(r.getMrp())).append("</td>")
                .append("<td class='num'>").append(discStr).append("</td>")
                .append("<td class='num'>").append(r.getGstLabel()).append("</td>")
                .append("<td class='num'>₹ ").append(fmt(r.getSubTotal())).append("</td>")
                .append("</tr>");
        }

        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'/>"
            + "<title>Tax Invoice</title>"
            + "<style>"
            + "  *{box-sizing:border-box;margin:0;padding:0}"
            + "  body{font-family:'Courier New',monospace;font-size:12px;color:#111;background:#fff;padding:16px}"
            + "  .bill{max-width:740px;margin:0 auto;border:2px solid #333;padding:12px}"
            + "  .shop-name{font-size:20px;font-weight:bold;text-align:center;letter-spacing:1px}"
            + "  .shop-address{font-size:11px;text-align:center;color:#555}"
            + "  .divider{border-top:1px dashed #555;margin:8px 0}"
            + "  .bill-meta{display:flex;justify-content:space-between;font-size:11px;margin:6px 0}"
            + "  table{width:100%;border-collapse:collapse;margin-top:6px;font-size:11px}"
            + "  th{background:#222;color:#fff;padding:4px 6px;text-align:left}"
            + "  th.num,td.num{text-align:right}"
            + "  td{padding:4px 6px;border-bottom:1px solid #ddd}"
            + "  tr:nth-child(even){background:#f7f7f7}"
            + "  .totals{margin-top:10px;text-align:right;font-size:12px;line-height:1.8}"
            + "  .totals table{width:260px;margin-left:auto}"
            + "  .totals td{border:none;padding:2px 6px}"
            + "  .grand{font-size:15px;font-weight:bold;border-top:2px solid #333!important}"
            + "  .footer{font-size:10px;text-align:center;margin-top:12px;color:#555}"
            + "  .gstin{font-size:10px;text-align:center;letter-spacing:.5px}"
            + "  @media print{body{padding:0}.bill{border:none}button{display:none}}"
            + "</style></head><body>"
            + "<div class='bill'>"
            + "  <div class='shop-name'>SHOPKEEPER</div>"
            + "  <div class='shop-address'># 1, road name, city — 600001 | mobile : 98765-43210</div>"
            + "  <div class='gstin'>GSTIN: 33AABCS1429B1ZB &nbsp;|&nbsp; state : Tamil Nadu (33)</div>"
            + "  <div class='divider'></div>"
            + "  <div style='text-align:center;font-weight:bold;letter-spacing:1px'>TAX INVOICE</div>"
            + "  <div class='divider'></div>"
            + "  <div class='bill-meta'>"
            + "    <span>Bill No: <b>" + ordNo + "</b></span>"
            + "    <span>Date: <b>" + billDate + "</b></span>"
            + "    <span>Mobile: <b>" + (mobile > 0 ? mobile : "—") + "</b></span>"
            + "  </div>"
            + "  <table>"
            + "    <thead><tr>"
            + "      <th>#</th><th>company</th><th>item</th>"
            + "      <th class='num'>qty</th><th class='num'>MRP (₹)</th>"
            + "      <th class='num'>discount</th><th class='num'>GST</th>"
            + "      <th class='num'>subtotal (₹)</th>"
            + "    </tr></thead>"
            + "    <tbody>" + rows + "</tbody>"
            + "  </table>"
            + "  <div class='totals'><table>"
            + "    <tr><td>Gross Total</td><td class='num'>₹ " + fmt(grossTotal) + "</td></tr>"
            + "    <tr><td>Discount (−)</td><td class='num'>₹ " + fmt(totalDiscount) + "</td></tr>"
            + "    <tr><td>Taxable Value</td><td class='num'>₹ " + fmt(taxableBase) + "</td></tr>"
            + "    <tr><td>CGST (½ GST)</td><td class='num'>₹ " + fmt(cgst) + "</td></tr>"
            + "    <tr><td>SGST (½ GST)</td><td class='num'>₹ " + fmt(sgst) + "</td></tr>"
            + "    <tr class='grand'><td><b>Net Payable</b></td><td class='num'><b>₹ " + fmt(netPayable) + "</b></td></tr>"
            + "  </table></div>"
            + "  <div class='divider'></div>"
            + "  <div class='footer'>thank you for shopping with us!</div>"
            + "</div>"
            + "<br/><div style='text-align:center'>"
            + "  <button onclick='window.print()' style='padding:8px 24px;font-size:13px;cursor:pointer'>🖨 Print Bill</button>"
            + "</div>"
            + "</body></html>";

        // show in a Vaadin Dialog (full-screen).
        Dialog printDialog = new Dialog();
        printDialog.setWidth("800px");
        printDialog.setHeight("90vh");
        printDialog.setDraggable(true);
        printDialog.setResizable(true);

        H2 dlgTitle = new H2("bill preview");
        dlgTitle.addClassNames(LumoUtility.FontSize.MEDIUM);
        printDialog.getHeader().add(dlgTitle);

        // embed the HTML bill via an iframe (safe, print-isolated).
        String escapedHtml = html.replace("\\", "\\\\").replace("`", "\\`").replace("$", "\\$");
        Div iframeContainer = new Div();
        iframeContainer.setWidthFull();
        iframeContainer.setHeightFull();
        iframeContainer.getElement().executeJs(
            "const f = document.createElement('iframe');" +
            "f.style.width='100%';f.style.height='560px';f.style.border='none';" +
            "this.appendChild(f);" +
            "f.contentDocument.open();" +
            "f.contentDocument.write(`" + escapedHtml + "`);" +
            "f.contentDocument.close();"
        );
        printDialog.add(iframeContainer);

        Button closeDlgBtn = new Button("close", e -> printDialog.close());
        closeDlgBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        printDialog.getFooter().add(closeDlgBtn);

        printDialog.open();
    }

    private void refreshTotal() {
        // aggregate across all lines (each line may have a different GST slab + discount).
        BigDecimal grossTotal    = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalGst      = BigDecimal.ZERO;

        for (BillRow r : listBillRow) {
            grossTotal    = grossTotal.add(r.getGrossAmount());
            totalDiscount = totalDiscount.add(r.getDiscountAmount());
            totalGst      = totalGst.add(r.getLineTotalGst());
        }

        BigDecimal netPayable = grossTotal.subtract(totalDiscount);
        // BigDecimal taxableBase = netPayable.subtract(totalGst);
        // using a 1.18 factor as a fallback baseline or extracting it per line item.
        BigDecimal taxableBase = netPayable.divide(BigDecimal.ONE.add(GST_18), 2, RoundingMode.HALF_UP);


        // split GST equally between CGST & SGST (intra-state supply).
        // BigDecimal cgst = totalGst.divide(BigDecimal.TWO, 2, RoundingMode.HALF_UP);
        // BigDecimal sgst = totalGst.subtract(cgst);

        // calculate CGST and SGST individually at half the total rate (9% each)
        BigDecimal halfRate = GST_18.divide(BigDecimal.TWO);
        BigDecimal cgst = taxableBase.multiply(halfRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal sgst = cgst; // formally forces CGST and SGST to be mathematically identical.

        updateTotalRow(grossTotalSpan,  "gross total",  "₹ " + fmt(grossTotal),     false);
        updateTotalRow(discountSpan,    "discount (-)", "₹ " + fmt(totalDiscount),  false);
        updateTotalRow(cgstSpan,        "CGST (½ GST)", "₹ " + fmt(cgst),           false);
        updateTotalRow(sgstSpan,        "SGST (½ GST)", "₹ " + fmt(sgst),           false);
        updateTotalRow(netPayableSpan,  "net payable",  "₹ " + fmt(netPayable),     true);
    }

    // WebSocket BARCODE scanner (USB/Bluetooth)
    private void connectWebSocket(ComboBox<ProductPojo> searchBox) {
        registeredUi = UI.getCurrent();

        barcodeWebSocketHandler.registerConsumer(sessionId, barcode -> {
            registeredUi.access(() -> {
                try {
                    List<ProductPojo> results = productFacade.searchProduct(barcode.trim().toLowerCase());
                    if (!results.isEmpty()) {
                        addProductToGrid(results.get(0)); // auto-add first match.
                    } else {
                        Notify.error("product not found for barcode : [" + barcode + "]");
                    }
                } catch (Exception ex) {
                    Notify.error(ex);
                }
            });
        });
    }

    // QR scanner.
    private void openScannerDialog(ComboBox<ProductPojo> searchBox) {
        Dialog dialog = new Dialog();
        dialog.setWidth("350px");

        H2 title = new H2("scan this QR with mobile");
        title.addClassNames(
            LumoUtility.TextAlignment.CENTER,
            LumoUtility.Width.FULL,
            LumoUtility.Margin.LARGE,
            LumoUtility.FontSize.LARGE);
        
        dialog.getHeader().add(title);

        VerticalLayout layout = new VerticalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        layout.setPadding(true);
        
        dialog.add(layout);

        try {
            // String serverIp  = java.net.InetAddress.getLocalHost().getHostAddress();
            String serverIp  = getLocalIp();
            String mobileUrl = "http://" + serverIp + ":8080/scan/" + sessionId;
            logger.info("QR mobile URL : {}", mobileUrl);

            byte[] qrBytes = qrCodeService.generateQrCode(mobileUrl, 220, 220);
            String base64  = java.util.Base64.getEncoder().encodeToString(qrBytes);

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
            Paragraph hint3 = new Paragraph("3. product will auto-add to bill");

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
            dialog.add(new Paragraph("could not generate QR : " + ex.getMessage()));
        }

        dialog.open();
    }

    // onDetach - clean up WebSocket consumer.
    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        barcodeWebSocketHandler.unregisterConsumer(sessionId);
    }

    // create a monospace font for total panel.
    private Span monoSpan(String label, String value, boolean isBold) {
        Span container = new Span();
        container.setWidthFull();
        container.addClassNames(LumoUtility.Padding.Vertical.XSMALL);
        updateTotalRow(container, label, value, isBold);
        return container;
    }

    private String fmt(BigDecimal val) {
        return val.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private void updateTotalRow(Span container, String label, String value, boolean isBold) {
        container.removeAll();
        
        Span labelSpan = new Span(label);
        Span valueSpan = new Span(value);
        
        // labelSpan.getStyle().set("font-family", "monospace");
        // valueSpan.getStyle().set("font-family", "monospace");
        labelSpan.addClassName("bill_total_mono");
        valueSpan.addClassName("bill_total_mono");

        if (isBold) {            
            labelSpan.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.LARGE);
            valueSpan.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.LARGE);
        } else {
            labelSpan.addClassNames(LumoUtility.FontSize.SMALL);
            valueSpan.addClassNames(LumoUtility.FontSize.SMALL);
        }

        HorizontalLayout rowLayout = new HorizontalLayout(labelSpan, valueSpan);
        rowLayout.setWidthFull();
        rowLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        
        container.add(rowLayout);
    }

    // helper method to safely synchronize the Redis hit.
    private List<ProductPojo> getRedisCachedProduct(String searchText) {
        if (!searchText.equals(lastProductSearchText)) {
            lastProductSearchText = searchText;
            if (searchText.isEmpty()) {
                redisCachedProductResult = new ArrayList<>();
            } else {
                try {
                    // single Redis hit happens here only when the text changes.
                    redisCachedProductResult = productFacade.searchProduct(searchText);
                } catch (Exception ex) {
                    Notify.error(ex);
                    redisCachedProductResult = new ArrayList<>();
                }
            }
        }
        return redisCachedProductResult;
    }

    private String getLocalIp() {
        try {
            try (java.net.DatagramSocket socket = new java.net.DatagramSocket()) {
                socket.connect(java.net.InetAddress.getByName("8.8.8.8"), 80);
                return socket.getLocalAddress().getHostAddress();
            }
        } catch (Exception e) {
            return "localhost";
        }
    }

    @SuppressWarnings("unchecked")
    private void restoreTabState() {

        try {
            // set flag to true to lock listeners.
            this.isRestoring = true;
                    
            List<BillRow> savedRows = (List<BillRow>) VaadinSession.getCurrent().getAttribute(tabSessionKey);
            Long savedMobile = (Long) VaadinSession.getCurrent().getAttribute(tabSessionKey + "_MOBILE");        
            
            if (savedRows != null && !savedRows.isEmpty()) {
                // clear the current memory array and load the preserved session grid.
                this.listBillRow.clear();
                this.listBillRow.addAll(savedRows);
                this.renumberLines();
                this.dataProvider.refreshAll();
                this.refreshTotal();
            }

            if (savedMobile != null) {
                this.currentMobile = savedMobile;
                if (this.mobileField != null) {
                    this.mobileField.setValue(String.valueOf(savedMobile));
                }
            }
        } finally {
            // ALWAYS turn it off when done so user typing works normally.
            this.isRestoring = false;
        }        
    }

    // call this helper method anytime a change happens (add product, delete product, change qty).
    private void saveTabStateToSession() {
        if (tabSessionKey != null) {
            VaadinSession.getCurrent().setAttribute(tabSessionKey, new ArrayList<>(listBillRow));
            // to save the current mobile number.
            VaadinSession.getCurrent().setAttribute(tabSessionKey + "_MOBILE", currentMobile);
        }
    }

    private void clearFields() {
        // ALWAYS clear the mobile variables and the TextField first, even if grid is empty.
        this.currentMobile = null;
        if (this.mobileField != null) {
            this.mobileField.clear(); // this sets it back to empty text.
        }
    }
}

// Storage Type             VaadinSession (Current setup)   Browser sessionStorage   Browser localStorage
// Survives Refresh (F5)?   Yes                             Yes                      Yes
// Survives App Restart?    No (goes to login)              Yes                      Yes   
// Shared Across Tabs?      No (isolated by your key)       No (perfect for tabs)    Yes (will pollute other tabs)