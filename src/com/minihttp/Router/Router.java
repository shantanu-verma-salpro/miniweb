package com.minihttp.Router;

import com.minihttp.HttpHandler.HttpHandler;
import com.minihttp.HttpMethod.HttpMethod;
import com.minihttp.Pair.Pair;
import com.minihttp.PathParameters.PathParameters;
import com.minihttp.URLTrieNode.URLTrieNode;

import java.net.URISyntaxException;


public class Router {
    private URLTrieNode node;

    public Router() {
        node = new URLTrieNode(null);
    }

    public void add(String _u, HttpMethod r, HttpHandler h) throws URISyntaxException {
        String[] pathQuery = _u.split("\\?");
        String u = pathQuery[0];
        String[] parts = u.substring(1).split("/");
        URLTrieNode temp = node;
        for (String part : parts) {
            URLTrieNode n = temp.getChildren().get(new Pair<>(part, r));
            if (n == null) {
                n = new URLTrieNode(h);
                if (part.startsWith("{")) {
                    temp.setPathParam(part);
                }
                temp.getChildren().put(new Pair<>(part, r), n);
            }
            temp = n;
        }
    }

    public Pair<PathParameters, HttpHandler> find(String u, HttpMethod r) {
        PathParameters mp = new PathParameters();
        u = u.split("\\?")[0];
        String[] parts = u.substring(1).split("/");
        URLTrieNode temp = node;
        if (temp == null) return null;
        for (String part : parts) {
            URLTrieNode n = temp.getChildren().get(new Pair<>(part, r));
            if (n == null) {
                if (temp.getPathParam() != null) {
                    n = temp.getChildren().get(new Pair<>(temp.getPathParam(), r));
                    mp.put(temp.getPathParam().substring(1, temp.getPathParam().length() - 1), part);
                }
            }
            temp = n;
            if (temp == null) return null;
        }
        return new Pair<>(mp, temp.getHandler());
    }
}
