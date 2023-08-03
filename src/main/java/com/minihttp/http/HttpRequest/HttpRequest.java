package com.minihttp.http.HttpRequest;

import com.minihttp.http.HttpMethod.HttpMethod;
import com.minihttp.http.HttpParser.HttpParser;
import com.minihttp.http.HttpStatus.HttpStatus;
import com.minihttp.http.HttpUtil.HttpUtil;
import com.minihttp.util.Pair.Pair;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Map;

public class HttpRequest {
    private final String uri;
    private final Map<String, String> requestHeader;
    private final HttpMethod opCode;
    private final RequestBody requestBody;
    private final String inString;
    private final Map<String, String> queryParams;
    private final String accept;
    private final String contentType;
    private final Map<String, String> cookies;

    private HttpRequest(HttpMethod op, Map<String, String> reqHeader, String u, RequestBody body, String s,
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

    public RequestBody getRequestBody() {
        return requestBody;
    }


    public static class Create {
        private String uri;
        private Map<String, String> requestHeader;
        private HttpMethod opCode;
        private RequestBody requestBody;
        private String inString;
        private String accept;
        private String contentType;
        private Map<String, String> cookies;

        public static Pair<HttpStatus, HttpRequest> processHttpRequest(String httpRequestString) {
            return HttpParser.parse(httpRequestString);
        }

        public Create setCookies(Map<String, String> cookies) {
            this.cookies = cookies;
            return this;
        }


        public Create setCookies(String cookieHeader) {
            this.cookies = HttpParser.parseCookies(cookieHeader);
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

        public Map<String, String> setQueryParams(String h) {
            return HttpUtil.splitParams(h);
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

        public Create setRequestBody(RequestBody body) {
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


