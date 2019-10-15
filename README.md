The aim of this repository is to explain how to easily implement a merchant server in order that will be used from a webview mobile payment integration.

* This example uses [Spark](http://sparkjava.com/) framework that allows to create a simple server easily
* In order to perform a Post HTTP call easily, this example uses HttpClient (https://hc.apache.org/httpcomponents-client-ga/index.html)   

## Table of contents

* [Getting started](#getting-started)
* [What's included](#whats-included)
* [Configuration](#configuration)

## Getting started

* Clone the repo: `git clone https://github.com/lyra/webview-payment-sparkjava-integration-sample.git`.
* Set your shop data in the _app-configuration.properties_ file as described in [configuration instructions](#configuration).
* Run `mvn package`, to build jar executable
* Run `java -jar inApp-server-1.0-jar-with-dependencies.jar`

The application should have and output like this: 

    INFO org.eclipse.jetty.util.log - Logging initialized @698ms
    INFO spark.embeddedserver.jetty.EmbeddedJettyServer - == Spark has ignited ...
    INFO spark.embeddedserver.jetty.EmbeddedJettyServer - >> Listening on 0.0.0.0:9090
    INFO org.eclipse.jetty.server.Server - jetty-9.3.6.v20151106
    INFO org.eclipse.jetty.server.ServerConnector - Started ServerConnector@30946381{HTTP/1.1,[http/1.1]}{0.0.0.0:5678}
    INFO org.eclipse.jetty.server.Server - Started @1065ms

By default the application run on 9090 port. See [configuration instructions](#configuration) if you want to change this value.

## What's included

```
|---com.lyra
|   |-- Server.java 
|   |-- ServerConfiguration.java
|---com.lyra.redirection  
|   |-- RedirectionController.java
|   |-- RedirectionService.java
|---resources
|   |-- app-configuration.properties
```

## Configuration

Server port can be easily configured via [Spark](http://sparkjava.com/) modifying the _Server.java_ file. 

```java
port(9090); 
```

All the shop configuration data must be set in the _app-configuration.properties_ file: 

```
#
# EDIT YOUR MERCHANT SETTINGS HERE
#

merchantSiteId=#Your merchant Id here
merchantTestKey=#Your test certificate here
merchantProdKey=#Your production certificate here

#
# URL of your payment platform.
#
paymentPlatformUrl=#Your payment platform URL here
```
