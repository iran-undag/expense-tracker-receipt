package com.example.receipt.service;

import com.azure.ai.documentintelligence.DocumentIntelligenceClient;
import com.azure.ai.documentintelligence.DocumentIntelligenceClientBuilder;
import com.azure.ai.documentintelligence.models.AnalyzeDocumentOptions;
import com.azure.ai.documentintelligence.models.AnalyzeOperationDetails;
import com.azure.ai.documentintelligence.models.AnalyzeResult;
import com.azure.ai.documentintelligence.models.AnalyzedDocument;
import com.azure.ai.documentintelligence.models.DocumentField;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import com.azure.core.util.polling.SyncPoller;
import com.example.receipt.dto.ReceiptProcessResponse;
import com.example.receipt.exception.ReceiptProcessingException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class AzureDocumentReceiptProcessor implements ReceiptProcessor {

    private final DocumentIntelligenceClient client;

    public AzureDocumentReceiptProcessor(String endpoint, String key) {
        this(new DocumentIntelligenceClientBuilder()
                .endpoint(endpoint)
                .credential(new AzureKeyCredential(key))
                .buildClient());
    }

    AzureDocumentReceiptProcessor(DocumentIntelligenceClient client) {
        this.client = client;
    }

    @Override
    public ReceiptProcessResponse processReceipt(byte[] image, String filename) {
        AnalyzeDocumentOptions options = new AnalyzeDocumentOptions(BinaryData.fromBytes(image));
        SyncPoller<AnalyzeOperationDetails, AnalyzeResult> poller = client.beginAnalyzeDocument(
                "prebuilt-receipt",
                options);

        AnalyzeResult result = poller.getFinalResult();
        if (result.getDocuments() == null || result.getDocuments().isEmpty()) {
            throw new ReceiptProcessingException("No documents found in the receipt image.");
        }

        AnalyzedDocument document = result.getDocuments().get(0);
        Map<String, DocumentField> fields = document.getFields();

        return new ReceiptProcessResponse(
                getStringField(fields, "MerchantName", "Unknown Merchant"),
                getTotal(fields),
                getDate(fields).toString(),
                getStringField(fields, "Category", "General"));
    }

    private String getStringField(Map<String, DocumentField> fields, String fieldName, String defaultValue) {
        if (fields == null || !fields.containsKey(fieldName) || fields.get(fieldName) == null) {
            return defaultValue;
        }

        String content = fields.get(fieldName).getContent();
        if (content == null || content.trim().isEmpty()) {
            return defaultValue;
        }
        return content;
    }

    private BigDecimal getTotal(Map<String, DocumentField> fields) {
        if (fields == null || !fields.containsKey("Total") || fields.get("Total") == null) {
            return BigDecimal.ZERO;
        }

        DocumentField totalField = fields.get("Total");
        if (totalField.getValueCurrency() != null) {
            return BigDecimal.valueOf(totalField.getValueCurrency().getAmount());
        }
        if (totalField.getValueNumber() != null) {
            return BigDecimal.valueOf(totalField.getValueNumber());
        }
        return BigDecimal.ZERO;
    }

    private LocalDate getDate(Map<String, DocumentField> fields) {
        if (fields == null || !fields.containsKey("TransactionDate") || fields.get("TransactionDate") == null) {
            return LocalDate.now();
        }

        LocalDate date = fields.get("TransactionDate").getValueDate();
        return date == null ? LocalDate.now() : date;
    }
}
