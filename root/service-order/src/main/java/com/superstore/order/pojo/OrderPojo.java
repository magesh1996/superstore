package com.superstore.order.pojo;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "ordHdr", "ordDtl" })
public class OrderPojo
{
    public static class OrdHdr {

        private Long ordNo;
        private Long mobile;

        public Long getOrdNo() {
            return ordNo;
        }

        public void setOrdNo(Long ordNo) {
            this.ordNo = ordNo;
        }

        public Long getMobile() {
            return mobile;
        }

        public void setMobile(Long mobile) {
            this.mobile = mobile;
        }
    }

    private OrdHdr ordHdr;
    
    public OrdHdr getOrdHdr() {
        return ordHdr;
    }

    public void setOrdHdr(OrdHdr ordHdr) {
        this.ordHdr = ordHdr;
    }

    public static class OrdDtl {

        private Long lineNo;
        private String company;
        private String sku;
        private String name;
        private Long qty;

        public Long getLineNo() {
            return lineNo;
        }

        public void setLineNo(Long lineNo) {
            this.lineNo = lineNo;
        }

        public String getCompany() {
            return company;
        }

        public void setCompany(String company) {
            this.company = company;
        }

        public String getSku() {
            return sku;
        }

        public void setSku(String sku) {
            this.sku = sku;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Long getQty() {
            return qty;
        }

        public void setQty(Long qty) {
            this.qty = qty;
        }
    }

    private List<OrdDtl> ordDtl;

    public List<OrdDtl> getOrdDtl() {
        return ordDtl;
    }

    public void setOrdDtl(List<OrdDtl> ordDtl) {
        this.ordDtl = ordDtl;
    }
}