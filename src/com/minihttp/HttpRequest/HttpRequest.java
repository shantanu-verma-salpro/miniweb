package com.minihttp.HttpRequest;

import com.minihttp.HttpMethod.HttpMethod;
import com.minihttp.HttpStatus.HttpStatus;
import com.minihttp.Pair.Pair;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HttpRequest {
    private final String uri;
    private final Map<String, String> requestHeader;
    private final HttpMethod opCode;
    private final String requestBody;
    private final String inString;
    private final Map<String, String> queryParams;
    private final String accept;
    private final String contentType;
    private final Map<String, String> cookies;

    private HttpRequest(HttpMethod op, Map<String, String> reqHeader, String u, String body, String s,
                        Map<String, String> kv, Map<String, String> cookies, String accept, String contentType) {
        this.uri = u;
        this.opCode = op;
        this.requestHeader = reqHeader;
        this.requestBody = body;
        this.inString = s;
        this.queryParams = kv;
        this.cookies = cookies;
        this.accept = accept;
        this.contentType = contentType;
    }

    public Map<String, String> getParams() {
        return this.queryParams;
    }

    public String getAccept() {
        return this.accept;
    }

    public String getContentType() {
        return this.contentType;
    }

    @Override
    public String toString() {
        return inString;
    }

    public String getHeader(String name) {
        return this.requestHeader.get(name);
    }

    public String getURI() {
        return this.uri;
    }

    public HttpMethod getHttpMethod() {
        return this.opCode;
    }

    public Map<String, String> getRequestHeader() {
        return Collections.unmodifiableMap(requestHeader);
    }

    public String getRequestBody() {
        return requestBody;
    }


    public static class Create {
        private String uri;
        private Map<String, String> requestHeader;
        private HttpMethod opCode;
        private String requestBody;
        private String inString;
        private String accept;
        private String contentType;
        private Map<String, String> cookies;

        public static Pair<HttpStatus, HttpRequest> processHttpRequest(String httpRequestString) {
            HttpStatus status = HttpStatus.OK;
            try {
                String[] lines = httpRequestString.split("\\r?\\n");
                if (lines.length < 2) {
                    status = HttpStatus.BAD_REQUEST;
                    return new Pair<>(status, null);
                }

                String requestLine = lines[0];
                String[] requestParts = requestLine.split(" ");
                if (requestParts.length != 3) {
                    status = HttpStatus.BAD_REQUEST;
                    return new Pair<>(status, null);
                }

                String method = requestParts[0];
                String path = requestParts[1];
                if (!requestParts[2].equalsIgnoreCase("HTTP/1.1")) {
                    status = HttpStatus.HTTP_VERSION_NOT_SUPPORTED;
                    return new Pair<>(status, null);
                }

                Map<String, String> headers = new HashMap<>();
                String accept = "*/*";
                String contentType = "text/plain";
                for (int i = 1; i < lines.length; i++) {
                    String line = lines[i];
                    if (line.isEmpty()) {
                        break;
                    }
                    int colonIdx = line.indexOf(':');
                    if (colonIdx > 0) {
                        String headerName = line.substring(0, colonIdx).trim();
                        String headerValue = line.substring(colonIdx + 1).trim();
                        if (headerName.equalsIgnoreCase("accept"))
                            accept = headerValue;
                        if (headerName.equalsIgnoreCase("content-type"))
                            contentType = headerValue;
                        headers.put(headerName, headerValue);
                    }
                }

                String requestBody = null;
                int requestBodyStart = httpRequestString.indexOf("\r\n\r\n");
                if (requestBodyStart != -1) {
                    requestBodyStart += 4;
                    requestBody = httpRequestString.substring(requestBodyStart);
                }
                if (requestBody != null)
                    requestBody = URLDecoder.decode(requestBody, StandardCharsets.UTF_8);

                if (HttpMethod.valueOf(method) == HttpMethod.POST) {
                    if (requestBody != null && requestBody.trim().isEmpty()) {
                        status = HttpStatus.BAD_REQUEST;
                        return new Pair<>(status, null);
                    }
                }

                HttpRequest httpRequest = new HttpRequest.Create()
                        .setHttpMethod(HttpMethod.valueOf(method))
                        .setURI(path)
                        .setRequestHeader(headers)
                        .setAccept(accept)
                        .setContentType(contentType)
                        .setRequestBody(requestBody != null ? requestBody.trim() : "")
                        .setInString(httpRequestString)
                        .build();

                return new Pair<>(status, httpRequest);
            } catch (IllegalArgumentException | UnsupportedEncodingException e) {
                status = HttpStatus.BAD_REQUEST;
                return new Pair<>(status, null);
            }
        }

        public Create setCookies(Map<String, String> cookies) {
            this.cookies = cookies;
            return this;
        }

        private Map<String, String> parseCookies(String cookieHeader) {
            if (cookieHeader == null || cookieHeader.isEmpty()) {
                return Collections.emptyMap();
            }

            return Stream.of(cookieHeader.split(";"))
                    .map(String::trim)
                    .map(cookie -> cookie.split("="))
                    .filter(cookieParts -> cookieParts.length == 2)
                    .collect(Collectors.toMap(
                            cookieParts -> cookieParts[0],
                            cookieParts -> cookieParts[1],
                            (oldValue, newValue) -> oldValue
                    ));
        }

        public Create setCookies(String cookieHeader) {
            this.cookies = parseCookies(cookieHeader);
            return this;
        }

        public Create setAccept(String accept) {
            this.accept = accept;
            return this;
        }

        public Create setContentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Map<String, String> setQueryParams(String h) throws UnsupportedEncodingException {
            return Arrays.stream(h.split("&"))
                    .map(pair -> pair.split("="))
                    .filter(ps -> ps.length == 2)
                    .collect(Collectors.toMap(
                            ps -> URLDecoder.decode(ps[0], StandardCharsets.UTF_8),
                            ps -> URLDecoder.decode(ps[1], StandardCharsets.UTF_8),
                            (oldValue, newValue) -> oldValue
                    ));
        }

        public Create setURI(String u) {
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

        public Create setRequestHeader(Map<String, String> l) {
            this.requestHeader = l;
            return this;
        }

        public Create setRequestBody(String body) {
            this.requestBody = body;
            return this;
        }

        public HttpRequest build() throws UnsupportedEncodingException {
            String[] pq = this.uri.split("\\?");
            return new HttpRequest(
                    this.opCode,
                    this.requestHeader,
                    pq[0],
                    this.requestBody,
                    this.inString,
                    pq.length > 1 ? this.setQueryParams(pq[1]) : null,
                    this.cookies,
                    this.accept,
                    this.contentType
            );
        }
    }
}


