package com.minihttp.HttpRequest;

import com.minihttp.HttpMethod.HttpMethod;

import java.net.URI;
import java.util.*;

public class HttpRequest {
    private final URI uri;
    private final Map<String, List<String>> requestHeader;
    private final HttpMethod opCode;
    private final String requestBody;
    private final String inString;

    private HttpRequest(HttpMethod op, Map<String, List<String>> reqHeader, URI u, String body, String s) {
        this.uri = u;
        this.opCode = op;
        this.requestHeader = reqHeader;
        this.requestBody = body;
        this.inString = s;
    }

    @Override
    public String toString() {
        return inString;
    }

    public List<String> getHeader(String name) {
        return this.requestHeader.get(name);
    }

    public URI getURI() {
        return this.uri;
    }

    public HttpMethod getHttpMethod() {
        return this.opCode;
    }

    public Map<String, List<String>> getRequestHeader() {
        return Collections.unmodifiableMap(requestHeader);
    }

    public String getRequestBody() {
        return requestBody;
    }

    public static class Create {
        private URI uri;
        private Map<String, List<String>> requestHeader;
        private HttpMethod opCode;
        private String requestBody;
        private String inString;

        public static HttpRequest fromStringRequest(String requestString) throws Exception {
            String[] lines = requestString.split("\\r?\\n");
            if (lines.length < 2) {
                throw new IllegalArgumentException("Invalid HTTP request string");
            }

            // Parse request line
            String[] requestLine = lines[0].split(" ");
            if (requestLine.length != 3) {
                throw new IllegalArgumentException("Invalid request line in HTTP request");
            }
            HttpMethod method = HttpMethod.valueOf(requestLine[0]);
            URI uri = URI.create(requestLine[1]);

            // Parse request headers
            Map<String, List<String>> headers = new HashMap<>();
            int i = 1;
            while (i < lines.length && !lines[i].isEmpty()) {
                String[] headerParts = lines[i].split(":", 2);
                if (headerParts.length == 2) {
                    String headerName = headerParts[0].trim();
                    String headerValue = headerParts[1].trim();
                    String[] key_params = headerValue.split(";");
                    headers.putIfAbsent(headerName, Arrays.asList(key_params));
                }
                i++;
            }

            // Parse request body
            StringBuilder requestBody = new StringBuilder();
            for (int j = i + 1; j < lines.length; j++) {
                requestBody.append(lines[j]).append("\n");
            }

            return new HttpRequest.Create()
                    .setHttpMethod(method)
                    .setURI(uri)
                    .setRequestHeader(headers)
                    .setRequestBody(requestBody.toString().trim())
                    .setInString(requestString)
                    .build();
        }

        public Create setURI(URI u) {
            this.uri = u;
            return this;
        }

        public Create setInString(String s) {
            this.inString = s;
            return this;
        }

        public Create setHttpMethod(HttpMethod m) {
            this.opCode = m;
            return this;
        }

        public Create setRequestHeader(Map<String, List<String>> l) {
            this.requestHeader = l;
            return this;
        }

        public Create setRequestBody(String body) {
            this.requestBody = body;
            return this;
        }

        public HttpRequest build() {
            return new HttpRequest(this.opCode, this.requestHeader, this.uri, this.requestBody, this.inString);
        }
    }
}

