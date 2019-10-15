package com.lyra;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * This class encapsulates all configuration data needed to perform payments calling payment platform.<p></p>
 *
 * It reads the configuration from app-configuration.properties file. Please make sure that you have set all the configuration
 * parameters before running the server.
 *
 * @author Lyra Network
 */
public class ServerConfiguration {
    private static Map<String, String> appConfiguration = new HashMap<>();
    private static String CONF_FILENAME = "app-configuration.properties";

    //No public constructor
    private ServerConfiguration() {
    }

    /**
     * This method calculates the configuration data needed to connect to the payment platform.<p></p>
     * It sets also the mode of payment (TEST or PRODUCTION)
     *
     * @param mode             TEST or PRODUCTION
     * @return a {@link Map} object that contains the configuration to use
     */
    public static Map<String, String> getConfiguration(String mode) {

        if (appConfiguration.isEmpty()) {
            Properties configurationProperties = new Properties();

            try (InputStream is = ServerConfiguration.class.getClassLoader()
                    .getResourceAsStream(CONF_FILENAME)) {

                if (is != null) {
                    configurationProperties.load(is);
                } else {
                    throw new IOException("Could not read configuration file: " + CONF_FILENAME);
                }

                appConfiguration = (Map) configurationProperties;
            } catch (IOException e) {
                //This will be captured and handled by controller
                throw new RuntimeException("Cannot read configuration data. Please, provide a valid app.configuration file", e);
            }

            appConfiguration.put("mode", mode);
            if ("PRODUCTION".equals(mode)) {
                appConfiguration.put("usedMerchantKey", appConfiguration.get("merchantProdKey"));
            } else {
                appConfiguration.put("usedMerchantKey", appConfiguration.get("merchantTestKey"));
            }
        }

        return appConfiguration;
    }
}