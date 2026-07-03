package com.example.receipt.dto;

import java.math.BigDecimal;
import java.util.List;

public class ReceiptProcessResponse {

    private String description;
    private BigDecimal amount;
    private String date;
    private String category;
    private String receiptType;
    private List<String> itemDescriptions;

    public ReceiptProcessResponse() {
    }

    public ReceiptProcessResponse(String description, BigDecimal amount, String date, String category) {
        this(description, amount, date, category, null, List.of());
    }

    public ReceiptProcessResponse(
            String description,
            BigDecimal amount,
            String date,
            String category,
            String receiptType,
            List<String> itemDescriptions) {
        this.description = description;
        this.amount = amount;
        this.date = date;
        this.category = category;
        this.receiptType = receiptType;
        this.itemDescriptions = itemDescriptions;
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

    public String getReceiptType() {
        return receiptType;
    }

    public void setReceiptType(String receiptType) {
        this.receiptType = receiptType;
    }

    public List<String> getItemDescriptions() {
        return itemDescriptions;
    }

    public void setItemDescriptions(List<String> itemDescriptions) {
        this.itemDescriptions = itemDescriptions;
    }
}
