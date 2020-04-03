package org.mbari.m3.dive.visualizer;

import io.helidon.webserver.ServerResponse;
import java.io.IOException;

public class Utilities{

    public void headersRespondSend(String dataToSend, ServerResponse response) throws IOException, InterruptedException {
        response.headers().add("Access-Control-Allow-Origin", "*");
        response.headers().add("Access-Control-Allow-Headers", "*");
        response.headers().add("Access-Control-Allow-Credentials", "true");
        response.headers().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        response.send(dataToSend);
    }
}