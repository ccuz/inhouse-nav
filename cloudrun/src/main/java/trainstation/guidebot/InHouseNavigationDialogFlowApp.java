package trainstation.guidebot;

import com.google.actions.api.*;
import com.google.actions.api.response.ResponseBuilder;
import com.google.actions.api.response.helperintent.Permission;
import com.google.api.services.actions_fulfillment.v2.model.Location;
import com.google.api.services.dialogflow_fulfillment.v2.model.Context;
import com.google.api.services.dialogflow_fulfillment.v2.model.WebhookResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

import static java.text.MessageFormat.format;
import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * see https://github.com/actions-on-google/actions-on-google-java for documentation
 * and https://medium.com/google-developers/announcing-the-java-kotlin-client-library-for-actions-on-google-217828dead
 */
public class InHouseNavigationDialogFlowApp extends DialogflowApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(InHouseNavigationDialogFlowApp.class);

    private ResourceBundle messages = ResourceBundle.getBundle("messages");

    @ForIntent("Default Fallback Intent")
    public ActionResponse fallback(ActionRequest request) {
        ResourceBundle responses = ResourceBundle.getBundle("responses");
        String didNotUnderstand = responses.getString("didNotUnderstand");

        return getResponseBuilder(request)
                .add(didNotUnderstand)
                .build();
    }

    @ForIntent("welcome")
    public ActionResponse welcome(ActionRequest request) {
        ResponseBuilder responseBuilder = getResponseBuilder(request);

        responseBuilder
                //.add(messages.getString("welcome"))
                .add(new Permission()
                        .setPermissions(new String[]{
                                ConstantsKt.PERMISSION_DEVICE_PRECISE_LOCATION
                        })
                        .setContext(messages.getString("toLocateYou"))
                );
        responseBuilder.getConversationData().put("requestedPermission", "DEVICE_PRECISE_LOCATION");

        return responseBuilder.build();
    }

    @ForIntent("current_location")
    public ActionResponse currentlocation(ActionRequest request) {
        /**
         * agent.add(`I know where you are.`);
         *     let conv = agent.conv();
         * 	const {coordinates} = conv.device.location;
         * 	const {city} = conv.device.location;
         *  	if (city) {
         *         //conv.ask(`You are at ${coordinates.latitude}/${coordinates.longitude}`);
         *       	conv.ask(`You are at ${city}. On which plateform do your train leaves?`);
         *         const lifespan = 5;
         *   		//const contextParameters = {location: [coordinates.latitude, coordinates.longitude]};
         *   		const contextParameters = {location: `${city}`};
         *         conv.contexts.set('locationContext', lifespan, contextParameters);
         * 		//conv.close(`You are at ${coordinates.latitude}/${coordinates.longitude}`);
         *        } else {
         * 		// Note: Currently, precise locaton only returns lat/lng coordinates on phones and lat/lng coordinates
         * 		// and a geocoded address on voice-activated speakers.
         * 		// Coarse location only works on voice-activated speakers.
         * 		conv.close('Sorry, I could not figure out where you are.');
         *    }
         *     agent.add(conv);
         */
        ResponseBuilder responseBuilder = getResponseBuilder(request);

        Location location = request.getDevice().getLocation();
        if (location != null){
            if (isNotBlank(location.getCity())) {
                final String city = location.getCity();

                Context locationContext = new Context();
                locationContext.setLifespanCount(5);
                HashMap params = new HashMap();
                params.put("location", city);
                locationContext.setParameters(params);
                locationContext.setName("locationContext");

                WebhookResponse response = new WebhookResponse();
                List outputContexts = new ArrayList<>();
                outputContexts.add(locationContext);
                response.setOutputContexts(outputContexts);

                final String locationString = format(messages.getString("youAreAt"), city);
                responseBuilder.add(locationString);
                responseBuilder = responseBuilder.use(response);
            } else {
                responseBuilder
                        .add("Sorry, I could not figure out the city where you are.");
            }
        } else {
            responseBuilder
                    .add("Sorry, I could not figure out where you are.");
        }

        return responseBuilder.build();
    }

    @ForIntent("destination")
    public ActionResponse routing(ActionRequest request) throws IOException {
        /**
         * agent.add(`listen!`);
         *     let conv = agent.conv();
         *     //todo: call backend to get route steps
         *     //const {steps} = ['turn_left', 'turn_right', 'stairs_up', 'stairs_down', 'walk_x_meters'];
         *     //for (step as steps){
         *       conv.ask(`Turn left, go stairs up, pass in front of Brezelkonig, continue walking till platform ${conv.parameters.destination}, stairs-up`);
         *     //}
         *     const lifespan = 5;
         *   	const contextParameters = {route: 'Turn left, go stairs up, pass in front of Brezelkonig, continue walking till platform 5, stairs-up'};
         *     conv.contexts.set('route', lifespan, contextParameters);
         *
         *     agent.add(conv);
         */



        String destination = (String) request.getParameter("destination");

        String mockedResponse = format("Turn left, go stairs up, pass in front of Brezelkoenig, continue walking till platform ${0}, stairs-up", destination);

        Context routingContext = new Context();
        routingContext.setLifespanCount(5);
        HashMap params = new HashMap();
        params.put("route", mockedResponse);
        routingContext.setParameters(params);
        routingContext.setName("route");

        WebhookResponse response = new WebhookResponse();
        List outputContexts = new ArrayList<>();
        outputContexts.add(routingContext);
        response.setOutputContexts(outputContexts);

        ResponseBuilder responseBuilder = getResponseBuilder(request);
        responseBuilder.add(mockedResponse);
        responseBuilder.use(response);

        return responseBuilder.build();
    }

    private Optional<String> getParameterFromContext(String paramName, String contextName, ActionRequest request) {
        ActionContext context = request.getContext(request.getSessionId() + "/contexts/" + contextName);

        if (context == null || context.getParameters() == null || ((String) context.getParameters().get(paramName)).isEmpty()) {
            return Optional.empty();
        }

        return Optional.of((String) context.getParameters().get(paramName));
    }
}
