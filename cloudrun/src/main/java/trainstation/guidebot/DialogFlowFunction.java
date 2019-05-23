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
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Flow;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Akka Streams server on port 8080
 * see https://doc.akka.io/docs/akka-http/10.1.8/routing-dsl/index.html?language=java
 */
public class DialogFlowFunction extends AllDirectives {

    private InHouseNavigationDialogFlowApp dialogFlowApp = new InHouseNavigationDialogFlowApp();

    public static void main(String[] args) {
        final ActorSystem system = ActorSystem.create();
        final Materializer materializer = ActorMaterializer.create(system);

        final Http http = Http.get(system);

        DialogFlowFunction dialogFlowAgent = new DialogFlowFunction();

        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = dialogFlowAgent.
                handleDialogFlowRequest().flow(system, materializer);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow,
                ConnectHttp.toHost("localhost", 8080), materializer);

        //final CompletionStage<ServerBinding> binding = http.bindAndHandle(dialogFlowAgent.handleRequestResponseFlow(),
        //        ConnectHttp.toHost("localhost", 8080), materializer);

        binding
                .exceptionally(failure -> {
                            System.err.println("Something very bad happened! " + failure.getMessage());
                            system.terminate(); return null;
                })
                .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
                .thenAccept(unbound -> system.terminate()); // and shutdown when done
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
                post(() ->
                        path("/", () ->
                                withoutSizeLimit(() ->
                                        extractRequest(request -> {
                                            CompletableFuture<String> response = dialogFlowApp.handleRequest(
                                                    request.entity().getDataBytes().toString(),
                                                    getHeadersMap(request.getHeaders()));
                                            //final CompletionStage<Done> res = r..discardEntityBytes(materializer).completionStage();

                                            return onComplete(() -> response, done ->
                                                    // we only want to respond once the incoming data has been handled:
                                                    complete("Finished writing data :" + done));
                                        })
                                )
                        )
                );
        return defaultRoute;
    }

    private Map<String, String> getHeadersMap(Iterable<HttpHeader> requestHeaders) {
        Map<String, String> map = new HashMap();

        requestHeaders.iterator().forEachRemaining(header -> map.put(header.name(), header.value()));
        return map;
    }

}