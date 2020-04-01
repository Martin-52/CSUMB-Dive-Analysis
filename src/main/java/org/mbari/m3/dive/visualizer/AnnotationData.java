package org.mbari.m3.dive.visualizer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.helidon.webserver.ServerRequest;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import io.helidon.config.Config; 


public class AnnotationData {
    private final Logger log = Logger.getLogger(getClass().getName());
    Config config = Config.create();



    public JsonObject getAnnotationDataFromCache(ServerRequest request){
        String rovName = request.path().param("rov");
        int diveNumber = Integer.parseInt(request.path().param("diveNumber"));

        SingletonCache cacheWrapper = SingletonCache.getInstance();

        String data = cacheWrapper.cache.get("MBARIAnnotationData"+rovName+diveNumber, k -> {
            try {
                return initializeAnnotationData(rovName, diveNumber).toString();
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                log.log(Level.WARNING, "Unable to set and get annotation data - DiveAnnotationService.getAnnotationData()");
                
                e.printStackTrace();
            }
            return "{}";
        });

        return (new JsonParser().parse(data).getAsJsonObject());
    }


    private JsonObject initializeAnnotationData(String rovName,int diveNumber) throws IOException, InterruptedException {
        final HttpClient httpClient = HttpClient
            .newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();

        if(rovName.contains(" ")){// These are for rov names with a space (i.e Mini Rov & Doc Rickett)
            rovName = rovName.replace(" ","%20");
        }

        String mbariPath = config.get("mbariPath").asString().get();        

        String path = mbariPath + rovName + "%20" + diveNumber;

        HttpRequest request = HttpRequest
            .newBuilder()
            .GET()
            .uri(URI.create(path))
            .setHeader("User-Agent", "Java 11 HttpClient Bot")
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return (new JsonParser().parse(response.body()).getAsJsonObject());
    }
}