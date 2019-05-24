package trainstation.guidebot;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpHeader;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.http.javadsl.unmarshalling.Unmarshaller;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Flow;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * Akka Streams server on port 8080
 * see https://doc.akka.io/docs/akka-http/10.1.8/routing-dsl/index.html?language=java
 */
public class DialogFlowFunction extends AllDirectives {
    private static final Logger LOGGER = LoggerFactory.getLogger(DialogFlowFunction.class);

    private InHouseNavigationDialogFlowApp dialogFlowApp = new InHouseNavigationDialogFlowApp();

    public static void main(String[] args) throws IOException {
        int defaultPort = 8080;
        if (isNotBlank(System.getenv("PORT"))){
            defaultPort = Integer.parseInt(System.getenv("PORT"));
        }

        final ActorSystem system = ActorSystem.create();
        final Materializer materializer = ActorMaterializer.create(system);

        final Http http = Http.get(system);

        DialogFlowFunction dialogFlowAgent = new DialogFlowFunction();

        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = dialogFlowAgent.
                handleDialogFlowRequest().flow(system, materializer);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow,
                ConnectHttp.toHost("0.0.0.0", defaultPort), materializer);

        System.out.println(format("The server running on %d", defaultPort));

        /*binding.exceptionally(failure -> {
                            System.err.println("Something very bad happened! " + failure.getMessage());
                            system.terminate(); return null;
                })
                //.thenAccept(serverBinding -> serverBinding.terminate(Duration.of(4, MINUTES)));
                .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
                .thenAccept(unbound -> system.terminate()); // and shutdown when done*/

    }


    /*private Flow<HttpRequest, HttpResponse, NotUsed> handleRequestResponseFlow(Materializer materializer) {
        return Flow.fromFunction((HttpRequest request, HttpResponse response) -> {
                if (request.withMethod(POST).getUri().path().equals("/")) {
                    CompletableFuture<String> responseJson = dialogFlowApp.handleRequest(request.entity().getDataBytes().toString(), getHeadersMap(request.getHeaders()));
                    return HttpResponse.create().withEntity(ContentTypes.APPLICATION_JSON,
                            fromString(responseJson.get()));
                }
                else {
                    request.discardEntityBytes(materializer);
                    return HttpResponse.create().withStatus(StatusCodes.NOT_FOUND).withEntity("Unknown resource!");
                }
        });
    }*/

    /**
     * Handles request received via HTTP POST and delegates it to your Actions app.
     *  * See: [Request handling in Google App Engine](https://cloud.google.com/appengine/docs/standard/java/how-requests-are-handled).
     * @return Route handling
     */
    private Route handleDialogFlowRequest(){
        final Route defaultRoute =
                post(postRoutes());
        return defaultRoute;
    }

    @NotNull
    private Supplier<Route> postRoutes() {
        return () -> pathSingleSlash(postRootRoute());
    }

    @NotNull
    private Supplier<Route> postRootRoute() {
        return () -> extractRequest(googleActionRequestHandler());
        //return () -> withoutSizeLimit(
        //        () -> extractRequest(googleActionRequestHandler())
        //        );
    }

    @NotNull
    private Function<HttpRequest, Route> googleActionRequestHandler() {
        return request ->
            entity(Unmarshaller.entityToString(), body -> {
                LOGGER.debug("body: " + body);

                CompletableFuture<String> response = dialogFlowApp.handleRequest(body,
                        getHeadersMap(request.getHeaders()));
                //final CompletionStage<Done> res = r..discardEntityBytes(materializer).completionStage();

                return onComplete(() -> response, done -> {
                    String responseString = done.toString();
                    LOGGER.debug("response: " + responseString);
                    // we only want to respond once the incoming data has been handled:
                    return complete(responseString);
                });
            });
    }

    private Map<String, String> getHeadersMap(Iterable<HttpHeader> requestHeaders) {
        Map<String, String> map = new HashMap();

        requestHeaders.iterator().forEachRemaining(header -> map.put(header.name(), header.value()));
        return map;
    }

}