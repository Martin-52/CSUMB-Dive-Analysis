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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.mbari.expd.jdbc.BaseDAOImpl;
import org.mbari.expd.Dive;
import org.mbari.expd.DiveDAO;
import org.mbari.expd.jdbc.DiveDAOImpl;
import org.mbari.expd.jdbc.NavigationDatumDAOImpl;
import org.mbari.expd.DiveDAO;
import org.mbari.expd.jdbc.DiveDAOImpl;
import org.mbari.expd.NavigationDatum;
import org.mbari.expd.NavigationDatumDAO;

import org.mbari.expd.CtdDatum;
import org.mbari.expd.CtdDatumDAO;
import org.mbari.expd.jdbc.CtdDatumDAOImpl;
//import org.mbari.expd.jdbc.NavigationDAOImpl;

public class DiveAnnotationService implements Service {

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

        getROVPath(allAnnotationData);


        JsonObject linksAndAnnotations = getVideoLinksAndAnnotations(allAnnotationData);
        System.out.println("************************************************************************************");
        for(Map.Entry<String,JsonElement> entry : linksAndAnnotations.entrySet()){
    
            
            System.out.println("Video Link: " + entry.getKey());
            if(entry.getValue().getAsJsonArray().size()==0){
                System.out.println("No Annotations");
                System.out.println("************************************************************************************");
                continue;
            }
            
            // this will loop through each annotation
            for(int j = 0; j < entry.getValue().getAsJsonArray().size(); j++){
                System.out.println("Video Annotation " + (j+1));
                System.out.println(entry.getValue().getAsJsonArray().get(j));
                System.out.println("");
            }
            System.out.println("************************************************************************************");
        }

        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");

        String vaa = linksAndAnnotations.toString();
        System.out.println(vaa);

        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");

        JsonObject ans = new JsonParser().parse(vaa).getAsJsonObject();
        System.out.println("************************************************************************************");
        for(Map.Entry<String,JsonElement> entry : ans.entrySet()){
    
            
            System.out.println("Video Link: " + entry.getKey());
            if(entry.getValue().getAsJsonArray().size()==0){
                System.out.println("No Annotations");
                System.out.println("************************************************************************************");
                continue;
            }
            
            // this will loop through each annotation
            for(int j = 0; j < entry.getValue().getAsJsonArray().size(); j++){
                System.out.println("Video Annotation " + (j+1));
                System.out.println(entry.getValue().getAsJsonArray().get(j));
                System.out.println("");
            }
            System.out.println("************************************************************************************");
        }

        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");

        response.send(linksAndAnnotations.toString());
    }

    /**
    * Sends http request to retrieve the json for the given rov and dive number
    * @param rovName
    * @param diveNumber
    */
    private JsonObject getVideoAndAnnotations(String rovName, int diveNumber) throws IOException, InterruptedException {

       final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();

        String path = "http://dsg.mbari.org/references/query/dive/" + rovName + "%20" + diveNumber;
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(path))
                .setHeader("User-Agent", "Java 11 HttpClient Bot")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return (new JsonParser().parse(response.body()).getAsJsonObject());
    }

    
    /**
    * Returns a JsonArray of all Annotations from specific dive
    * @param allAnnotationData
    */
    private JsonArray getAnnotations(JsonObject allAnnotationData){
        if(allAnnotationData.isJsonNull()) {
            System.out.println("EMPTY ANNOTATION TREE - AnnotationServieHelper.getAnnotations()");
            return null;
        }
        return allAnnotationData.getAsJsonArray("annotations");
    }

    /**
    * Returns a JsonArray of all Media (includes video links) from specific dive
    * @param allAnnotationData
    */
    private JsonArray getMedia(JsonObject allAnnotationData){
        if(allAnnotationData.isJsonNull()) {
            System.out.println("EMPTY ANNOTATION TREE - AnnotationServieHelper.getMedia()");
            return null;
        }
        return allAnnotationData.getAsJsonArray("media");
    }


    /**
    * Returns a JsonArray of all Video Links from 'Media' for specific dive
    * @param allAnnotationData
    */
    private JsonArray getVideoLinks(JsonObject allAnnotationData){
        if(allAnnotationData.isJsonNull()) {
            System.out.println("EMPTY ANNOTATION TREE - AnnotationServieHelper.getVideoLinks()");
            return null;
        }
        JsonArray allDiveVideos = new JsonArray();
        
        JsonArray media = allAnnotationData.getAsJsonArray("media"); 
        for(int i = 0;i < media.size(); i++){
            String uri = media.get(i).getAsJsonObject().get("uri").toString();
            if(uri.charAt(uri.length()-2)=='4'){
                allDiveVideos.add(media.get(i).getAsJsonObject().get("uri"));
            }
        }
        return allDiveVideos;
    }

    private JsonObject getVideoLinksAndAnnotations(JsonObject allAnnotationData){
        if(allAnnotationData.isJsonNull()) {
            System.out.println("EMPTY ANNOTATION TREE - AnnotationServieHelper.linksAndAnnotations()");
            return null;
        }
        JsonArray allMedia = getMedia(allAnnotationData);
        JsonArray videoLinks = getVideoLinks(allAnnotationData);
        JsonObject linksAndUUID = new JsonObject(); //  linksAndUUID eventually become links and their annotations 

        for(int j = 0; j < videoLinks.size();j++) {
            for(int i = 0; i < allMedia.size(); i++) {   
                String video_reference_uuid = "";
                if(videoLinks.get(j).toString().equals(allMedia.get(i).getAsJsonObject().get("uri").toString())){
                    video_reference_uuid = getVideoReferenceUUID(allMedia.get(i).getAsJsonObject().get("video_uuid").toString(),allAnnotationData);
                    // length == 0 means that this mp4 video does not have a matching mov file. 
                    // It needs a matching mov file for us to get the
                    // video_reference_uuid that will get us the matching annotations
                    if(video_reference_uuid.length()==0){                                           
                        linksAndUUID.addProperty(videoLinks.get(j).toString(), "No video_reference_uuid");
                    } else { 
                        linksAndUUID.addProperty(videoLinks.get(j).toString(), video_reference_uuid.substring(1, video_reference_uuid.length()-1));// trimming excess quotation marks
                    }
                };
            }
        }
                
        // now that i got the links and their video_reference_uuids, time to get the annotations to each video link
        for(Map.Entry<String,JsonElement> entry : linksAndUUID.entrySet()){
            if(entry.getValue().toString().substring(1,entry.getValue().toString().length()-1).equals("No video_reference_uuid")){
                entry.setValue(new JsonArray()); // If there is no video_reference_uuid, it cannot have annotations
                continue;
            }
            entry.setValue(getAnnotationsByVideoReferenceUUID(entry.getValue().toString(), allAnnotationData));
        }
        return linksAndUUID;
    }


    /**
    * This function takes the video_reference_uuid. It will return a list (Gson list) of annotations
    * @param video_reference_uuid
    */
    public JsonArray getAnnotationsByVideoReferenceUUID(String video_reference_uuid,JsonObject allAnnotationData){
        if(allAnnotationData.isJsonNull()) {
            System.out.println("EMPTY ANNOTATION TREE - AnnotationServieHelper.getAnnotationsByVideoReferenceUUID()");
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
            System.out.println("EMPTY ANNOTATION TREE - AnnotationServieHelper.getVideoReferenceUUID()");
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
    
    public JsonArray getROVPath(JsonObject allAnnotationData) {
        if(allAnnotationData.isJsonNull()) {
            System.out.println("EMPTY ANNOTATION TREE - getROVPath.getVideoReferenceUUID()");
            return null;
        }
        JsonArray annotations = getAnnotations(allAnnotationData);
        
        JsonArray allRovInfo = new JsonArray();

        for (int i = 0; i < annotations.size(); i++){
            System.out.println("===========");
            
            if(annotations.get(i).getAsJsonObject().get("ancillary_data") != null){
                //System.out.println(annotations.get(i).getAsJsonObject().get("ancillary_data").getAsJsonObject());
                if(annotations.get(i).getAsJsonObject().get("ancillary_data").getAsJsonObject().get("latitude") != null 
                    && annotations.get(i).getAsJsonObject().get("ancillary_data").getAsJsonObject().get("longitude") != null 
                    && annotations.get(i).getAsJsonObject().get("ancillary_data").getAsJsonObject().get("depth_meters") != null){
                    JsonObject rovInfo = new JsonObject();
                    rovInfo.addProperty("latitude", annotations.get(i).getAsJsonObject().get("ancillary_data").getAsJsonObject().get("latitude").getAsString());
                    rovInfo.addProperty("longitude", annotations.get(i).getAsJsonObject().get("ancillary_data").getAsJsonObject().get("longitude").getAsString());
                    rovInfo.addProperty("depth_meters", annotations.get(i).getAsJsonObject().get("ancillary_data").getAsJsonObject().get("depth_meters").getAsString());
                    allRovInfo.add(rovInfo);
                    
                    System.out.println("latitude: " + annotations.get(i).getAsJsonObject().get("ancillary_data").getAsJsonObject().get("latitude"));
                    System.out.println("longitude: " + annotations.get(i).getAsJsonObject().get("ancillary_data").getAsJsonObject().get("longitude"));    
                    System.out.println("depth_meters: " +annotations.get(i).getAsJsonObject().get("ancillary_data").getAsJsonObject().get("depth_meters"));    
                }
            }
        }
        //return null;
        return allRovInfo;
    }

    public List<String> getAllROVNamesEXPD(){
        return BaseDAOImpl.getAllRovNames();
    }

    public List<NavigationDatum> getROVPathEXPD(String rovName, int diveNumber){
        DiveDAO dao = new DiveDAOImpl();
        Dive dive = dao.findByPlatformAndDiveNumber(rovName, diveNumber);
        NavigationDatumDAOImpl dao1 = new NavigationDatumDAOImpl();
        return dao1.fetchBestNavigationData(dive);
    }

    public Dive getGeneralDiveInfoEXPD(String rovName, int diveNumber){
        DiveDAO dao = new DiveDAOImpl();
        return dao.findByPlatformAndDiveNumber(rovName, diveNumber);
    }

    public List<CtdDatum> getDiveSamplesEXPD(String rovName,int diveNumber){
        DiveDAO dao = new DiveDAOImpl();
        Dive dive = dao.findByPlatformAndDiveNumber("Ventana", 4222);
        CtdDatumDAO dao1 = new CtdDatumDAOImpl();
        return dao1.fetchCtdData(dive);
    }
}
