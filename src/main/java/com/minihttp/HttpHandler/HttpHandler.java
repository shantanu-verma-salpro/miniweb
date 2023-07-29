package com.minihttp.HttpHandler;

import com.minihttp.HttpRequest.HttpRequest;
import com.minihttp.HttpResponse.HttpResponse;
import com.minihttp.PathParameters.PathParameters;

public interface HttpHandler {
    public HttpResponse handle(HttpRequest req, PathParameters param);
}
