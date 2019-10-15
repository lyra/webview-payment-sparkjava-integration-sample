package com.lyra.redirection;

import com.google.gson.Gson;
import com.lyra.ServerConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This class initializes a payment in the payment platform and returns the generated URL.<p></p>
 *
 * @author Lyra Network
 */
public class RedirectionService {
    /**
     * This method creates a form and post it into the payment platform in order to generate the payment URL<p></p>
     *
     * @param email customer email
     * @param amount amount of payment in cents
     * @param currency the used currency in ISO 4217. Ex: 978
     * @param mode the used mode of payment: TEST or PRODUCTION
     * @param language the language used to perform the payment. Ex: EN
     * @param cardType the card type. Ex: VISA
     * @param configurationSet configuration name. If null, the default one will be taken
     * @return The generated URL, ready to be browsed in the Webview
     */
    public String initPayment(String email, String amount, String currency, String mode, String language, String cardType, String orderId, String configurationSet) {
        String redirectionUrl = null;

        //Read configuration
        Map<String, String> configuration = ServerConfiguration.getConfiguration(mode);

        //Create HTTP client
        CloseableHttpClient client = createHttpClient(configuration);

        //Prepare the post object to perform post
        HttpPost post = new HttpPost(String.format("%s/vads-payment/entry.silentInit.a",
                configuration.get("paymentPlatformUrl")));

        //Create form to send to payment platform
        Integer transactionId = TransactionIdGenerator.generateNewTransactionId();
        List<NameValuePair> formParameters = createFormParameters(transactionId, email, amount, currency, mode, language, cardType, orderId, configuration);

        try {
            post.setEntity(new UrlEncodedFormEntity(formParameters, "UTF-8"));
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException("Error in payment initialization", uee);
        }

        //Perform the HTTP call to payment platform
        try (CloseableHttpResponse httpResponse = client.execute(post)) {
            HttpEntity entity = httpResponse.getEntity();

            // Get response code
            int httpResponseCode = httpResponse.getStatusLine().getStatusCode();
            // Get all response data
            Map<String, String> responseData = new Gson().fromJson(EntityUtils.toString(entity, "UTF-8"), Map.class);

            //If the HTTP return code is 200 (OK) we prepare the generated URL
            if (httpResponseCode == 200) {
                if ("INITIALIZED".equals(responseData.get("status"))) {
                    redirectionUrl = responseData.get("redirect_url");
                } else {
                    //Payment could  not be created. Maybe a missing parameter, an invalid value or signature?
                    //Use logs here in order to detect and fix the real cause
                    throw new RuntimeException("Error in payment initialization. Returned error: " + responseData.get("error"));
                }
            } else {
                throw new RuntimeException("Error in payment initialization. HTTP errorCode: " + httpResponse);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Error in payment initialization", ex);
        }

        //Return the generated URL
        return redirectionUrl;
    }

    /*
    * Create the HTTP client with timeout configuration of 20 seconds
     */
    private static CloseableHttpClient createHttpClient (Map<String, String> configuration) {
        int timeout = Integer.valueOf(configuration.get("httpPostTimeout"));
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout * 1000)
                .setConnectionRequestTimeout(timeout * 1000)
                .setSocketTimeout(timeout * 1000).build();

        CookieStore httpCookieStore = new BasicCookieStore();
        CloseableHttpClient client = HttpClientBuilder.create()
                .setDefaultCookieStore(httpCookieStore)
                .setDefaultRequestConfig(config)
                .build();

        return client;
    }

    /*
    * Create a list of all parameters that will be sent to payment platform.
    *
    * This fields need to be signed using the private key of the target shop
     */
    private List<NameValuePair> createFormParameters(Integer transactionId, String email, String amount, String currency, String mode, String language, String cardType, String orderId, Map<String, String> configuration) {
        String merchantSiteId = configuration.get("merchantSiteId");
        String usedMerchantKey = configuration.get("usedMerchantKey");

        //List that contains the parameters of the form used to create payment
        List<NameValuePair> formParameters = new ArrayList<>();

        formParameters.add(new BasicNameValuePair("vads_action_mode", "INTERACTIVE"));
        formParameters.add(new BasicNameValuePair("vads_amount", amount));
        formParameters.add(new BasicNameValuePair("vads_ctx_mode", mode));
        formParameters.add(new BasicNameValuePair("vads_currency", currency));
        if (StringUtils.isNotEmpty(email)) {
        	formParameters.add(new BasicNameValuePair("vads_cust_email", email));
        }

        formParameters.add(new BasicNameValuePair("vads_language", language));
        if (StringUtils.isNotEmpty(orderId)) {
            formParameters.add(new BasicNameValuePair("vads_order_id", orderId));
        }
        formParameters.add(new BasicNameValuePair("vads_page_action", "PAYMENT"));

        //Set the card type if provided
        if (StringUtils.isNotEmpty(cardType)) {
            formParameters.add(new BasicNameValuePair("vads_payment_cards", cardType.toUpperCase()));
        }

        formParameters.add(new BasicNameValuePair("vads_payment_config", "SINGLE"));
        formParameters.add(new BasicNameValuePair("vads_site_id", merchantSiteId));

        /*
            Set the simplified mode for payment page.
            This mode does not show extra logos and language item,
            reducing the loading time drastically in high latency cases
         */
        formParameters.add(new BasicNameValuePair("vads_theme_config", "SIMPLIFIED_DISPLAY=true"));

        formParameters.add(new BasicNameValuePair("vads_trans_date", calculateDateFormatInUTC("yyyyMMddHHmmss")));
        formParameters.add(new BasicNameValuePair("vads_trans_id", String.format("%06d", transactionId)));

        formParameters.add(new BasicNameValuePair("vads_url_cancel", "http://webview_" + merchantSiteId + ".cancel"));
        formParameters.add(new BasicNameValuePair("vads_url_error", "http://webview_" + merchantSiteId + ".error"));
        formParameters.add(new BasicNameValuePair("vads_url_refused", "http://webview_" + merchantSiteId + ".refused"));
        formParameters.add(new BasicNameValuePair("vads_url_return", "http://webview_" + merchantSiteId + ".return"));
        formParameters.add(new BasicNameValuePair("vads_url_success", "http://webview_" + merchantSiteId + ".success"));

        formParameters.add(new BasicNameValuePair("vads_version", "V2"));

        //Create the string to sign
        String concatenateMapParams = "";
        for (NameValuePair pair : formParameters) {
            concatenateMapParams += pair.getValue() + "+";
        }
        //Add private key in signature
        concatenateMapParams += usedMerchantKey;

        //Add signature to form parameters
        formParameters.add(new BasicNameValuePair("signature", hmacSha256(concatenateMapParams, usedMerchantKey)));

        return formParameters;
    }

    /*
     * Utility method that return current UTC date in an specified format
     */
    private static String calculateDateFormatInUTC(String format) {
        Date now = new Date();
        SimpleDateFormat dateFormatter = new SimpleDateFormat(format);
        dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormatter.format(now);
    }

    /*
     * Utility method used to sign the form data using  HMAC-SHA-256 algorithm
     */
    private static String hmacSha256(String input, String key) {
        Mac hmacSha256;
        byte[] inputBytes;
        try {
            inputBytes = input.getBytes("UTF-8");
            try {
                hmacSha256 = Mac.getInstance("HmacSHA256");
            } catch (NoSuchAlgorithmException nsae) {
                hmacSha256 = Mac.getInstance("HMAC-SHA-256");
            }
            SecretKeySpec macKey = new SecretKeySpec(key.getBytes("UTF-8"), "RAW");
            hmacSha256.init(macKey);
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }

        return Base64.getEncoder().encodeToString(hmacSha256.doFinal(inputBytes));
    }
}

/**
 * Simple implementation of a generator for transactionId (this number must be unique by day)<p></p>
 *
 * Note that this simple implementation does not guarantee an unique transactionId. If you want to perform a
 * stronger implementation you should verify each generation using one storage based system (database, files, etc)
 */
class TransactionIdGenerator {
    private static int counter = new Random().nextInt(500000);
    private static int maxCounter = 999999;

    public static synchronized int generateNewTransactionId() {
        if (counter == (maxCounter - 1))
            counter = 0;

        return counter++;
    }
}