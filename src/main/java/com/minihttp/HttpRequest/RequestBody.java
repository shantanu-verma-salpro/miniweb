package com.minihttp.HttpRequest;

import com.google.gson.JsonObject;
import com.minihttp.HttpUtil.HttpUtil;

import java.util.Map;

public class RequestBody {
    private String contentType;
    private String body;

    public RequestBody(String contentType, String body) {
        this.contentType = contentType;
        this.body = body;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public JsonObject asJson() {
        return HttpUtil.asJson(body);
    }

    public String asText() {
        return HttpUtil.asText(body);
    }

    public Object asRaw() {
        return body;
    }

    public Map<String, String> asFormValues() {
        return HttpUtil.asFormValue(body);
    }

}