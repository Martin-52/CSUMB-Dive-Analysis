package org.mbari.m3.dive.visualizer;

import com.google.gson.JsonArray;
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
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mbari.expd.CameraDatum;
import org.mbari.expd.CameraDatumDAO;
import org.mbari.expd.CtdDatum;
import org.mbari.expd.CtdDatumDAO;
import org.mbari.expd.Dive;
import org.mbari.expd.DiveDAO;
import org.mbari.expd.NavigationDatum;
import org.mbari.expd.jdbc.CameraDatumDAOImpl;
import org.mbari.expd.jdbc.CtdDatumDAOImpl;
import org.mbari.expd.jdbc.DiveDAOImpl;
import org.mbari.expd.jdbc.NavigationDatumDAOImpl;
import mbarix4j.math.Matlib;
import mbarix4j.math.Statlib;


public class DataErrorService implements Service {
    private final Logger log = Logger.getLogger(getClass().getName());

    @Override
    public void update(Routing.Rules rules) {

        rules.get("/timestamps/{rov}/{diveNumber}", (req, res) -> {
            try {
                missingTimestampsHttpResponse(req, res);
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });

        rules.get("/ancillary/{rov}/{diveNumber}", (req, res) -> {
            try {
                missingAncillaryHttpResponse(req, res);
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });

        rules.get("/navcoverage/{rov}/{diveNumber}", (req, res) -> {
            try {
                navLogCoverageHttpResponse(req, res);
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
        });

        rules.get("/ctdcoverage/{rov}/{diveNumber}", (req, res) -> {
            try {
                ctdLogCoverageHttpResponse(req, res);
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
        });

        rules.get("/camcoveragehd/{rov}/{diveNumber}", (req, res) -> {
            try {
                cameraLogCoverageHttpResponse(req, res, true);
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
        });
        rules.get("/camcoveragesd/{rov}/{diveNumber}", (req, res) -> {
            try {
                cameraLogCoverageHttpResponse(req, res, false);
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
        });
    }

    /**
     * Sends http request to retrieve the json for the given rov and dive number
     * returns MBARI information on dive. This contains a json tree with annotations and media
     * @param rovName
     * @param diveNumber
     */
    private JsonObject getDiveDataThroughHttpRequest(String rovName, int diveNumber) throws IOException, InterruptedException {
        final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
        if(rovName.contains(" ")){// These are for rov names with a space (i.e Mini Rov & Doc Rickett)
            rovName = rovName.replace(" ","%20");
        }
        String path = "http://dsg.mbari.org/references/query/dive/" + rovName + "%20" + diveNumber;

        HttpRequest request = HttpRequest
            .newBuilder()
            .GET()
            .uri(URI.create(path))
            .setHeader("User-Agent", "Java 11 HttpClient Bot")
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return (new JsonParser().parse(response.body()).getAsJsonObject());
    }

    /**
     * Sends http response with Json Array of annotations with missing timestamps
     * @param request
     * @param response
     */
    private void missingTimestampsHttpResponse(ServerRequest request, ServerResponse response)
            throws IOException, InterruptedException {
        String rovName = request.path().param("rov");
        int diveNumber = Integer.parseInt(request.path().param("diveNumber"));

        JsonObject allAnnotationData = getDiveDataThroughHttpRequest(rovName, diveNumber);

        JsonArray missingTimestamps = getAnnotationsWithMissingTimestamps(allAnnotationData);

        response.headers().add("Access-Control-Allow-Origin", "*");
        response.headers().add("Access-Control-Allow-Headers", "*");
        response.headers().add("Access-Control-Allow-Credentials", "true");
        response.headers().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        response.send(missingTimestamps.toString());
    }

    /**
     * Sends http response with Json Array of annotations with missing ancillary data
     * @param request
     * @param response
     */
    private void missingAncillaryHttpResponse(ServerRequest request, ServerResponse response)
            throws IOException, InterruptedException {
        String rovName = request.path().param("rov");
        int diveNumber = Integer.parseInt(request.path().param("diveNumber"));

        JsonObject allAnnotationData = getDiveDataThroughHttpRequest(rovName, diveNumber);

        JsonArray missingAncillary = getAnnotationsWithMissingAncillaryData(allAnnotationData);

        response.headers().add("Access-Control-Allow-Origin", "*");
        response.headers().add("Access-Control-Allow-Headers", "*");
        response.headers().add("Access-Control-Allow-Credentials", "true");
        response.headers().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        response.send(missingAncillary.toString());
    }

    /**
     * Sends http response of a double (amount of footage time / time of dive) of the ratio of camera log footage covered in dive.
     * @param request
     * @param response
     * @param isHd  quality of footage
     */
    private void cameraLogCoverageHttpResponse(ServerRequest request, ServerResponse response, boolean isHd) throws IOException, InterruptedException {
        String rovName = request.path().param("rov");
        int diveNumber = Integer.parseInt(request.path().param("diveNumber"));

        String coverageRatio = getCameraLogCoverageRatioOfDive(rovName, diveNumber, isHd);

        response.headers().add("Access-Control-Allow-Origin", "*");
        response.headers().add("Access-Control-Allow-Headers", "*");
        response.headers().add("Access-Control-Allow-Credentials", "true");
        response.headers().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        response.send(coverageRatio);
    }

    /**
     * Sends http response of a double (amount of ctd time / time of dive) of the ratio of ctd logs covered in dive.
     * @param request
     * @param response
     */
    private void ctdLogCoverageHttpResponse(ServerRequest request, ServerResponse response) throws IOException, InterruptedException {
        String rovName = request.path().param("rov");
        int diveNumber = Integer.parseInt(request.path().param("diveNumber"));

        String coverageRatio = getCTDCoverageRatioOfDive(rovName, diveNumber);

        response.headers().add("Access-Control-Allow-Origin", "*");
        response.headers().add("Access-Control-Allow-Headers", "*");
        response.headers().add("Access-Control-Allow-Credentials", "true");
        response.headers().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        response.send(coverageRatio);
    }

    /**
     * Sends http response of a double (amount of nav time / time of dive) of the ratio of nav logs covered in dive.
     * @param request
     * @param response
     */
    private void navLogCoverageHttpResponse(ServerRequest request, ServerResponse response) throws IOException, InterruptedException {
        String rovName = request.path().param("rov");
        int diveNumber = Integer.parseInt(request.path().param("diveNumber"));

        String coverageRatio = getNavCoverageRatioOfDive(rovName, diveNumber);

        response.headers().add("Access-Control-Allow-Origin", "*");
        response.headers().add("Access-Control-Allow-Headers", "*");
        response.headers().add("Access-Control-Allow-Credentials", "true");
        response.headers().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        response.send(coverageRatio);
    }

    /**
     * Returns a JsonArray of all Annotations from specific dive
     * @param allAnnotationData
     */
    private JsonArray getAnnotations(JsonObject allAnnotationData) {
        if (allAnnotationData == null) {
            log.log(Level.WARNING, "Annotation Data empty - DiveAnnotationService.getAnnotations()");
            return null;
        }
        return allAnnotationData.getAsJsonArray("annotations");
    }


    public JsonArray getAnnotationsWithMissingTimestamps(JsonObject allAnnotationData) {
        if (allAnnotationData == null) {
            log.log(Level.WARNING, "Annotation Data empty - DataErrorService.getMissingTimestamps()");
            return null;
        }
        JsonArray allAnnotations = getAnnotations(allAnnotationData);
        JsonArray annosWithMissingTimestamps = new JsonArray();

        for (int i = 0; i < allAnnotations.size(); i++) {
            if (allAnnotations.get(i).getAsJsonObject().get("recorded_timestamp") == null) {
                annosWithMissingTimestamps.add(allAnnotations.get(i).getAsJsonObject());
            }
        }
        return annosWithMissingTimestamps;
    }

    private JsonArray getAnnotationsWithMissingAncillaryData(JsonObject allAnnotationData) {
        if (allAnnotationData == null) {
            log.log(Level.WARNING, "Annotation Data empty - DataErrorService.getMissingTimestamps()");
            return null;
        }
        JsonArray allAnnotations = getAnnotations(allAnnotationData);
        JsonArray annotationsWithMissingData = new JsonArray();

        for (int i = 0; i < allAnnotations.size(); i++) {
            if (allAnnotations.get(i).getAsJsonObject().get("ancillary_data") == null) {
                annotationsWithMissingData.add(allAnnotations.get(i).getAsJsonObject());
            }
        }
        return annotationsWithMissingData;
    }

    private String getCameraLogCoverageRatioOfDive(String rovName, int diveNumber, boolean isHd){
        DiveDAO dao = new DiveDAOImpl();
        // returns null if no match is found
        Dive dive = dao.findByPlatformAndDiveNumber(rovName, diveNumber);
        double coverageRatio = 0.0;

        if(dive != null) {
            CameraDatumDAO daoCam = new CameraDatumDAOImpl();
            List<CameraDatum> camSamples = null;
            try {
                camSamples = daoCam.fetchCameraData(dive,isHd);
            } catch (Exception e){
                log.log(Level.WARNING, "DiveErrorService.getCameraLogCoverageRatioOfDive(): Unable to retrieve camera data");
            }
            if(camSamples != null){
                double coverage = camSamples.size()*sampleIntervalCam(camSamples);
                double diveLengthSecs = (dive.getEndDate().getTime() - dive.getStartDate().getTime())/1000; 
                coverageRatio = coverage/diveLengthSecs;
            }    
        } else {
            log.log(Level.WARNING, "DiveErrorService.getCameraLogCoverageRatioOfDive(): null dive");
        }
        coverageRatio = coverageRatio * 100.0;
        DecimalFormat df = new DecimalFormat("#.00");
        String text = Double.toString(Math.abs(coverageRatio));
        int integerPlaces = text.indexOf('.');
        int decimalPlaces = text.length() - integerPlaces - 1;
        if(decimalPlaces>=2){
            return df.format(coverageRatio);
        }
        return Double.toString(coverageRatio);
    }

    private double sampleIntervalCam(List<CameraDatum> samples) {
        if(samples == null || samples.size() == 0) return 0.0;

        double[] seconds = samples.stream()
            .mapToDouble(t -> getDateToSecondsCam(t))
            .sorted()
            .toArray();
        
        double[] intervals = Matlib.diff(seconds);

        double lowerQ = Statlib.percentile(intervals, 0.25);
        double upperQ = Statlib.percentile(intervals, 0.75);
        double[] goodIntervals = Arrays.stream(intervals)
            .filter(i -> i >= lowerQ && i <= upperQ)
            .toArray();
        
        return Statlib.mean(goodIntervals);
    }

    private double getDateToSecondsCam(CameraDatum obj) {
        if(obj == null) return 0.0;
        String timestamp = obj.getDate().toString();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        double seconds = -1;
        try {
            Date date = sdf.parse(timestamp);
            Calendar calendar = Calendar.getInstance();    
            calendar.setTime(date);
            seconds = calendar.getTimeInMillis()/1000.0;
            
        } catch(ParseException e) {
            log.log(Level.WARNING, "Parsing timestamp error: " + timestamp + " - DiveAnnotationService.getAnnotationsByVidRefUUIDAndTimestampDuration()");
            e.printStackTrace();
        }
        return seconds;
    }

    private String getCTDCoverageRatioOfDive(String rovName, int diveNumber){
        DiveDAO dao = new DiveDAOImpl();
        // returns null if no match is found
        Dive dive = dao.findByPlatformAndDiveNumber(rovName, diveNumber);
        double coverageRatio = 0.0;
        
        if (dive != null) {
            CtdDatumDAO daoCTD = new CtdDatumDAOImpl();
            List<CtdDatum> ctdSamples = null;
            try {
                ctdSamples = daoCTD.fetchCtdData(dive);
            } catch(Exception e){
                log.log(Level.WARNING, "DiveErrorService.getCTDCoverageRatioOfDive() - Unable to retrieve ctd data");
            }  
            if(ctdSamples != null){
                double coverage = ctdSamples.size()*sampleIntervalCTD(ctdSamples);
                double diveLengthSecs = (dive.getEndDate().getTime() - dive.getStartDate().getTime())/1000; 
                coverageRatio = coverage/diveLengthSecs;
            }    
        } else {
            log.log(Level.WARNING, "DiveErrorService.getCTDCoverageRatioOfDive() - null dive");
        }

        coverageRatio = coverageRatio * 100.0;
        DecimalFormat df = new DecimalFormat("#.00");
        String text = Double.toString(Math.abs(coverageRatio));
        int integerPlaces = text.indexOf('.');
        int decimalPlaces = text.length() - integerPlaces - 1;
        if(decimalPlaces>=2){
            return df.format(coverageRatio);
        }
        return Double.toString(coverageRatio);
    }

    private double sampleIntervalCTD(List<CtdDatum> samples) {
        if(samples == null || samples.size() == 0) return 0.0;

        double[] seconds = samples.stream()
            .mapToDouble(t -> getDateToSecondsCTD(t))
            .sorted()
            .toArray();
        
        double[] intervals = Matlib.diff(seconds);

        double lowerQ = Statlib.percentile(intervals, 0.25);
        double upperQ = Statlib.percentile(intervals, 0.75);
        double[] goodIntervals = Arrays.stream(intervals)
            .filter(i -> i >= lowerQ && i <= upperQ)
            .toArray();
        
        return Statlib.mean(goodIntervals);
    }

    private double getDateToSecondsCTD(CtdDatum obj) {
        if(obj == null) return 0.0;
        String timestamp = obj.getDate().toString();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        double seconds = -1;
        try {
            Date date = sdf.parse(timestamp);
            Calendar calendar = Calendar.getInstance();    
            calendar.setTime(date);
            seconds = calendar.getTimeInMillis()/1000.0;
            
        } catch(ParseException e) {
            log.log(Level.WARNING, "Parsing timestamp error: " + timestamp + " - DiveAnnotationService.getAnnotationsByVidRefUUIDAndTimestampDuration()");
            e.printStackTrace();
        }
        return seconds;
    }
    

    private String getNavCoverageRatioOfDive(String rovName, int diveNumber){
        DiveDAO dao = new DiveDAOImpl();
        // findBy...() returns null if not found
        Dive dive = dao.findByPlatformAndDiveNumber(rovName, diveNumber);
        double coverageRatio = 0.0;

        if(dive!=null){
            NavigationDatumDAOImpl navDatumImpl = new NavigationDatumDAOImpl();
            List<NavigationDatum> navSamples = null;
            try {
                navSamples = navDatumImpl.fetchBestNavigationData(dive);
            } catch(Exception e) {
                log.log(Level.WARNING, "DiveErrorService.getNavCoverageRatioOfDive(): Unable to retrieve navigation data");
            }
            if(navSamples != null){
                double coverage = (int) (navSamples.size()*sampleIntervalNav(navSamples));
                double diveLengthSecs = (dive.getEndDate().getTime() - dive.getStartDate().getTime())/1000; 
                coverageRatio = coverage/diveLengthSecs;
            }    
        } else {
            log.log(Level.WARNING, "DiveErrorService.getNavCoverageRatioOfDive(): null dive");
        }

        coverageRatio = coverageRatio * 100.0;
        DecimalFormat df = new DecimalFormat("#.00");
        String text = Double.toString(Math.abs(coverageRatio));
        int integerPlaces = text.indexOf('.');
        int decimalPlaces = text.length() - integerPlaces - 1;
        if(decimalPlaces>=2){
            return df.format(coverageRatio);
        }
        return Double.toString(coverageRatio);
    }

    private double sampleIntervalNav(List<NavigationDatum> samples) {
        if(samples == null || samples.size() == 0) return 0.0;

        double[] seconds = samples.stream()
            .mapToDouble(t -> getDateToSecondsNav(t))
            .sorted()
            .toArray();
        
        double[] intervals = Matlib.diff(seconds);

        double lowerQ = Statlib.percentile(intervals, 0.25);
        double upperQ = Statlib.percentile(intervals, 0.75);
        double[] goodIntervals = Arrays.stream(intervals)
            .filter(i -> i >= lowerQ && i <= upperQ)
            .toArray();
        
        return Statlib.mean(goodIntervals);
    }

    private double getDateToSecondsNav(NavigationDatum obj) {
        if(obj == null) return 0.0;
        String timestamp = obj.getDate().toString();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        double seconds = -1;
        try {
            Date date = sdf.parse(timestamp);
            Calendar calendar = Calendar.getInstance();    
            calendar.setTime(date);
            seconds = calendar.getTimeInMillis()/1000.0;
        } catch(ParseException e) {
            log.log(Level.WARNING, "Parsing timestamp error: " + timestamp + " - DiveAnnotationService.getAnnotationsByVidRefUUIDAndTimestampDuration()");
            e.printStackTrace();
        }
        return seconds;
    }

}
