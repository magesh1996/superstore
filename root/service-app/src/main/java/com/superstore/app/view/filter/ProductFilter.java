package com.superstore.app.view.filter;

import com.superstore.app.pojo.ProductPojo;
import com.vaadin.flow.component.grid.dataview.GridListDataView;

public class ProductFilter{
    private String company;
    private String sku;
    private String name;

    private GridListDataView<ProductPojo> productDataView;

    public ProductFilter(GridListDataView<ProductPojo> productDataView) {
        this.productDataView = productDataView;
        this.productDataView.addFilter(this::check);
    }

    // re-bind to new dataView after grid refresh.
    public void setDataView(GridListDataView<ProductPojo> productDataView) {
        this.productDataView = productDataView;
        this.productDataView.addFilter(this::check);
    }

    public void setCompany(String company) {
        this.company = company;
        productDataView.refreshAll();
    }

    public void setSku(String sku) {
        this.sku = sku;
        productDataView.refreshAll();
    }

    public void setName(String name) {
        this.name = name;
        productDataView.refreshAll();
    }

    private boolean check(ProductPojo productPojo) {
        boolean matchesCompany = matches(String.valueOf(productPojo.getCompany()), company);
        boolean matchesSku = matches(String.valueOf(productPojo.getSku()), sku);
        boolean matchesName = matches(String.valueOf(productPojo.getName()), name);
        return matchesSku && matchesName && matchesCompany;
    }

    private boolean matches(String value, String searchTerm) {
        return searchTerm == null || searchTerm.isEmpty() || value.toLowerCase().contains(searchTerm.toLowerCase());
    }

}