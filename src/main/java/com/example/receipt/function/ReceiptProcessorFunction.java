package com.example.receipt.function;

import com.example.receipt.dto.ReceiptProcessResponse;
import com.example.receipt.exception.ReceiptProcessingException;
import com.example.receipt.service.AzureDocumentReceiptProcessor;
import com.example.receipt.service.ReceiptProcessor;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class ReceiptProcessorFunction {

    private static final String FILE_NAME_HEADER = "x-file-name";

    private final ReceiptProcessor receiptProcessor;

    public ReceiptProcessorFunction() {
        this(new AzureDocumentReceiptProcessor(
                requiredEnv("AZURE_DOCUMENT_AI_ENDPOINT"),
                requiredEnv("AZURE_DOCUMENT_AI_KEY")));
    }

    ReceiptProcessorFunction(ReceiptProcessor receiptProcessor) {
        this.receiptProcessor = receiptProcessor;
    }

    @FunctionName("processReceipt")
    public HttpResponseMessage processReceipt(
            @HttpTrigger(
                    name = "request",
                    methods = {HttpMethod.POST},
                    authLevel = AuthorizationLevel.FUNCTION,
                    route = "process-receipt")
            HttpRequestMessage<Optional<byte[]>> request,
            ExecutionContext context) {

        Optional<byte[]> body = request.getBody();
        if (body.isEmpty() || body.get().length == 0) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Receipt image body is required")
                    .build();
        }

        try {
            ReceiptProcessResponse response = receiptProcessor.processReceipt(body.get(), getFilename(request));
            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(response)
                    .build();
        } catch (ReceiptProcessingException e) {
            if (context != null) {
                context.getLogger().warning(e.getMessage());
            }
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .header("Content-Type", "application/json")
                    .body(Map.of(
                            "error", "Receipt Processing Failed",
                            "message", e.getMessage()))
                    .build();
        }
    }

    private String getFilename(HttpRequestMessage<?> request) {
        return request.getHeaders().entrySet().stream()
                .filter(entry -> FILE_NAME_HEADER.equals(entry.getKey().toLowerCase(Locale.ROOT)))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be set");
        }
        return value;
    }
}
