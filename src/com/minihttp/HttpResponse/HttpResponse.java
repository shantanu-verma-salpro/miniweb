package com.minihttp.HttpResponse;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class HttpResponse {
    private static final Map<Integer, String> statusCodes = Map.ofEntries(
            Map.entry(100, "Continue"),
            Map.entry(101, "Switching Protocols"),
            Map.entry(200, "OK"),
            Map.entry(201, "Created"),
            Map.entry(202, "Accepted"),
            Map.entry(203, "Non-Authoritative Information"),
            Map.entry(204, "No Content"),
            Map.entry(205, "Reset Content"),
            Map.entry(206, "Partial Content"),
            Map.entry(300, "Multiple Choices"),
            Map.entry(302, "Found"),
            Map.entry(303, "See Other"),
            Map.entry(304, "Not Modified"),
            Map.entry(305, "Use Proxy"),
            Map.entry(400, "Bad Request"),
            Map.entry(401, "Unauthorized"),
            Map.entry(402, "Payment Required"),
            Map.entry(403, "Forbidden"),
            Map.entry(404, "Not Found"),
            Map.entry(405, "Method Not Allowed"),
            Map.entry(406, "Not Acceptable"),
            Map.entry(407, "Proxy Authentication Required"),
            Map.entry(408, "Request Timeout"),
            Map.entry(409, "Conflict"),
            Map.entry(410, "Gone"),
            Map.entry(411, "Length Required"),
            Map.entry(412, "Precondition Failed"),
            Map.entry(413, "Request Entity Too Large"),
            Map.entry(414, "Request-URI Too Long"),
            Map.entry(415, "Unsupported Media Type"),
            Map.entry(416, "Requested Range Not Satisfiable"),
            Map.entry(417, "Expectation Failed"),
            Map.entry(500, "Internal Server Error"),
            Map.entry(501, "Not Implemented"),
            Map.entry(502, "Bad Gateway"),
            Map.entry(503, "Service Unavailable"),
            Map.entry(504, "Gateway Timeout"),
            Map.entry(505, "HTTP Version Not Supported")
    );
    private final Integer statusCode;
    private final Map<String, List<String>> responseHeader;
    private final Optional<Object> entity;

    private HttpResponse(Integer s, Map<String, List<String>> rh, Optional<Object> e) {
        this.statusCode = s;
        this.responseHeader = rh;
        this.entity = e;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();

        // Append status line
        String statusLine = "HTTP/1.1 " + this.statusCode + " " + statusCodes.get(this.statusCode) + "\r\n";
        s.append(statusLine);

        // Append headers
        for (Map.Entry<String, List<String>> x : this.responseHeader.entrySet()) {
            s.append(x.getKey()).append(": ");
            List<String> values = x.getValue();
            int size = values.size();
            for (int i = 0; i < size; i++) {
                s.append(values.get(i));
                if (i < size - 1) {
                    s.append(';');
                }
            }
            s.append("\r\n");
        }

        // Append entity if present and not empty
        String entityString = this.entity.map(Object::toString).orElse("");
        if (!entityString.isEmpty()) {
            byte[] entityBytes = entityString.getBytes(StandardCharsets.UTF_8);
            String contentLengthHeader = "Content-Length: " + entityBytes.length + "\r\n\r\n";
            s.append(contentLengthHeader);
            s.append(new String(entityBytes));
        } else {
            s.append("\r\n");
        }

        return s.toString();
    }

    public Map<String, List<String>> getResponseHeader() {
        return Collections.unmodifiableMap(responseHeader);
    }

    public Optional<Object> getEntity() {
        return entity;
    }

    public static class Create {
        private Integer statusCode;
        private Map<String, List<String>> responseHeader;
        private Optional<Object> entity;

        public Create() {
            this.responseHeader = new HashMap<>();
            this.responseHeader.put("Content-Security-Policy", List.of("default-src 'self'"));
            this.responseHeader.put("X-Frame-Options", List.of("deny"));
            this.responseHeader.put("X-Content-Type-Options", List.of("nosniff"));
            this.responseHeader.put("Referrer-Policy", List.of("origin-when-cross-origin"));
            this.responseHeader.put("Server", List.of("MININIO/1.0.0.0"));
            this.responseHeader.put("Connection", List.of("keep-alive"));
            String pattern = "E, dd MMM yyyy HH:mm:ss z";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
            String date = simpleDateFormat.format(new Date());
            this.responseHeader.put("Date", List.of(date));
            this.statusCode = 200;
            this.entity = Optional.of("");
        }

        public Create setStatusCode(Integer s) {
            this.statusCode = s;
            return this;
        }

        public Create setResponseHeader(Map<String, List<String>> ls) {
            this.responseHeader.putAll(ls);
            return this;
        }

        public Create setEntity(Optional<Object> e) {
            this.entity = e;
            return this;
        }

        public HttpResponse build() {

            return new HttpResponse(this.statusCode, this.responseHeader, this.entity);
        }

    }
}
