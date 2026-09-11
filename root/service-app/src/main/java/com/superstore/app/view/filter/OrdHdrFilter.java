package com.superstore.app.view.filter;

import com.superstore.app.pojo.OrderPojo;
import com.vaadin.flow.component.grid.dataview.GridListDataView;

public class OrdHdrFilter{
    private String ordNo;
    private String mobileNo;

    private final GridListDataView<OrderPojo> ordHdrDataView;

    public OrdHdrFilter(GridListDataView<OrderPojo> ordHdrDataView) {
        this.ordHdrDataView = ordHdrDataView;
        this.ordHdrDataView.addFilter(this::check);
    }

    public void setOrdNo(String ordNo) {
        this.ordNo = ordNo;
        ordHdrDataView.refreshAll();
    }

    public void setMobileNo(String mobileNo) {
        this.mobileNo = mobileNo;
        ordHdrDataView.refreshAll();
    }

    private boolean check(OrderPojo ordHdr) {
        boolean matchesOrdNo = matches(String.valueOf(ordHdr.getOrdHdr().getOrdNo()), ordNo);
        boolean matchesMobileNo = matches(String.valueOf(ordHdr.getOrdHdr().getMobile()), mobileNo);
        return matchesOrdNo && matchesMobileNo;
    }

    private boolean matches(String value, String searchTerm) {
        return searchTerm == null || searchTerm.isEmpty() || value.toLowerCase().contains(searchTerm.toLowerCase());
    }

}