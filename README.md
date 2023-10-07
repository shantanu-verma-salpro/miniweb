
# MiniWeb 🌐

MiniWeb is a minimalist web server implemented in Java, utilizing the Non-blocking I/O (NIO) and the Multi-Reactor pattern to efficiently handle network events in a scalable manner.

## Features ✨
- **Non-blocking I/O (NIO)**: Efficient handling of network events with Java NIO.
- **Multi-Reactor Pattern**: Enhanced scalability and performance by employing a multi-reactor pattern.
- **Trie-based Routing**: Utilizes a trie for efficient routing and URL matching.

## Usage 🚀

### 1. Define Your Handlers:

```java
public static class BookController {
    public static HttpResponse handleGetAll(HttpRequest req, PathParameters p) {
        // Handle GET request for all books
        String responseString = "Handling GET all books";
        return new HttpResponse.Builder()
                .statusCode(200)
                .body(responseString)
                .build();
    }
}
```
### 2. Route and Start the Server:

```cpp
public class App {
    public static void main(String[] args) throws IOException {
        MiniServer x = new MiniServer();
        x.route("/books", HttpMethod.GET, BookController::handleGetAll);
        x.start();
    }
}
```

## Installation 🛠️

Compile and run the project using your favorite IDE or via the command line.

## Contributing 🤝

Contributions are welcome! Feel free to open a PR or submit issues for any bugs or enhancements.

## License 📄

This project is open-source and available under the MIT License.
