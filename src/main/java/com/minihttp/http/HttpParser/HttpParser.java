package com.minihttp.http.HttpParser;

import com.minihttp.http.HttpMethod.HttpMethod;
import com.minihttp.http.HttpRequest.HttpRequest;
import com.minihttp.http.HttpRequest.RequestBody;
import com.minihttp.http.HttpStatus.HttpStatus;
import com.minihttp.http.HttpUtil.HttpUtil;
import com.minihttp.util.Pair.Pair;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HttpParser {
    public static Pair<HttpStatus, HttpRequest> parse(String httpRequestString) {
        if (httpRequestString == null || httpRequestString.isEmpty()) {
            return new Pair<>(HttpStatus.BAD_REQUEST, null);
        }

        HttpStatus status = HttpStatus.OK;
        try {
            String[] lines = httpRequestString.split("\\r?\\n");
            if (lines[0].isEmpty() || lines.length < 2 || Character.isWhitespace(lines[0].charAt(0))) {
                status = HttpStatus.BAD_REQUEST;
                return new Pair<>(status, null);
            }

            String requestLine = lines[0];
            String[] requestParts = requestLine.split("\\s");
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
            String cookies = "";
            int contentLength = -1;

            for (int i = 1; i < lines.length; i++) {
                String line = lines[i];
                if (line.isEmpty()) {
                    break;
                }
                int colonIdx = line.indexOf(':');
                if (colonIdx > 0) {
                    String headerName = line.substring(0, colonIdx).trim().toLowerCase();
                    String headerValue = line.substring(colonIdx + 1).trim();
                    if (headerName.equalsIgnoreCase("accept")) {
                        accept = headerValue;
                    }
                    if (headerName.equalsIgnoreCase("content-type")) {
                        contentType = headerValue;
                    }
                    if (headerName.equalsIgnoreCase("content-length")) {
                        contentLength = Integer.parseInt(headerValue);
                    }
                    if (headerName.equalsIgnoreCase("cookies")) {
                        cookies = headerValue;
                    }
                    headers.put(headerName, headerValue);
                }
            }

                String requestBody = null;
                int requestBodyStart = httpRequestString.indexOf("\r\n\r\n");
                if (requestBodyStart != -1) {
                    requestBodyStart += 4;
                    requestBody = httpRequestString.substring(requestBodyStart);


                    try {
                        String charset = HttpUtil.parseContentTypeCharset(contentType);
                        requestBody = HttpUtil.decode(requestBody, charset);
                    } catch (UnsupportedEncodingException e) {
                        status = HttpStatus.BAD_REQUEST;
                        return new Pair<>(status, null);
                    }
                }

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
                        .setRequestBody(new RequestBody(contentType, requestBody))
                        .setInString(httpRequestString)
                        .setCookies(cookies)
                        .build();

                return new Pair<>(status, httpRequest);

        } catch (IllegalArgumentException e) {
            status = HttpStatus.BAD_REQUEST;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        return new Pair<>(status, null);
    }

    public static Map<String, String> parseCookies(String cookieHeader) {
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
}
