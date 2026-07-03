package com.example.receipt.service;

import com.azure.ai.documentintelligence.DocumentIntelligenceClient;
import com.azure.ai.documentintelligence.models.AnalyzeDocumentOptions;
import com.azure.ai.documentintelligence.models.AnalyzeOperationDetails;
import com.azure.ai.documentintelligence.models.AnalyzeResult;
import com.azure.ai.documentintelligence.models.AnalyzedDocument;
import com.azure.ai.documentintelligence.models.CurrencyValue;
import com.azure.ai.documentintelligence.models.DocumentField;
import com.azure.core.util.polling.SyncPoller;
import com.example.receipt.dto.ReceiptProcessResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AzureDocumentReceiptProcessorTest {

    private DocumentIntelligenceClient client;
    private SyncPoller<AnalyzeOperationDetails, AnalyzeResult> poller;
    private AnalyzeResult analyzeResult;
    private AnalyzedDocument analyzedDocument;
    private AzureDocumentReceiptProcessor processor;

    @BeforeEach
    void setUp() {
        client = mock(DocumentIntelligenceClient.class);
        poller = mock(SyncPoller.class);
        analyzeResult = mock(AnalyzeResult.class);
        analyzedDocument = mock(AnalyzedDocument.class);
        processor = new AzureDocumentReceiptProcessor(client);

        when(client.beginAnalyzeDocument(eq("prebuilt-receipt"), any(AnalyzeDocumentOptions.class)))
                .thenReturn(poller);
        when(poller.getFinalResult()).thenReturn(analyzeResult);
    }

    @Test
    void processReceipt_withCurrencyTotal_shouldExtractCorrectly() {
        when(analyzeResult.getDocuments()).thenReturn(List.of(analyzedDocument));
        Map<String, DocumentField> fields = new HashMap<>();

        DocumentField merchantField = mock(DocumentField.class);
        when(merchantField.getContent()).thenReturn("Starbucks");
        fields.put("MerchantName", merchantField);

        DocumentField totalField = mock(DocumentField.class);
        CurrencyValue currencyValue = mock(CurrencyValue.class);
        when(currencyValue.getAmount()).thenReturn(15.50);
        when(totalField.getValueCurrency()).thenReturn(currencyValue);
        fields.put("Total", totalField);

        DocumentField dateField = mock(DocumentField.class);
        LocalDate transactionDate = LocalDate.of(2026, 5, 21);
        when(dateField.getValueDate()).thenReturn(transactionDate);
        fields.put("TransactionDate", dateField);

        DocumentField receiptTypeField = mock(DocumentField.class);
        when(receiptTypeField.getContent()).thenReturn("Meal");
        fields.put("ReceiptType", receiptTypeField);

        DocumentField itemsField = mock(DocumentField.class);
        DocumentField itemField = mock(DocumentField.class);
        DocumentField itemDescriptionField = mock(DocumentField.class);
        when(itemDescriptionField.getContent()).thenReturn("Latte");
        when(itemField.getValueMap()).thenReturn(Map.of("Description", itemDescriptionField));
        when(itemsField.getValueList()).thenReturn(List.of(itemField));
        fields.put("Items", itemsField);

        when(analyzedDocument.getFields()).thenReturn(fields);

        ReceiptProcessResponse response = processor.processReceipt(new byte[]{1, 2, 3}, "test-receipt.jpg");

        assertNotNull(response);
        assertEquals("Starbucks", response.getDescription());
        assertEquals(0, new BigDecimal("15.50").compareTo(response.getAmount()));
        assertEquals("2026-05-21", response.getDate());
        assertEquals("Other", response.getCategory());
        assertEquals("Meal", response.getReceiptType());
        assertEquals(List.of("Latte"), response.getItemDescriptions());
    }

    @Test
    void processReceipt_withNumberTotal_shouldExtractCorrectly() {
        when(analyzeResult.getDocuments()).thenReturn(List.of(analyzedDocument));
        Map<String, DocumentField> fields = new HashMap<>();

        DocumentField totalField = mock(DocumentField.class);
        when(totalField.getValueCurrency()).thenReturn(null);
        when(totalField.getValueNumber()).thenReturn(25.75);
        fields.put("Total", totalField);

        when(analyzedDocument.getFields()).thenReturn(fields);

        ReceiptProcessResponse response = processor.processReceipt(new byte[]{1, 2, 3}, "test-receipt.jpg");

        assertNotNull(response);
        assertEquals("Unknown Merchant", response.getDescription());
        assertEquals(0, new BigDecimal("25.75").compareTo(response.getAmount()));
        assertEquals("Other", response.getCategory());
    }

    @Test
    void processReceipt_withNullOrMissingFields_shouldUseDefaults() {
        when(analyzeResult.getDocuments()).thenReturn(List.of(analyzedDocument));
        Map<String, DocumentField> fields = new HashMap<>();
        fields.put("MerchantName", null);
        fields.put("Total", null);
        fields.put("TransactionDate", null);

        when(analyzedDocument.getFields()).thenReturn(fields);

        ReceiptProcessResponse response = processor.processReceipt(new byte[]{1, 2, 3}, "test-receipt.jpg");

        assertNotNull(response);
        assertEquals("Unknown Merchant", response.getDescription());
        assertEquals(BigDecimal.ZERO, response.getAmount());
        assertEquals(LocalDate.now().toString(), response.getDate());
        assertEquals("Other", response.getCategory());
    }
}
