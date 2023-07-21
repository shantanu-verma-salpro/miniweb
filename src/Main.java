import com.minihttp.HttpMethod.HttpMethod;
import com.minihttp.HttpResponse.HttpResponse;

import java.net.URI;
import java.util.AbstractMap;
import java.util.Optional;

public class Main {

    public static void main(String[] args) throws Exception {
        MiniHttpServer server = new MiniHttpServer(5000);
        server.addRoute(URI.create("/hello"),
                new AbstractMap.SimpleImmutableEntry<>(
                        HttpMethod.GET,
                        request -> new HttpResponse.Create().
                                setStatusCode(200).
                                setEntity(Optional.of("hello")).
                                build()));
        server.start();
        server.stop();
    }
}

