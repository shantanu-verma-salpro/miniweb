package com.minihttp.HttpResponse;

import com.minihttp.HttpStatus.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class HttpResponse {

    private final int statusCode;
    private final Map<String, List<String>> responseHeader;
    private final Optional<Object> entity;
    private final byte[] httpResponseBytes;

    private HttpResponse(int statusCode, Map<String, List<String>> responseHeader, Optional<Object> entity) {
        this.statusCode = statusCode;
        this.responseHeader = responseHeader;
        this.entity = entity;
        this.httpResponseBytes = generateHttpResponseBytes();
    }

    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public String toString() {
        return new String(httpResponseBytes, StandardCharsets.UTF_8);
    }

    public Map<String, List<String>> getResponseHeader() {
        return Collections.unmodifiableMap(responseHeader);
    }

    public Optional<Object> getEntity() {
        return entity;
    }

    private byte[] generateHttpResponseBytes() {
        StringBuilder sb = new StringBuilder();
        String statusLine = "HTTP/1.1 " + statusCode + " " + HttpStatus.fromCode(statusCode) + "\r\n";
        sb.append(statusLine);

        int totalHeadersSize = 0;
        for (Map.Entry<String, List<String>> entry : responseHeader.entrySet()) {
            String headerName = entry.getKey();
            List<String> values = entry.getValue();
            totalHeadersSize += headerName.length() + values.stream().mapToInt(String::length).sum() + values.size() - 1;
        }

        // Append headers
        sb.ensureCapacity(sb.length() + totalHeadersSize);
        for (Map.Entry<String, List<String>> entry : responseHeader.entrySet()) {
            String headerName = entry.getKey();
            List<String> values = entry.getValue();
            sb.append(headerName).append(": ");
            int size = values.size();
            for (int i = 0; i < size; i++) {
                sb.append(values.get(i));
                if (i < size - 1) {
                    sb.append(';');
                }
            }
            sb.append("\r\n");
        }

        // Append entity if present and not empty
        entity.ifPresent(obj -> {
            String entityString = obj.toString();
            if (!entityString.isEmpty()) {
                String contentLengthHeader = "Content-Length: " + entityString.length() + "\r\n\r\n";
                sb.append(contentLengthHeader);
                sb.append(entityString);
            } else {
                sb.append("\r\n");
            }
        });

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static class Create {
        private int statusCode = 200;
        private Map<String, List<String>> responseHeader = new HashMap<>();
        private Optional<Object> entity = Optional.of("");
        private String contentType = "text/plain";

        public Create() {
            responseHeader.put("Content-Security-Policy", List.of("default-src 'self'"));
            responseHeader.put("X-Frame-Options", List.of("deny"));
            responseHeader.put("X-Content-Type-Options", List.of("nosniff"));
            responseHeader.put("Referrer-Policy", List.of("origin-when-cross-origin"));
            responseHeader.put("Server", List.of("MININIO/1.0.0.0"));
            responseHeader.put("Connection", List.of("keep-alive"));
            String pattern = "E, dd MMM yyyy HH:mm:ss z";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
            String date = simpleDateFormat.format(new Date());
            responseHeader.put("Date", List.of(date));
        }

        public Create setStatusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Create setResponseHeader(Map<String, List<String>> responseHeader) {
            this.responseHeader.putAll(responseHeader);
            return this;
        }

        public Create setEntity(Optional<Object> entity) {
            this.entity = entity;
            return this;
        }

        public Create setContentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public HttpResponse build() {
            responseHeader.put("Content-Type", List.of(contentType));
            return new HttpResponse(statusCode, responseHeader, entity);
        }
    }
}
