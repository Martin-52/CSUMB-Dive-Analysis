package org.mbari.m3.dive.visualizer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import mbarix4j.math.Matlib;
import mbarix4j.math.Statlib;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.Set;

public class DataErrorService implements Service {
    private final Logger log = Logger.getLogger(getClass().getName());

    @Override
    public void update(Routing.Rules rules) {

        // rules.get("/getRovNames", this::getRovNames);

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
                getNavCoverageRatioOfDive(req, res);
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });


        rules.get("/ctdcoverage/{rov}/{diveNumber}", (req, res) -> {
            try {
                getCTDCoverageRatioOfDive(req, res);
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });

        rules.get("/camcoverage/{rov}/{diveNumber}", (req, res) -> {
            try {
                getCameraLogCoverageRatioOfDive(req, res);
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });
        
    }

    /**
     * Sends http request to retrieve the json for the given rov and dive number
     * 
     * @param rovName
     * @param diveNumber
     */
    private JsonObject getDiveDataThroughHttpRequest(String rovName, int diveNumber)
            throws IOException, InterruptedException {

        final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();

        String path = "http://dsg.mbari.org/references/query/dive/" + rovName + "%20" + diveNumber;

        HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create(path))
                .setHeader("User-Agent", "Java 11 HttpClient Bot").build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return (new JsonParser().parse(response.body()).getAsJsonObject());
    }

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

   ////////////////////////////////////////////////////////////
    private void getCameraLogCoverageRatioOfDive(ServerRequest request, ServerResponse response) throws IOException, InterruptedException {
        String rovName = request.path().param("rov");
        int diveNumber = Integer.parseInt(request.path().param("diveNumber"));

        DiveDAO dao = new DiveDAOImpl();
        // returns null if no match is found
        Dive dive = dao.findByPlatformAndDiveNumber(rovName, diveNumber);
        if(dive == null) {
            log.log(Level.WARNING, "getCTDCoverageRatioOfDive(): null dive - DiveErrorService");
            return;
        }

        CameraDatumDAO daoCam = new CameraDatumDAOImpl();
        List<CameraDatum> camSamples = daoCam.fetchCameraData(dive, true);


        double coverage = camSamples.size()*sampleIntervalCam(camSamples);
        double diveLengthSecs = (dive.getEndDate().getTime() - dive.getStartDate().getTime())/1000; 
        double coverageRatio = coverage/diveLengthSecs;

        // System.out.println("percent of dive covered: " + coverageRatio + "%");
        // System.out.println("coverage: " + coverage + " = sample size: " + camSamples.size() + " * sample interval: " + sampleIntervalCam(camSamples));
        // System.out.println("dive end date: " + dive.getEndDate());
        // System.out.println("dive start date: " + dive.getStartDate());
        // System.out.println("dive length in seconds: " + diveLengthSecs);
        // System.out.println("sample interval: " + sampleIntervalCam(camSamples));

        response.headers().add("Access-Control-Allow-Origin", "*");
        response.headers().add("Access-Control-Allow-Headers", "*");
        response.headers().add("Access-Control-Allow-Credentials", "true");
        response.headers().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        response.send(Double.toString(coverageRatio));
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


    ////////////////////////////////////////////////////////////
    private void getCTDCoverageRatioOfDive(ServerRequest request, ServerResponse response) throws IOException, InterruptedException {
        String rovName = request.path().param("rov");
        int diveNumber = Integer.parseInt(request.path().param("diveNumber"));

        DiveDAO dao = new DiveDAOImpl();
        // returns null if no match is found
        Dive dive = dao.findByPlatformAndDiveNumber(rovName, diveNumber);
        if(dive == null) {
            log.log(Level.WARNING, "getCTDCoverageRatioOfDive(): null dive - DiveErrorService");
            return;
        }

        CtdDatumDAO daoCTD = new CtdDatumDAOImpl();
        List<CtdDatum> ctdSamples = daoCTD.fetchCtdData(dive);
        
        double coverage = ctdSamples.size()*sampleIntervalCTD(ctdSamples);
        double diveLengthSecs = (dive.getEndDate().getTime() - dive.getStartDate().getTime())/1000; 
        double coverageRatio = coverage/diveLengthSecs;

        // System.out.println("percent of dive covered: " + coverageRatio + "%");
        // System.out.println("coverage: " + coverage + " sample size: " + ctdSamples.size() + " sample interval: " + sampleInterval);
        // System.out.println("dive end date: " + dive.getEndDate());
        // System.out.println("dive start date: " + dive.getStartDate());
        // System.out.println("dive length in seconds: " + diveLengthSecs);
        // System.out.println("sample interval: " + sampleInterval);

        response.headers().add("Access-Control-Allow-Origin", "*");
        response.headers().add("Access-Control-Allow-Headers", "*");
        response.headers().add("Access-Control-Allow-Credentials", "true");
        response.headers().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        response.send(Double.toString(coverageRatio));
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
    

//////////////////////////////////////////////////////////////////

    private void getNavCoverageRatioOfDive(ServerRequest request, ServerResponse response) throws IOException, InterruptedException {

        String rovName = request.path().param("rov");
        int diveNumber = Integer.parseInt(request.path().param("diveNumber"));
        
        DiveDAO dao = new DiveDAOImpl();
        Dive dive = dao.findByPlatformAndDiveNumber(rovName, diveNumber);

        if(dive==null){
            log.log(Level.WARNING, "getNavCoverageRatioOfDive(): null dive - DiveErrorService");
            return;
        }

        NavigationDatumDAOImpl dao1 = new NavigationDatumDAOImpl();
        List<NavigationDatum> nav = dao1.fetchBestNavigationData(dive);
        
        double coverage = (int) (nav.size()*sampleIntervalNav(nav));
        double diveLengthSecs = (dive.getEndDate().getTime() - dive.getStartDate().getTime())/1000; 
        double coverageRatio = coverage/diveLengthSecs;

        // System.out.println("nav record size * sample interval ( total coverage ): " + nav.size()*sampleIntervalNav(nav));
        // System.out.println("percent of dive covered: " + coverageRatio + "%");
        // System.out.println("dive length in seconds: " + diveLengthSecs);
        // System.out.println("sample interval: " + sampleIntervalNav(nav));

        response.headers().add("Access-Control-Allow-Origin", "*");
        response.headers().add("Access-Control-Allow-Headers", "*");
        response.headers().add("Access-Control-Allow-Credentials", "true");
        response.headers().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        response.send(Double.toString(coverageRatio));
    }

    /**
   * 
   * @param <T> The type of the list
   * @param samples A list of objects
   * @param fn A function that converts an object of type T to a unit of time as 
   *           seconds (fractional seconds are OK)
   * @return The estimated time that a sample represents (sample interval)
   */
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
