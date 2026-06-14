package com.example.receipt.function;

import com.example.receipt.dto.ReceiptProcessResponse;
import com.example.receipt.exception.ReceiptProcessingException;
import com.example.receipt.service.ReceiptProcessor;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.HttpStatusType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ReceiptProcessorFunctionTest {

    @Test
    void processReceipt_withImageBody_shouldReturnReceiptJson() {
        byte[] image = new byte[]{1, 2, 3};
        CapturingReceiptProcessor processor = new CapturingReceiptProcessor();
        ReceiptProcessorFunction function = new ReceiptProcessorFunction(processor);
        TestHttpRequest request = new TestHttpRequest(Optional.of(image), Map.of("X-File-Name", "receipt.jpg"));

        HttpResponseMessage response = function.processReceipt(request, null);

        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals("application/json", response.getHeader("Content-Type"));
        assertEquals("receipt.jpg", processor.filename);
        assertArrayEquals(image, processor.image);

        ReceiptProcessResponse body = (ReceiptProcessResponse) response.getBody();
        assertEquals("Store", body.getDescription());
        assertEquals(0, new BigDecimal("12.34").compareTo(body.getAmount()));
        assertEquals("2026-05-21", body.getDate());
        assertEquals("Food", body.getCategory());
    }

    @Test
    void processReceipt_withoutImageBody_shouldReturnBadRequest() {
        ReceiptProcessorFunction function = new ReceiptProcessorFunction(
                (image, filename) -> new ReceiptProcessResponse("Store", BigDecimal.ONE, LocalDate.now().toString(), "Food"));

        HttpResponseMessage response = function.processReceipt(new TestHttpRequest(Optional.empty(), Map.of()), null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
    }

    @Test
    void processReceipt_withProcessingError_shouldReturnJsonBadRequest() {
        ReceiptProcessorFunction function = new ReceiptProcessorFunction(
                (image, filename) -> {
                    throw new ReceiptProcessingException("No documents found in the receipt image.");
                });

        HttpResponseMessage response = function.processReceipt(
                new TestHttpRequest(Optional.of(new byte[]{1, 2, 3}), Map.of()),
                null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
        assertEquals("application/json", response.getHeader("Content-Type"));

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Receipt Processing Failed", body.get("error"));
        assertEquals("No documents found in the receipt image.", body.get("message"));
    }

    private static class CapturingReceiptProcessor implements ReceiptProcessor {
        private byte[] image;
        private String filename;

        @Override
        public ReceiptProcessResponse processReceipt(byte[] image, String filename) {
            this.image = image;
            this.filename = filename;
            return new ReceiptProcessResponse(
                    "Store",
                    new BigDecimal("12.34"),
                    "2026-05-21",
                    "Food");
        }
    }

    private static class TestHttpRequest implements HttpRequestMessage<Optional<byte[]>> {
        private final Optional<byte[]> body;
        private final Map<String, String> headers;

        private TestHttpRequest(Optional<byte[]> body, Map<String, String> headers) {
            this.body = body;
            this.headers = headers;
        }

        @Override
        public URI getUri() {
            return URI.create("http://localhost:7071/api/process-receipt");
        }

        @Override
        public com.microsoft.azure.functions.HttpMethod getHttpMethod() {
            return com.microsoft.azure.functions.HttpMethod.POST;
        }

        @Override
        public Map<String, String> getHeaders() {
            return headers;
        }

        @Override
        public Map<String, String> getQueryParameters() {
            return Map.of();
        }

        @Override
        public Optional<byte[]> getBody() {
            return body;
        }

        @Override
        public HttpResponseMessage.Builder createResponseBuilder(HttpStatus status) {
            return new TestHttpResponseBuilder().status(status);
        }

        @Override
        public HttpResponseMessage.Builder createResponseBuilder(HttpStatusType status) {
            return new TestHttpResponseBuilder().status(status);
        }
    }

    private static class TestHttpResponseBuilder implements HttpResponseMessage.Builder {
        private HttpStatusType status;
        private final Map<String, String> headers = new HashMap<>();
        private Object body;

        @Override
        public HttpResponseMessage.Builder status(HttpStatusType status) {
            this.status = status;
            return this;
        }

        @Override
        public HttpResponseMessage.Builder header(String key, String value) {
            headers.put(key, value);
            return this;
        }

        @Override
        public HttpResponseMessage.Builder body(Object body) {
            this.body = body;
            return this;
        }

        @Override
        public HttpResponseMessage build() {
            return new TestHttpResponse(status, headers, body);
        }
    }

    private static class TestHttpResponse implements HttpResponseMessage {
        private final HttpStatusType status;
        private final Map<String, String> headers;
        private final Object body;

        private TestHttpResponse(HttpStatusType status, Map<String, String> headers, Object body) {
            this.status = status;
            this.headers = headers;
            this.body = body;
        }

        @Override
        public HttpStatusType getStatus() {
            return status;
        }

        @Override
        public String getHeader(String key) {
            return headers.get(key);
        }

        @Override
        public Object getBody() {
            return body;
        }
    }
}
