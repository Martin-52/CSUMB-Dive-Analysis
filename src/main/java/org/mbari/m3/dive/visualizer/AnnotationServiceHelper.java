package org.mbari.m3.dive.visualizer;

import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.Collection;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.mbari.expd.Dive;
import org.mbari.expd.DiveDAO;
import org.mbari.expd.jdbc.DiveDAOImpl;
import org.json.simple.JSONObject;



import io.helidon.webserver.Routing;

public class AnnotationServiceHelper {


    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();

    // private HttpResponse<String> response;
    private JsonObject allAnnotationData;



    AnnotationServiceHelper(String rov,Integer number) throws IOException, InterruptedException {
        // when initialized, this.sendGET() will get the entire json tree needed
        this.sendGET(rov, number);
    }



    private void sendGET(String rov, Integer number) throws IOException, InterruptedException {
        String path = "http://dsg.mbari.org/references/query/dive/" + rov + "%20" + number;
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(path))
                .setHeader("User-Agent", "Java 11 HttpClient Bot") // add request header
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // I am using gson. First i get the entire json object. then i get the annotations list from within the json tree.
        // the annotations list contains an annotation of what the user saw from within a 15 min chunk of video. that video is 
        // referenced within the annotation.
        this.allAnnotationData = new JsonParser().parse(response.body()).getAsJsonObject();
    }

    public JsonArray getAnnotations(){
        if(this.allAnnotationData.isJsonNull()) {
            System.out.println("EMPTY ANNOTATION TREE");
            return null;
        }
        return allAnnotationData.getAsJsonArray("annotations");
    }

    public JsonArray getVideoLinks(){
        if(this.allAnnotationData.isJsonNull()) {
            System.out.println("EMPTY ANNOTATION TREE");
            return null;
        }
        JsonArray allDiveVideos = new JsonArray();
        // All videos in tree
        JsonArray media = allAnnotationData.getAsJsonArray("media"); 
        for(int i = 0;i < media.size(); i++){
            String uri = media.get(i).getAsJsonObject().get("uri").toString();
            if(uri.charAt(uri.length()-2)=='4'){//checks last char is a '4' and not 'm'
                allDiveVideos.add(media.get(i).getAsJsonObject().get("uri").toString());
            }
        }
        return allDiveVideos;
    }
}













//     JsonArray annotations = allAnnotationData.getAsJsonArray("annotations");
// String videoUUID = "";
// String videoLink = "";


        // first 15 minute video chunk (first annotation video reference uuid).
        // String observationUUID = annotations.get(0).getAsJsonObject().get("observation_uuid").toString();
        // String videoReferenceUUID = annotations.get(0).getAsJsonObject().get("video_reference_uuid").toString();

        // for(int i = 0;i < media.size(); i++){
        //     if(media.get(i).getAsJsonObject().get("video_reference_uuid").toString().equals(videoReferenceUUID)){
        //         // get mp4 uuid
        //         videoUUID = media.get(i).getAsJsonObject().get("video_uuid").toString();
                
        //     }
        // }


        // the media lists seems to be in some order. Each mov seems to be followed by its mp4 varient. but to be sure it is the correct
        // video, im going to loop through the media again.
        // for(int i = 0;i < media.size(); i++){
        //     if(media.get(i).getAsJsonObject().get("video_uuid").toString().equals(videoUUID)){
        //         String uri = media.get(i).getAsJsonObject().get("uri").toString();
        //         if(uri.charAt(uri.length()-2)=='4'){//checks last char is a '4' and not 'm'
        //             videoLink = media.get(i).getAsJsonObject().get("uri").toString();
        //         }
        //     }
        // }


        // THIS IS AN EXAMPLE. THIS GETS THE FIRST ANNOTATION VIDEO
        // System.out.println("==================");
        // gson JsonArray functions: https://www.javadoc.io/doc/com.google.code.gson/gson/2.6.2/com/google/gson/JsonArray.html
        // System.out.println("annotation uuid: " + observationUUID);
        // System.out.println("video uuid: " + videoUUID);
        // System.out.println("video link (uri): " + videoLink);
        // System.out.println("==================");