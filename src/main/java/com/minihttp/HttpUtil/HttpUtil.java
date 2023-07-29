package com.minihttp.HttpUtil;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class HttpUtil {
    static public String decode(String data, String charset) throws UnsupportedEncodingException {
        return URLDecoder.decode(data, charset);
    }

    public static String decode(String data) {
        return URLDecoder.decode(data, StandardCharsets.UTF_8);
    }

    public static JsonObject asJson(Object data) {
        JsonElement jsonElement = JsonParser.parseString(data.toString());
        return jsonElement.getAsJsonObject();
    }

    public static String asText(Object data) {
        return data.toString();
    }

    public static Map<String, String> splitParams(String data) {
        return Arrays.stream(data.split("&"))
                .map(pair -> pair.split("="))
                .filter(ps -> ps.length == 2)
                .collect(Collectors.toMap(
                        ps -> URLDecoder.decode(ps[0], StandardCharsets.UTF_8),
                        ps -> URLDecoder.decode(ps[1], StandardCharsets.UTF_8),
                        (oldValue, newValue) -> oldValue
                ));
    }

    public static Map<String, String> asFormValue(Object data) {
        return splitParams(data.toString());
    }

    public static String encode(String data, String charset) throws UnsupportedEncodingException {
        return URLEncoder.encode(data, charset);
    }

    public static String encode(String data) {
        return URLEncoder.encode(data, StandardCharsets.UTF_8);
    }

    public static String parseContentTypeCharset(String contentType) {
        if (contentType != null) {
            int charsetIdx = contentType.indexOf("charset=");
            if (charsetIdx != -1) {
                return contentType.substring(charsetIdx + 8);
            }
        }
        return StandardCharsets.UTF_8.name(); // Default to UTF-8
    }

    public static String decodeBodyUsingContentType(String body, String type) throws UnsupportedEncodingException {
        return decode(body, parseContentTypeCharset(type));
    }

    public static String getContentType(String fileType) {
        switch (fileType) {
            case "html":
                return "text/html; charset=UTF-8";
            case "css":
                return "text/css; charset=UTF-8";
            case "js":
                return "application/javascript; charset=UTF-8";
            case "json":
                return "application/json; charset=UTF-8";
            case "xml":
                return "application/xml; charset=UTF-8";
            case "pdf":
                return "application/pdf";
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "gif":
                return "image/gif";
            case "ico":
                return "image/x-icon";
            default:
                return "application/octet-stream";
        }
    }

    public static String getExtension(String fileName) {
        int dotIdx = fileName.lastIndexOf(".");
        if (dotIdx != -1) {
            return fileName.substring(dotIdx + 1).toLowerCase();
        }
        return "";
    }

}
