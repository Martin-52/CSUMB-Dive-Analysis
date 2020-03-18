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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import java.util.logging.Level;
import java.util.logging.Logger;

public class PhotoAnnotationService implements Service {
    private final Logger log = Logger.getLogger(getClass().getName());

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/{rov}/{diveNumber}", (req, res) -> {
            try {
                getRovPhotoAnnotations(req, res);
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
    private void getRovPhotoAnnotations(ServerRequest request, ServerResponse response)
            throws IOException, InterruptedException {

        String rovName = request.path().param("rov");
        int diveNumber = Integer.parseInt(request.path().param("diveNumber"));

        JsonObject allAnnotationData = getAnnotationData(rovName, diveNumber);
        JsonObject photoLinksAndAnnotations = getPhotoUrlsAndAnnotations(allAnnotationData);

        response.headers().add("Access-Control-Allow-Origin", "*");
        response.headers().add("Access-Control-Allow-Headers", "*");
        response.headers().add("Access-Control-Allow-Credentials", "true");
        response.headers().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        response.send(photoLinksAndAnnotations.toString());
    }

    /**
     * Sends http request to retrieve the json for the given rov and dive number
     * 
     * @param rovName
     * @param diveNumber
     */
    private JsonObject getAnnotationData(String rovName, int diveNumber) throws IOException, InterruptedException {

        final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
        String path = "http://dsg.mbari.org/references/query/dive/" + rovName + "%20" + diveNumber;

        HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create(path))
                .setHeader("User-Agent", "Java 11 HttpClient Bot").build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return (new JsonParser().parse(response.body()).getAsJsonObject());
    }

    private JsonArray getAnnotations(JsonObject allAnnotationData) {
        if (allAnnotationData.isJsonNull()) {
            log.log(Level.WARNING, "Annotation Data empty - DiveAnnotationService.getAnnotations()");
            return null;
        }
        return allAnnotationData.getAsJsonArray("annotations");
    }

    JsonObject getPhotoUrlsAndAnnotations(JsonObject allAnnotationData){
        if(allAnnotationData==null){
            System.out.println("No data");
        }
        JsonArray annotations = getAnnotations(allAnnotationData);
        JsonObject jpgPhotoUrlsMap = new JsonObject();

        for(int i = 0; i < annotations.size(); i++){
            if(annotations.get(i).getAsJsonObject().get("image_references")!=null){
                int imageArrayLength = annotations.get(i).getAsJsonObject().get("image_references").getAsJsonArray().size();
                
                for(int j = 0; j < imageArrayLength; j++) {
                    String photoUrl = annotations.get(i).getAsJsonObject().get("image_references").getAsJsonArray().get(j).getAsJsonObject().get("url").getAsString();
                    String format = annotations.get(i).getAsJsonObject().get("image_references").getAsJsonArray().get(j).getAsJsonObject().get("format").getAsString();

                    if(isJpg(format)){
                        if(!jpgPhotoUrlsMap.has(photoUrl)){ // some photos have multiple annotations. photoUrl will have a list of photos as value
                            // Missing timestamp
                            String timestamp = "";
                            if(annotations.get(i).getAsJsonObject().get("recorded_timestamp")==null){
                                continue;
                            }
                            timestamp = annotations.get(i).getAsJsonObject().get("recorded_timestamp").getAsString();
                            jpgPhotoUrlsMap.add(photoUrl, new JsonObject());
                            jpgPhotoUrlsMap.get(photoUrl).getAsJsonObject().addProperty("timestamp", timestamp);
                            jpgPhotoUrlsMap.get(photoUrl).getAsJsonObject().add("annotations", new JsonArray());
                        }
                        JsonObject newAnnotation = annotations.get(i).getAsJsonObject();
                        jpgPhotoUrlsMap.get(photoUrl).getAsJsonObject().get("annotations").getAsJsonArray().add(newAnnotation);                            
                    }
                }
            }
        }

        // order annotation arrays
        for (Entry<String, JsonElement> entry : jpgPhotoUrlsMap.entrySet()) {
            JsonArray orderedAnnotations = new JsonArray();
            orderedAnnotations = sortAnnotationArray(entry.getValue().getAsJsonObject().get("annotations").getAsJsonArray());
            entry.getValue().getAsJsonObject().add("annotations", orderedAnnotations);
        }

        // get ordered list of photo links for front end
        JsonObject mappingObj = new JsonObject();
        mappingObj.add("photoMapping", getOrderedListOfPhotoLinks(jpgPhotoUrlsMap));
        jpgPhotoUrlsMap.add("mappingObject", mappingObj);

        return jpgPhotoUrlsMap;
    }

    private boolean isJpg(String format){
        return(format.equals("image/jpg"));
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
        if(annotationArry.size()<=1) {
            return annotationArry;
        }
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

    private JsonArray getOrderedListOfPhotoLinks(JsonObject jpgPhotoUrlsMap){
        // ordering the links by their timestamps
        List<JsonObject> mapping = new ArrayList<JsonObject>();
        
        // add timestamps and links to mapping list
        for (Entry<String, JsonElement> entry : jpgPhotoUrlsMap.entrySet()) {
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
