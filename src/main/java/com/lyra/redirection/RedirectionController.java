package com.lyra.redirection;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import spark.Route;

import java.util.HashMap;
import java.util.Map;

/**
 * Example controller that handles a redirection payment that will be displayed inside a Webview.<p></p>
 *
 * This controller just extracts parameters from Request and send them to the service component. This should
 * return a prepared URL to browse into the WebView<p></p>
 *
 * For readability purposes in this example:
 * <li>We do not use logs</li>
 * <li>The JSON content is converted into a basic map structure. Use an appropiate DTO class hierarchy instead
 * if you want to provide a more scalable and robust code</li>
 *
 * @author Lyra Network
 */
public class RedirectionController {

    public static Route createPayment = (request, response) -> {
        response.type("application/json; charset=utf-8"); //We return always a JSON response

        //Retrieve all parameters from request payload
        Map<String, String> requestData = null;
        try {
            requestData = new Gson().fromJson(request.body(), HashMap.class); //Parse request JSON into a Map structure
        } catch(JsonSyntaxException jse) {

            response.status(400);
            return toJSONError("Bad Request");
        }

        //Retrieve payment URL
        String redirectionUrl = null;
        try {
            //Parse all parameters in order to initialize the payment and generate the URL
            String email = requestData.get("email");
            String amount = requestData.get("amount");
            String currency = requestData.get("currency");
            String mode = requestData.get("mode");
            String language = requestData.get("language");
            String cardType = requestData.get("cardType");
            String orderId = requestData.get("orderId");
            String configurationSet = requestData.get("configurationSet");

            //Call the service in order to initialize the payment context and return generatedURL to perform redirection
            redirectionUrl = new RedirectionService().initPayment(email, amount, currency, mode, language, cardType, orderId, configurationSet);
        } catch (Exception e) {
            response.status(500);
            return toJSONError("Internal Server Error");
        }

        //Return response data with the URL in JSON format
        return toJSONOk(redirectionUrl);
    };

    /*
     *
     * Static example methods to build a JSON response for the client
     *
     */
    private static String toJSONOk(String  redirectionUrl) {
        return responseToJSON("OK", "", redirectionUrl);
    }
    private static String toJSONError(String message) {
        return responseToJSON("ERROR", message, "");
    }

    private static String responseToJSON(String status, String errorMessage, String redirectionUrl) {
        JsonObject responseObject = new JsonObject();

        responseObject.addProperty("status", status);
        responseObject.addProperty("errorMessage", errorMessage);
        responseObject.addProperty("redirectionUrl", redirectionUrl);

        return responseObject.toString();
    }
}