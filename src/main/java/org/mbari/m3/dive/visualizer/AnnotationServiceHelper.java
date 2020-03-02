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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Multiset.Entry;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;

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
            System.out.println("EMPTY ANNOTATION TREE - AnnotationServieHelper.getAnnotations()");
            return null;
        }
        return allAnnotationData.getAsJsonArray("annotations");
    }

    public JsonArray getMedia(){
        if(this.allAnnotationData.isJsonNull()) {
            System.out.println("EMPTY ANNOTATION TREE - AnnotationServieHelper.getMedia()");
            return null;
        }
        return allAnnotationData.getAsJsonArray("media");
    }

    public JsonArray getVideoLinks(){
        if(this.allAnnotationData.isJsonNull()) {
            System.out.println("EMPTY ANNOTATION TREE - AnnotationServieHelper.getVideoLinks()");
            return null;
        }
        JsonArray allDiveVideos = new JsonArray();
        
        JsonArray media = allAnnotationData.getAsJsonArray("media"); 
        for(int i = 0;i < media.size(); i++){
            String uri = media.get(i).getAsJsonObject().get("uri").toString();
            if(uri.charAt(uri.length()-2)=='4'){//checks last char is a '4' and not 'm'
                allDiveVideos.add(media.get(i).getAsJsonObject().get("uri")); // Testing if i can get the uri without turning it into a string when adding it to the AlldiveVideos list
            }
        }
        return allDiveVideos;
    }

    // i am looking for the video_uuid that has the .mov video file, not .mp4
    public String getVideoReferenceUUID(String video_uuid){
        if(this.allAnnotationData.isJsonNull()) {
            System.out.println("EMPTY ANNOTATION TREE - AnnotationServieHelper.getVideoReferenceUUID()");
            return null;
        }

        // loop through the media
        JsonArray allMedia = getMedia();
        String video_reference_uuid = "";

        for(int i = 0; i < allMedia.size(); i++) {   
            // i have link, must get the uuid, now get the video_reference_uuid. 
            String curr_uuid = allMedia.get(i).getAsJsonObject().get("video_uuid").toString();
            if(video_uuid.equals(curr_uuid)){
                //System.out.println(video_uuid + " matched " + curr_uuid);
                // need to check the uri file name
                String curr_uri = allMedia.get(i).getAsJsonObject().get("uri").toString();
                if(curr_uri.charAt(curr_uri.length()-2)=='v') {
                    //System.out.println("current uri.mov " + curr_uri);
                    //System.out.println(".mov video_reference_uuid: " + allMedia.get(i).getAsJsonObject().get("video_reference_uuid").toString());
                    video_reference_uuid = allMedia.get(i).getAsJsonObject().get("video_reference_uuid").toString();
                    break;
                }
            }
        }
        return video_reference_uuid;
    }

    // This function takes the video_reference_uuid. It will return a list (Gson list) of annotations
    public JsonArray getAnnotationsByVideoReferenceUUID(String video_reference_uuid){
        if(this.allAnnotationData.isJsonNull()) {
            System.out.println("EMPTY ANNOTATION TREE - AnnotationServieHelper.getAnnotationsByVideoReferenceUUID()");
            return null;
        }

        JsonArray allAnnotations = getAnnotations();
        JsonArray videoAnnotations = new JsonArray();
        for(int i = 0; i < allAnnotations.size(); i++) {
            if(video_reference_uuid.equals(allAnnotations.get(i).getAsJsonObject().get("video_reference_uuid").toString())){
                videoAnnotations.add(allAnnotations.get(i).getAsJsonObject());
            }
        }
        return videoAnnotations;
    }

    public JsonObject getVideoLinksAndAnnotations(){
        if(this.allAnnotationData.isJsonNull()) {
            System.out.println("EMPTY ANNOTATION TREE - AnnotationServieHelper.linksAndAnnotations()");
            return null;
        }
        JsonArray allAnnotations = getAnnotations();
        JsonArray allMedia = getMedia();
        JsonArray videoLinks = getVideoLinks();
        JsonObject linksAndUUID = new JsonObject();

        // ================================================================================================ //
        // 
        //                      linksAndUUID eventually become links and their annotations 
        // 
        // ================================================================================================ //

        
        // it might be better to get the annotations for a video after it is clicked rather than getting them all at once. 


        // this loop loops through all links("uri"'s). for each link it will find its own media annotation (by matching uri's). For each link (uri)
        // it will call getVideoReferenceUUID function. look at getVideoUUID function for more detail. 
        // Once the video_reference_uuid is returned, it is checked if it is empty. If it is "No video_reference_uuid" tag
        // becomes the value. If it isnt empty, it becomes the value to the link
        for(int j = 0; j < videoLinks.size();j++) {
            for(int i = 0; i < allMedia.size(); i++) {   
                // i have link, must get the uuid, now get the video_reference_uuid. 
                String video_reference_uuid = "";
    
                if(videoLinks.get(j).toString().equals(allMedia.get(i).getAsJsonObject().get("uri").toString())){
                    video_reference_uuid = getVideoReferenceUUID(allMedia.get(i).getAsJsonObject().get("video_uuid").toString());
                    if(video_reference_uuid.length()==0){   // length == 0 means that this mp4 video does not have a matching mov file. 
                                                            //It needs a matching mov file for us to get the
                                                            // video_reference_uuid that will get us the matching annotations
                        linksAndUUID.addProperty(videoLinks.get(j).toString(), "No video_reference_uuid");
                        
                    } else { 
                        linksAndUUID.addProperty(videoLinks.get(j).toString(), video_reference_uuid.substring(1, video_reference_uuid.length()-1));// trimming excess quotation marks
                    }
                    //System.out.println(allMedia.get(i).getAsJsonObject().get("video_uuid").toString() + " ----- " + video_reference_uuid);
                };
            }
        }
        
        
        // now that i got the links and their video_reference_uuids, time to get the annotations to each video link
        for(Map.Entry<String,JsonElement> entry : linksAndUUID.entrySet()){
            if(entry.getValue().toString().substring(1,entry.getValue().toString().length()-1).equals("No video_reference_uuid")){
                entry.setValue(new JsonArray()); // If there is no video_reference_uuid, it cannot have annotations
                continue;
            }
            entry.setValue(getAnnotationsByVideoReferenceUUID(entry.getValue().toString()));
        }
        
        return linksAndUUID;
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