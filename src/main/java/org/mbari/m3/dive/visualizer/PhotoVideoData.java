package org.mbari.m3.dive.visualizer;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.helidon.webserver.ServerRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.Set;


public class PhotoVideoData {
    private final Logger log = Logger.getLogger(getClass().getName());
    AnnotationData annotationDataHelper = new AnnotationData();
    Utilities utilities = new Utilities();
    Cache<String, AnnotationData> cache = Caffeine
        .newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .maximumSize(100)
        .build();


    /**
     * Returns a list of dives for the given ROV.
     * 
     * @param request
    */
    public String getRovDiveAnnotations(ServerRequest request){
        String rovName = request.path().param("rov");
        int diveNumber = Integer.parseInt(request.path().param("diveNumber"));

        SingletonCache cacheWrapper = SingletonCache.getInstance();
        String rovVideoLinksAnnotations = cacheWrapper.cache.get("VideoLinksAndAnnotations"+rovName+diveNumber, k -> {
            JsonObject allAnnotationData = annotationDataHelper.getAnnotationDataFromCache(request);
            return getVidsAndAnnotations(allAnnotationData).toString();
        });

        return rovVideoLinksAnnotations;
    }

    public String getRovPhotoAnnotations(ServerRequest request){
        String rovName = request.path().param("rov");
        int diveNumber = Integer.parseInt(request.path().param("diveNumber"));

        SingletonCache cacheWrapper = SingletonCache.getInstance();
        
        String rovPhotoLinksAnnotations = cacheWrapper.cache.get("PhotoLinksAndAnnotations"+rovName+diveNumber, k -> {
        
            JsonObject allAnnotationData = annotationDataHelper.getAnnotationDataFromCache(request);
            return getPhotoUrlsAndAnnotations(allAnnotationData).toString();
        });

        return rovPhotoLinksAnnotations;
    }


    /**
     * Returns a JsonArray of all Annotations from specific dive
     * 
     * @param allAnnotationData
     */
    private JsonArray getAnnotations(JsonObject allAnnotationData) {
        if (allAnnotationData == null) {
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
        if (allAnnotationData == null) {
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
        if (allAnnotationData == null) {
            log.log(Level.WARNING, "Annotation Data empty - DiveAnnotationService.getVideoLinks()");
            return null;
        }
        if(allAnnotationData.get("media") == null){
            return new JsonArray();
        }

        JsonArray allDiveVideos = new JsonArray();

        JsonArray media = allAnnotationData.getAsJsonArray("media");
        for (int i = 0; i < media.size(); i++) {
            if(media.get(i).getAsJsonObject().get("uri") == null) { continue; }
            String uri = media.get(i).getAsJsonObject().get("uri").getAsString();
            if (uri.charAt(uri.length() - 1) == '4') {
                allDiveVideos.add(uri);
            }
        }
        return allDiveVideos;
    }


    private JsonObject getVidsAndAnnotations(JsonObject allAnnotationData) {
        if (allAnnotationData == null) {
            log.log(Level.WARNING, "Annotation Data empty - DiveAnnotationService.getVideoLinksAndAnnotations()");
            return null;
        }
        JsonArray allMedia = getMedia(allAnnotationData);
        JsonArray videoLinks = getVideoLinks(allAnnotationData);
        JsonObject linksAndUUID = new JsonObject(); // linksAndUUID eventually becomes links and their annotations

        for(int j = 0; j < videoLinks.size();j++) {
            for(int i = 0; i < allMedia.size(); i++) {   
                if(allMedia.get(i).getAsJsonObject().get("uri") == null) { continue; }
                if(allMedia.get(i).getAsJsonObject().get("video_uuid") == null) { continue; }

                String video_reference_uuid = "";
                String cur_uri = allMedia.get(i).getAsJsonObject().get("uri").getAsString();
                if(videoLinks.get(j).getAsString().equals(cur_uri)){ 

                    video_reference_uuid = getVideoReferenceUUID(
                        allMedia.get(i)
                            .getAsJsonObject()
                            .get("video_uuid")
                            .getAsString() 
                        ,allAnnotationData);

                    if(video_reference_uuid.length()!=0){   
                        if(allMedia.get(i).getAsJsonObject().get("start_timestamp") != null){
                            if(allMedia.get(i).getAsJsonObject().get("duration_millis") != null){

                                String uri = videoLinks.get(j).getAsString();                                
                                JsonObject tempObj = new JsonObject();

                                tempObj.addProperty("video_reference_uuid",video_reference_uuid);
                                tempObj.addProperty("timestamp", allMedia.get(i).getAsJsonObject().get("start_timestamp").getAsString());
                                tempObj.addProperty("duration_millis", allMedia.get(i).getAsJsonObject().get("duration_millis").getAsString());

                                linksAndUUID.add(uri, tempObj);
                            }
                        }                 
                    } 
                }
            }
        }

        // Gets and Orders annotations
        for (Entry<String, JsonElement> entry : linksAndUUID.entrySet()) {
            String vid_ref_id = entry.getValue().getAsJsonObject().get("video_reference_uuid").getAsString();
            String timestamp = entry.getValue().getAsJsonObject().get("timestamp").getAsString();
            int duration = entry.getValue().getAsJsonObject().get("duration_millis").getAsInt();
            
            JsonArray videoAnnotations = getAnnotationsByVidRefUUIDAndTimestampDuration(
                vid_ref_id,
                timestamp, 
                duration, 
                allAnnotationData);
            if(videoAnnotations.size()>0){
                JsonArray newSortedArray = sortAnnotationArray(videoAnnotations);
                entry.getValue().getAsJsonObject().add("annotations", newSortedArray);
            }
            entry.getValue().getAsJsonObject().remove("video_reference_uuid");
            entry.getValue().getAsJsonObject().remove("duration_millis");
        }

        // Add mapping to linksAndUUID
        // Made an object in case we want to add another variable
        linksAndUUID.add("videoOrdering",getOrderedListOfPhotoOrVideoLinks(linksAndUUID));

        return linksAndUUID;
    }

    /**
    * This function takes the video_reference_uuid. It will return a list (Gson list) of annotations
    * @param video_reference_uuid
    */
    private JsonArray getAnnotationsByVidRefUUIDAndTimestampDuration(
        String video_reference_uuid, String timestamp, int duration, JsonObject allAnnotationData){

        if(allAnnotationData == null) {
            log.log(Level.WARNING, "Annotation Data empty - DiveAnnotationService.getAnnotationsByVidRefUUIDAndTimestampDuration()");
            return null;
        }

        JsonArray allAnnotations = getAnnotations(allAnnotationData);
        JsonArray videoAnnotations = new JsonArray();

        long startTime = getTimeInMillis(timestamp);
        long endTime = startTime + duration;
        
        Set<String> annotationUUIDSet = new HashSet<String>();

        // Compare all annotations that have a time between the start and end time, 
        // and ones with matching vid reference ids
        for (int i = 0; i < allAnnotations.size(); i ++) {
            if(allAnnotations.get(i).getAsJsonObject().get("observation_uuid") == null) { continue; }
            if(allAnnotations.get(i).getAsJsonObject().get("video_reference_uuid") == null) { continue; }
            if(allAnnotations.get(i).getAsJsonObject().get("recorded_timestamp") == null) { continue; }

            String curObsUUID = allAnnotations.get(i)
                .getAsJsonObject()
                .get("observation_uuid")
                .getAsString();
            String curVidRefUUID = allAnnotations.get(i)
                .getAsJsonObject()
                .get("video_reference_uuid")
                .getAsString();
            long curTimestamp = getTimeInMillis(allAnnotations.get(i)
                .getAsJsonObject()
                .get("recorded_timestamp")
                .getAsString());

            if(!annotationUUIDSet.contains(curObsUUID)){
                if(curTimestamp <= endTime && curTimestamp >= startTime 
                    || video_reference_uuid.equals(curVidRefUUID)){

                    videoAnnotations.add(allAnnotations.get(i).getAsJsonObject());
                    annotationUUIDSet.add(curObsUUID);
                }
            }
        }

        return videoAnnotations;
    }

    private long getTimeInMillis(String timestamp) {
        long fixedTime = -1;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
        try {
            Date date = sdf.parse(timestamp);
            Calendar calendar = Calendar.getInstance();    
            calendar.setTime(date);
            fixedTime = calendar.getTimeInMillis();
        } catch(ParseException e) {
            log.log(Level.WARNING, "Parsing timestamp error: " + timestamp + " - DiveAnnotationService.getAnnotationsByVidRefUUIDAndTimestampDuration()");
            e.printStackTrace();
        }
        return fixedTime;
    }

    // I am looking for the video_uuid that has the .mov video file, not .mp4
    private String getVideoReferenceUUID(String video_uuid,JsonObject allAnnotationData){
        if(allAnnotationData == null) {
            log.log(Level.WARNING, "Annotation Data empty - DiveAnnotationService.getVideoReferenceUUID()");
            return null;
        }

        JsonArray allMedia = getMedia(allAnnotationData);
        String video_reference_uuid = "";

        for(int i = 0; i < allMedia.size(); i++) {
            if(allMedia.get(i).getAsJsonObject().get("video_uuid") == null) { continue; }
            if(allMedia.get(i).getAsJsonObject().get("uri") == null) { continue; }
            if(allMedia.get(i).getAsJsonObject().get("video_reference_uuid") == null) { continue; }

            String curr_uuid = allMedia.get(i)
                .getAsJsonObject()
                .get("video_uuid")
                .getAsString();
            if(video_uuid.equals(curr_uuid)){
                
                String curr_uri = allMedia.get(i)
                    .getAsJsonObject()
                    .get("uri")
                    .getAsString();
                if(curr_uri.charAt(curr_uri.length()-1)=='v') {
                    video_reference_uuid = allMedia.get(i)
                        .getAsJsonObject()
                        .get("video_reference_uuid")
                        .getAsString();
                    break;
                }
            }
        }
        return video_reference_uuid;
    } 
    
    private JsonArray getROVPathFromAnnotations(JsonObject allAnnotationData) {
        if(allAnnotationData == null) {
            log.log(Level.WARNING, "Annotation Data empty - DiveAnnotationService.getROVPathFromAnnotations()");
            return null;
        }
        JsonArray annotations = getAnnotations(allAnnotationData);
        
        JsonArray allRovInfo = new JsonArray();

        for (int i = 0; i < annotations.size(); i++){
            JsonObject ancillaryData = annotations.get(i).getAsJsonObject().get("ancillary_data").getAsJsonObject();
            if(ancillaryData!= null){
                if(ancillaryData.get("latitude") != null 
                    && ancillaryData.get("longitude") != null 
                    && ancillaryData.get("depth_meters") != null){

                    JsonObject rovInfo = new JsonObject();
                    rovInfo.addProperty("latitude", 
                        annotations.get(i)
                            .getAsJsonObject()
                            .get("ancillary_data")
                            .getAsJsonObject()
                            .get("latitude")
                            .getAsString());
                    rovInfo.addProperty("longitude", 
                        annotations.get(i)
                            .getAsJsonObject()
                            .get("ancillary_data")
                            .getAsJsonObject()
                            .get("longitude")
                            .getAsString());
                    rovInfo.addProperty("depth_meters", 
                        annotations.get(i)
                            .getAsJsonObject()
                            .get("ancillary_data")
                            .getAsJsonObject()
                            .get("depth_meters")
                            .getAsString());
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


    //////////////////// PHOTO ////////////////////////////////////////



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
        mappingObj.add("photoMapping", getOrderedListOfPhotoOrVideoLinks(jpgPhotoUrlsMap));
        jpgPhotoUrlsMap.add("mappingObject", mappingObj);

        return jpgPhotoUrlsMap;
    }

    private boolean isJpg(String format){
        return(format.equals("image/jpg"));
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

        return listToJsonArray(mapping);
    }


    private JsonArray getOrderedListOfPhotoOrVideoLinks(JsonObject urlsMap){
        if(urlsMap.size()==0){
            return new JsonArray();
        }
        // ordering the links by their timestamps
        List<JsonObject> mapping = new ArrayList<JsonObject>();
        
        // add timestamps and links to mapping list
        for (Entry<String, JsonElement> entry : urlsMap.entrySet()) {
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
        
        return listToJsonArray(mapping);
    }

    private JsonArray listToJsonArray(List<JsonObject> list){
       // making finalMapping to turn mapping into a jsonArray
       JsonArray jsonArray = new JsonArray();
       for(int i = 0; i < list.size();i++) {
           jsonArray.add(list.get(i).get("link"));
       }
       return jsonArray;
    }

}