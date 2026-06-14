package com.example.receipt.dto;

import java.math.BigDecimal;
public class ReceiptProcessResponse {

    private String description;
    private BigDecimal amount;
    private String date;
    private String category;

    public ReceiptProcessResponse() {
    }

    public ReceiptProcessResponse(String description, BigDecimal amount, String date, String category) {
        this.description = description;
        this.amount = amount;
        this.date = date;
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
