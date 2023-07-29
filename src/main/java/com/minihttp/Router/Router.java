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
                } else if (part.startsWith("*")) {
                    temp.setIsWildcard(true);
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
        URLTrieNode wildcardNode = null;
        if (temp == null) return null;
        boolean wilcardOccured = false;
        for (String part : parts) {
            URLTrieNode n = temp.getChildren().get(new Pair<>(part, r));
            if (n == null) {

                if (wilcardOccured) n = temp;
                else if (temp.isWildcard()) {
                    n = temp.getChildren().get(new Pair<>("*", r));
                    wilcardOccured = true;
                } else if (temp.getPathParam() != null) {
                    n = temp.getChildren().get(new Pair<>(temp.getPathParam(), r));
                    mp.put(temp.getPathParam().substring(1, temp.getPathParam().length() - 1), part);
                }

            } else wilcardOccured = false;
            temp = n;
            if (temp == null) return null;
        }
        return new Pair<>(mp, temp.getHandler());
    }
}

/*
public Pair<PathParameters, HttpHandler> find(String u, HttpMethod r) {
        PathParameters mp = new PathParameters();
        u = u.split("\\?")[0];
        String[] parts = u.substring(1).split("/");
        URLTrieNode temp = node;
        URLTrieNode wildcard = null;
        if (temp == null) return null;
        boolean wilcardOccured = false;
        for (String part : parts) {
            URLTrieNode n = temp.getChildren().get(new Pair<>(part, r));
            if (n == null) {
                LogWrapper.log(part + ":null");
                if (temp.getPathParam() != null) {
                    n = temp.getChildren().get(new Pair<>(temp.getPathParam(), r));
                    mp.put(temp.getPathParam().substring(1, temp.getPathParam().length() - 1), part);
                }
                if(wilcardOccured) n = temp;
                else if(temp.isWildcard()) {
                    LogWrapper.log(part + " wildcard");
                    n = temp.getChildren().get(new Pair<>("*", r));
                    wilcardOccured = true;
                }

            }else wilcardOccured = false;
            temp = n;
            if (temp == null) return null;
        }
        return new Pair<>(mp, temp.getHandler());
    }
 */

