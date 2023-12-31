package com.minihttp.routing.URLTrieNode;

import com.minihttp.handlers.HttpHandler.HttpHandler;
import com.minihttp.http.HttpMethod.HttpMethod;
import com.minihttp.util.Pair.Pair;

import java.util.HashMap;
import java.util.Map;

public class URLTrieNode {
    final Map<Pair<String, HttpMethod>, URLTrieNode> children;
    HttpHandler handler;
    String pathParam;
    boolean wildcard;


    public URLTrieNode(HttpHandler handler) {
        children = new HashMap<>();
        this.handler = handler;
        pathParam = null;
        this.wildcard = false;
    }

    public boolean isWildcard() {
        return this.wildcard;
    }

    public void setIsWildcard(boolean x) {
        this.wildcard = x;
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
