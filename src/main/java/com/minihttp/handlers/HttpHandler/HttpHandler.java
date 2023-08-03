package com.minihttp.handlers.HttpHandler;

import com.minihttp.PathParameters.PathParameters;
import com.minihttp.http.HttpRequest.HttpRequest;
import com.minihttp.http.HttpResponse.HttpResponse;

public interface HttpHandler {
    HttpResponse handle(HttpRequest req, PathParameters param);
}
