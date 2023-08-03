package com.minihttp.http.HttpUtil;

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

/**
 * The HttpUtil class provides utility methods for handling HTTP-related operations such as
 * URL encoding/decoding, parsing content types, and converting data to JSON or text format.
 */
public class HttpUtil {
    /**
     * Decodes the given URL-encoded data using the specified charset.
     *
     * @param data    The URL-encoded data to be decoded.
     * @param charset The character set used for decoding.
     * @return The decoded data as a String.
     * @throws UnsupportedEncodingException If the specified charset is not supported.
     */
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
        return switch (fileType) {
            case "html" -> "text/html; charset=UTF-8";
            case "css" -> "text/css; charset=UTF-8";
            case "js" -> "application/javascript; charset=UTF-8";
            case "json" -> "application/json; charset=UTF-8";
            case "xml" -> "application/xml; charset=UTF-8";
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "ico" -> "image/x-icon";
            default -> "application/octet-stream";
        };
    }

    public static String getExtension(String fileName) {
        int dotIdx = fileName.lastIndexOf(".");
        if (dotIdx != -1) {
            return fileName.substring(dotIdx + 1).toLowerCase();
        }
        return "";
    }

}
