import com.minihttp.PathParameters.PathParameters;
import com.minihttp.http.HttpMethod.HttpMethod;
import com.minihttp.http.HttpRequest.HttpRequest;
import com.minihttp.http.HttpRequest.RequestBody;
import com.minihttp.http.HttpResponse.HttpResponse;
import com.minihttp.server.core.MiniServer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;


public class Main {
    public static void main(String[] args) throws IOException {
        int port = 8090;

        try {
            MiniServer httpServer = new MiniServer();

            BookController bookController = new BookController();
            httpServer.addRoute("/books", HttpMethod.GET, bookController::handleGetAll);
            httpServer.addRoute("/books/{id}", HttpMethod.GET, bookController::handleGetById);
            httpServer.addRoute("/books/*", HttpMethod.GET, bookController::handlePost);
            httpServer.addRoute("/books/*/h", HttpMethod.GET, bookController::handlePut);
            httpServer.addRoute("/books/*/h/{id}/*", HttpMethod.GET, bookController::handleGetById);
            httpServer.addRoute("/books/1", HttpMethod.GET, bookController::handleGetAll);
            httpServer.addRoute("/libs", HttpMethod.GET, bookController::handleGetAll);
            httpServer.addRoute("/books/{id}", HttpMethod.GET, bookController::handleGetById);
            httpServer.addRoute("/books/{id}/{name}", HttpMethod.GET, bookController::handleGetById);
            httpServer.addRoute("/books", HttpMethod.POST, bookController::handlePost);
            httpServer.addRoute("/books/{id}", HttpMethod.PUT, bookController::handlePut);
            httpServer.addRoute("/books/{id}", HttpMethod.DELETE, bookController::handleDelete);

            httpServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        System.in.read();

    }

    public static class BookController {
        public HttpResponse handleGetAll(HttpRequest req, PathParameters p) {
            // Handle GET request for all books
            String responseString = "Handling GET all books";
            return new HttpResponse.Create()
                    .setStatusCode(200)
                    .setEntity(Optional.of(responseString))
                    .build();
        }

        public HttpResponse handleGetById(HttpRequest req, PathParameters p) {
            String responseString = "Handling GET book by id: " + p.entrySet() + req.getParams() + req.getRequestBody() + " " + req.getContentType();
            return new HttpResponse.Create()
                    .setStatusCode(200)
                    .setEntity(Optional.of(responseString))
                    .build();
        }

        public HttpResponse handlePost(HttpRequest req, PathParameters p) {
            String responseString = "Handling POST request to add a new book";
            RequestBody body = req.getRequestBody();
            return new HttpResponse.Create()
                    .setStatusCode(200)
                    .setEntity(Optional.of(responseString + ":" + body.asJson().keySet()))
                    .build();
        }

        public HttpResponse handlePut(HttpRequest req, PathParameters p) {
            String responseString = "Handling PUT request to update book with id: " + req.getURI();
            return new HttpResponse.Create()
                    .setStatusCode(200)
                    .setEntity(Optional.of(responseString))
                    .build();
        }

        public HttpResponse handleDelete(HttpRequest req, PathParameters p) {
            String responseString = "Handling DELETE request to delete book with id: " + req.getURI();
            return new HttpResponse.Create()
                    .setStatusCode(200)
                    .setEntity(Optional.of(responseString))
                    .build();
        }
    }
}