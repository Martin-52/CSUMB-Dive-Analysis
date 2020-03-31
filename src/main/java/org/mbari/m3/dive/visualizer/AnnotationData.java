package org.mbari.m3.dive.visualizer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map.Entry;
import java.util.Set;
import io.helidon.config.Config; 


public class AnnotationData {
    private final JsonObject annotationData;
    private static int objectCounter = 0;
    Config config = Config.create();

    AnnotationData(){
        this.annotationData = new JsonObject();
    }

    AnnotationData(JsonObject annotationData){
        this.annotationData = annotationData;
    }

    AnnotationData(String rovName, int diveNumber) throws IOException, InterruptedException {
        this.annotationData = initializeAnnotationData(rovName, diveNumber);
    }

    public static AnnotationData get(JsonObject data) {
        objectCounter++;
        return new AnnotationData(data);
    }

    public JsonObject getData(){
        return this.annotationData;
    }

    public int getSize(){
        return AnnotationData.objectCounter;
    }

    public JsonObject initializeAnnotationData(String rovName,int diveNumber) throws IOException, InterruptedException {
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