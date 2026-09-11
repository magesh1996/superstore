package com.superstore.app.view;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.superstore.app.facade.OrderFacade;
import com.superstore.app.pojo.OrderPojo;
import com.superstore.app.utility.Notify;
import com.superstore.app.view.filter.OrdHdrFilter;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;

@Route("order")
// @PageTitle("order")
@Menu(order = 3, title = "order")
@PermitAll
public class OrderView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(OrderView.class);

    private final OrderFacade orderFacade;
    
    public OrderView(OrderFacade orderFacade) {
        this.orderFacade = orderFacade;
    }

    @SuppressWarnings("null")
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        removeAll(); // VERY IMPORTANT (prevents duplicate UI on re-attach)

        setSizeFull();

        List<OrderPojo> listOrderPojo = new ArrayList<>();

        try {
            listOrderPojo = orderFacade.getAllOrder();
        } catch (Exception e) {
            // Notify.error(e);
            logger.error("failed to refresh order grid", e);
            Notify.error("service-order is down");
        }

        Grid<OrderPojo> ordHdrGrid = new Grid<>(OrderPojo.class, false);
        ordHdrGrid.addThemeVariants(GridVariant.LUMO_COMPACT,GridVariant.LUMO_COLUMN_BORDERS);
        ordHdrGrid.removeAllColumns();
        // Grid.Column<OrderPojo> ordNoColumn = ordHdrGrid.addColumn(ord -> ord.getOrdHdr().getOrdNo()).setHeader("Order");
        // Grid.Column<OrderPojo> mobileNoColumn = ordHdrGrid.addColumn(ord -> ord.getOrdHdr().getMobile()).setHeader("Mobile");
        Grid.Column<OrderPojo> ordNoColumn = ordHdrGrid.addColumn(ord -> ord.getOrdHdr().getOrdNo());
        Grid.Column<OrderPojo> mobileNoColumn = ordHdrGrid.addColumn(ord -> ord.getOrdHdr().getMobile());

        ordHdrGrid.setItemDetailsRenderer(
                new ComponentRenderer<>(ord -> {

                    Grid<OrderPojo.OrdDtl> ordDtlGrid = new Grid<>(OrderPojo.OrdDtl.class, false);
                    ordDtlGrid.addThemeVariants(GridVariant.LUMO_COMPACT,GridVariant.LUMO_COLUMN_BORDERS);
                    ordDtlGrid.removeAllColumns();
                    ordDtlGrid.addColumn(OrderPojo.OrdDtl::getLineNo).setHeader("line");
                    ordDtlGrid.addColumn(OrderPojo.OrdDtl::getCompany).setHeader("company");
                    ordDtlGrid.addColumn(OrderPojo.OrdDtl::getSku).setHeader("SKU");
                    ordDtlGrid.addColumn(OrderPojo.OrdDtl::getQty).setHeader("quantity");
                    
                    ordDtlGrid.setItems(ord.getOrdDtl());
                    // setAllRowsVisible prevents the detail grid from collapsing to 0px and it auto-calculates its own height inside the expanded view.
                    ordDtlGrid.setAllRowsVisible(true);
                    ordDtlGrid.setWidthFull();
                    return ordDtlGrid;
                })
        );
        ordHdrGrid.setDetailsVisibleOnClick(true);

        // ordHdrGrid.setItems(listOrderPojo);
        // ordHdrGrid.setWidthFull();
        // ordHdrGrid.setSizeFull();
        // add(ordHdrGrid);

        GridListDataView<OrderPojo> dataView = ordHdrGrid.setItems(listOrderPojo);
        OrdHdrFilter ordHdrFilter = new OrdHdrFilter(dataView);

        HeaderRow headerRow = ordHdrGrid.appendHeaderRow();
        headerRow.getCell(ordNoColumn).setComponent(createFilterHeader("order","search", ordHdrFilter::setOrdNo));
        headerRow.getCell(mobileNoColumn).setComponent(createFilterHeader("mobile","search", ordHdrFilter::setMobileNo));

        ordHdrGrid.setWidthFull();
        ordHdrGrid.setSizeFull();
        add(ordHdrGrid);
    }

    private static Component createFilterHeader(String label, String placeholder, Consumer<String> filterChangeConsumer)
    {
        TextField textField = new TextField(label);
        textField.setPlaceholder(placeholder);
        textField.setValueChangeMode(ValueChangeMode.EAGER);
        textField.setClearButtonVisible(true);
        textField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        textField.addValueChangeListener(e -> filterChangeConsumer.accept(e.getValue()));
        return textField;
    }
}
