package com.minihttp.URLTrieNode;

import com.minihttp.HttpHandler.HttpHandler;
import com.minihttp.HttpMethod.HttpMethod;
import com.minihttp.Pair.Pair;

import java.util.HashMap;
import java.util.Map;

public class URLTrieNode {
    Map<Pair<String, HttpMethod>, URLTrieNode> children;
    HttpHandler handler;
    String pathParam;


    public URLTrieNode(HttpHandler handler) {
        children = new HashMap<>();
        this.handler = handler;
        pathParam = null;

    }

    public String getPathParam() {
        return this.pathParam;
    }

    public void setPathParam(String x) {
        this.pathParam = x;
    }

    public Map<Pair<String, HttpMethod>, URLTrieNode> getChildren() {
        return children;
    }

    public HttpHandler getHandler() {
        return handler;
    }

    public void setHandler(HttpHandler h) {
        this.handler = h;
    }
}
