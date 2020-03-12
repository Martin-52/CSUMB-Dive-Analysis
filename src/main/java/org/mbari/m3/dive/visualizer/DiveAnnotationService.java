package org.mbari.m3.dive.visualizer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.mbari.expd.Dive;
import org.mbari.expd.DiveDAO;
import org.mbari.expd.NavigationDatum;
import org.mbari.expd.NavigationDatumDAO;
import org.mbari.expd.jdbc.DiveDAOImpl;
import org.mbari.expd.jdbc.NavigationDatumDAOImpl;
import org.mbari.expd.jdbc.NavigationDatumImpl;

import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

public class DiveAnnotationService implements Service {
    private final Logger log = Logger.getLogger(getClass().getName());

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/{rov}/{diveNumber}", (req, res) -> {
            try {
                getRovDiveAnnotations(req, res);
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });

    }

    /**
     * Returns a list of dives for the given ROV.
     * 
     * @param request
     * @param response
     * @throws InterruptedException
     * @throws IOException
     */
    private void getRovDiveAnnotations(ServerRequest request, ServerResponse response)
            throws IOException, InterruptedException {

        String rovName = request.path().param("rov");
        int diveNumber = Integer.parseInt(request.path().param("diveNumber"));

        JsonObject allAnnotationData = getVideoAndAnnotations(rovName, diveNumber);

        JsonObject linksAndAnnotations = getVidsAndAnnotations(allAnnotationData);

        response.headers().add("Access-Control-Allow-Origin", "*");
        response.headers().add("Access-Control-Allow-Headers", "*");
        response.headers().add("Access-Control-Allow-Credentials", "true");
        response.headers().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        response.send(linksAndAnnotations.toString());
    }



    /**
     * Sends http request to retrieve the json for the given rov and dive number
     * 
     * @param rovName
     * @param diveNumber
     */
    private JsonObject getVideoAndAnnotations(String rovName, int diveNumber) throws IOException, InterruptedException {

        final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();

        String path = "http://dsg.mbari.org/references/query/dive/" + rovName + "%20" + diveNumber;

        HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create(path))
                .setHeader("User-Agent", "Java 11 HttpClient Bot").build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return (new JsonParser().parse(response.body()).getAsJsonObject());
    }

    /**
     * Returns a JsonArray of all Annotations from specific dive
     * 
     * @param allAnnotationData
     */
    private JsonArray getAnnotations(JsonObject allAnnotationData) {
        if (allAnnotationData.isJsonNull()) {
            log.log(Level.WARNING, "Annotation Data empty - DiveAnnotationService.getAnnotations()");
            return null;
        }
        return allAnnotationData.getAsJsonArray("annotations");
    }

    /**
     * Returns a JsonArray of all Media (includes video links) from specific dive
     * 
     * @param allAnnotationData
     */
    private JsonArray getMedia(JsonObject allAnnotationData) {
        if (allAnnotationData.isJsonNull()) {
            log.log(Level.WARNING, "Annotation Data empty - DiveAnnotationService.getMedia()");
            return null;
        }
        return allAnnotationData.getAsJsonArray("media");
    }

    /**
     * Returns a JsonArray of all Video Links from 'Media' for specific dive
     * 
     * @param allAnnotationData
     */
    private JsonArray getVideoLinks(JsonObject allAnnotationData) {
        if (allAnnotationData.isJsonNull()) {
            log.log(Level.WARNING, "Annotation Data empty - DiveAnnotationService.getVideoLinks()");
            return null;
        }
        JsonArray allDiveVideos = new JsonArray();

        JsonArray media = allAnnotationData.getAsJsonArray("media");
        for (int i = 0; i < media.size(); i++) {
            String uri = media.get(i).getAsJsonObject().get("uri").toString();
            if (uri.charAt(uri.length() - 2) == '4') {
                allDiveVideos.add(media.get(i).getAsJsonObject().get("uri"));
            }
        }
        return allDiveVideos;
    }


    public JsonObject getVidsAndAnnotations(JsonObject allAnnotationData) {
        if (allAnnotationData.isJsonNull()) {
            log.log(Level.WARNING, "Annotation Data empty - DiveAnnotationService.getVideoLinksAndAnnotations()");
            return null;
        }
        JsonArray allMedia = getMedia(allAnnotationData);
        JsonArray videoLinks = getVideoLinks(allAnnotationData);
        JsonObject linksAndUUID = new JsonObject(); // linksAndUUID eventually become links and their annotations
        JsonArray mp4WithNoAvailableMov = new JsonArray();

        for(int j = 0; j < videoLinks.size();j++) {
            for(int i = 0; i < allMedia.size(); i++) {   
                String video_reference_uuid = "";
                if(videoLinks.get(j).toString().equals(allMedia.get(i).getAsJsonObject().get("uri").toString())){
                    
                    String uri = videoLinks.get(j).getAsString();
                    linksAndUUID.add(videoLinks.get(j).getAsString(), new JsonObject());
                    //linksAndUUID.add(videoLinks.get(j).toString(), new JsonObject());
                    video_reference_uuid = getVideoReferenceUUID(allMedia.get(i).getAsJsonObject().get("video_uuid").toString(),allAnnotationData);
                                                            // length == 0 means that this mp4 video does not have a matching mov file. 
                                                            // It needs a matching mov file for us to get the
                                                            // video_reference_uuid that will get us the matching annotations
                    if(video_reference_uuid.length()==0){                                           
                        linksAndUUID.get(uri).getAsJsonObject().addProperty("video_reference_uuid", "No video_reference_uuid");
                        // No mov available
                        mp4WithNoAvailableMov.add(videoLinks.get(j).toString());
                        
                    } else { 
                        linksAndUUID.get(uri).getAsJsonObject().addProperty("video_reference_uuid",video_reference_uuid.substring(1, video_reference_uuid.length()-1));
                    }
                    linksAndUUID.get(uri).getAsJsonObject().addProperty("timestamp", allMedia.get(i).getAsJsonObject().get("start_timestamp").toString().substring(1, allMedia.get(i).getAsJsonObject().get("start_timestamp").toString().length() - 1));
                }
            }
        }
        
        // gets and orders annotations
        for (Entry<String, JsonElement> entry : linksAndUUID.entrySet()) {
            if(!entry.getValue().getAsJsonObject().get("video_reference_uuid").toString().substring(1,entry.getValue().getAsJsonObject().get("video_reference_uuid").toString().length()-1).equals("No video_reference_uuid")){
                entry.getValue().getAsJsonObject().add("annotations", getAnnotationsByVideoReferenceUUID(entry.getValue().getAsJsonObject().get("video_reference_uuid").toString(), allAnnotationData));
                
                if(entry.getValue().getAsJsonObject().get("annotations").getAsJsonArray().size()>0){
                    JsonArray newSortedArray = sortAnnotationArray(entry.getValue().getAsJsonObject().get("annotations").getAsJsonArray());
                    entry.getValue().getAsJsonObject().add("annotations", newSortedArray);
                }

            } else {
                entry.getValue().getAsJsonObject().add("annotations", new JsonArray());
            }
            entry.getValue().getAsJsonObject().remove("video_reference_uuid");
        }

        // add mapping to linksAndUUID
        // i made an object in case i wanted to add another variable
        JsonObject mappingObj = new JsonObject();
        mappingObj.add("videoMapping", getOrderedListOfMp4Links(linksAndUUID));
        linksAndUUID.add("mappingObject", mappingObj);

        return linksAndUUID;
    }

    /**
    * This function takes the video_reference_uuid. It will return a list (Gson list) of annotations
    * @param video_reference_uuid
    */
    public JsonArray getAnnotationsByVideoReferenceUUID(String video_reference_uuid,JsonObject allAnnotationData){
        if(allAnnotationData.isJsonNull()) {
            log.log(Level.WARNING, "Annotation Data empty - DiveAnnotationService.getAnnotationsByVideoReferenceUUID()");
            return null;
        }

        JsonArray allAnnotations = getAnnotations(allAnnotationData);
        JsonArray videoAnnotations = new JsonArray();
        for(int i = 0; i < allAnnotations.size(); i++) {
            if(video_reference_uuid.equals(allAnnotations.get(i).getAsJsonObject().get("video_reference_uuid").toString())){
                videoAnnotations.add(allAnnotations.get(i).getAsJsonObject());
            }
        }
        return videoAnnotations;
    }

    // I am looking for the video_uuid that has the .mov video file, not .mp4
    public String getVideoReferenceUUID(String video_uuid,JsonObject allAnnotationData){
        if(allAnnotationData.isJsonNull()) {
            log.log(Level.WARNING, "Annotation Data empty - DiveAnnotationService.getVideoReferenceUUID()");
            return null;
        }

        JsonArray allMedia = getMedia(allAnnotationData);
        String video_reference_uuid = "";

        for(int i = 0; i < allMedia.size(); i++) {
            String curr_uuid = allMedia.get(i).getAsJsonObject().get("video_uuid").toString();
            if(video_uuid.equals(curr_uuid)){
                String curr_uri = allMedia.get(i).getAsJsonObject().get("uri").toString();
                if(curr_uri.charAt(curr_uri.length()-2)=='v') {
                    video_reference_uuid = allMedia.get(i).getAsJsonObject().get("video_reference_uuid").toString();
                    break;
                }
            }
        }
        return video_reference_uuid;
    } 
    
    public JsonArray getROVPathFromAnnotations(JsonObject allAnnotationData) {
        if(allAnnotationData.isJsonNull()) {
            log.log(Level.WARNING, "Annotation Data empty - DiveAnnotationService.getROVPathFromAnnotations()");
            return null;
        }
        JsonArray annotations = getAnnotations(allAnnotationData);
        
        JsonArray allRovInfo = new JsonArray();

        for (int i = 0; i < annotations.size(); i++){
            if(annotations.get(i).getAsJsonObject().get("ancillary_data") != null){
                if(annotations.get(i).getAsJsonObject().get("ancillary_data").getAsJsonObject().get("latitude") != null 
                    && annotations.get(i).getAsJsonObject().get("ancillary_data").getAsJsonObject().get("longitude") != null 
                    && annotations.get(i).getAsJsonObject().get("ancillary_data").getAsJsonObject().get("depth_meters") != null){
                    JsonObject rovInfo = new JsonObject();
                    rovInfo.addProperty("latitude", annotations.get(i).getAsJsonObject().get("ancillary_data").getAsJsonObject().get("latitude").getAsString());
                    rovInfo.addProperty("longitude", annotations.get(i).getAsJsonObject().get("ancillary_data").getAsJsonObject().get("longitude").getAsString());
                    rovInfo.addProperty("depth_meters", annotations.get(i).getAsJsonObject().get("ancillary_data").getAsJsonObject().get("depth_meters").getAsString());
                    allRovInfo.add(rovInfo);
                }
            }
        }
        return allRovInfo;
    }

    private String getTimeFromTimestamp(String timestamp) {
        int indexToGetTime = 1;
        for(int k = 0; k < timestamp.length();k++){
            if(timestamp.charAt(k)=='T'){
                break;
            }
            indexToGetTime++;
        }
        return timestamp.substring(indexToGetTime,indexToGetTime+8).replace(":", "");
    }

    private JsonArray sortAnnotationArray(JsonArray annotationArry){
        List<JsonObject> mapping = new ArrayList<JsonObject>();

        for(int i = 0; i < annotationArry.size();i++) {
            mapping.add(i, annotationArry.get(i).getAsJsonObject());
        }

        //sorts mapping by timestamp
        Collections.sort(mapping, new Comparator<JsonObject>() {
            @Override
            public int compare(JsonObject obj1, JsonObject obj2) {
                
                // first we need to clean the timestamps 
                // we can check if String is good
                String time1 = getTimeFromTimestamp(obj1.get("recorded_timestamp").getAsString());
                String time2 = getTimeFromTimestamp(obj2.get("recorded_timestamp").getAsString());

                if(time1==null || time2==null){
                    return 0;
                }
                if(Integer.parseInt(time1) > Integer.parseInt(time2)){
                    return 1;
                }
             return -1;
            }
        });

        // back to jsonArray
        JsonArray temp = new JsonArray();
        for(int i = 0; i < mapping.size();i++) {
            temp.add(mapping.get(i).getAsJsonObject());
        }
        return temp;
    }

    private JsonArray getOrderedListOfMp4Links(JsonObject linksAndUUID){
        // ordering the links by their timestamps
        List<JsonObject> mapping = new ArrayList<JsonObject>();
        
        // add timestamps and links to mapping list
        for (Entry<String, JsonElement> entry : linksAndUUID.entrySet()) {
            String timestamp = entry.getValue().getAsJsonObject().get("timestamp").getAsString();
            int i = 0;
            for(; i < timestamp.length();i++){
                if(timestamp.charAt(i)=='T') break;
            }

            String temp = timestamp.substring(i+1,i+1+8);
            String time = temp.replace(":", "");

            JsonObject newObj = new JsonObject();
            newObj.addProperty("link", entry.getKey());
            newObj.addProperty("timestamp", Integer.parseInt(time)); 
                
            mapping.add(newObj);
        }

        // sorts mapping by timestamp
        Collections.sort(mapping, new Comparator<JsonObject>() {
            @Override
            public int compare(JsonObject obj1, JsonObject obj2) {
                if(obj1.get("timestamp")==null || obj2.get("timestamp")==null){
                    return 0;
                }
                if(obj1.get("timestamp").getAsInt() > obj2.get("timestamp").getAsInt()){
                    return 1;
                }
                return -1;
            }
        });
        
        // making finalMapping to turn mapping into a jsonArray
        JsonArray finalMapping = new JsonArray();
        for(int i = 0; i < mapping.size();i++) {
            finalMapping.add(mapping.get(i).get("link"));
        }
        return finalMapping;
    }
}
