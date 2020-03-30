package org.mbari.m3.dive.visualizer;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
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
import java.util.concurrent.TimeUnit;
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
    AnnotationData annotationData = new AnnotationData();

    Cache<String, AnnotationData> cache = Caffeine
        .newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .maximumSize(100)
        .build();
    
    Utilities utilities = new Utilities();


    @Override
    public void update(Routing.Rules rules) {

        rules.get("/timestamps/{rov}/{diveNumber}", (req, res) -> {
            try {
                utilities.headersRespondSend(getAnnotationsWithMissingTimestamps(req), res);
                //missingTimestampsHttpResponse(req);
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });

        rules.get("/ancillary/{rov}/{diveNumber}", (req, res) -> {
            try {
                utilities.headersRespondSend(getAnnotationsWithMissingAncillaryData(req), res);
                //missingAncillaryHttpResponse(req, res);
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });

        rules.get("/navcoverage/{rov}/{diveNumber}", (req, res) -> {
            try {
                utilities.headersRespondSend(getNavCoverageRatioOfDive(req), res);
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
        });

        rules.get("/ctdcoverage/{rov}/{diveNumber}", (req, res) -> {
            try {
                utilities.headersRespondSend(getCTDCoverageRatioOfDive(req), res); 
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
        });

        rules.get("/camcoveragehd/{rov}/{diveNumber}", (req, res) -> {
            try {
                utilities.headersRespondSend(getCameraLogCoverageRatioOfDive(req,true), res); 
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
        });
        rules.get("/camcoveragesd/{rov}/{diveNumber}", (req, res) -> {
            try {
                utilities.headersRespondSend(getCameraLogCoverageRatioOfDive(req,false), res); 
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
        });
    }


    /**
     * Returns a JsonObject of all Annotations from specific dive
     * @param allAnnotationData
     * Notes: it calls the cache. It passes a key ("annotations"), and a function k.
     *         The function provides data to the cache if key does not exist.
     */
    private JsonObject getAnnotationData(ServerRequest request){
        String rovName = request.path().param("rov");
        int diveNumber = Integer.parseInt(request.path().param("diveNumber"));

        annotationData = cache.get("annotations", k -> {
            try {
                return AnnotationData.get(annotationData.set(rovName, diveNumber));
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                log.log(Level.WARNING, "Unable to set and get annotation data - DataErrorService.getAnnotationData()");
            }
            
            return new AnnotationData();
        });

        return annotationData.getData();
    }

    /**
     * Returns a JsonArray of all Annotations from specific dive
     * @param allAnnotationData
     */
    private JsonArray getAnnotations(JsonObject allAnnotationData) {
        if (allAnnotationData == null) {
            log.log(Level.WARNING, "Annotation Data empty - DataErrorService.getAnnotations()");
            return null;
        }
        return allAnnotationData.getAsJsonArray("annotations");
    }

    private String getAnnotationsWithMissingTimestamps(ServerRequest request) {
        JsonObject allAnnotationData = getAnnotationData(request);

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
        return annosWithMissingTimestamps.toString();
    }


    private String getAnnotationsWithMissingAncillaryData(ServerRequest request) {
        JsonObject allAnnotationData = getAnnotationData(request);

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
        return annotationsWithMissingData.toString();
    }

    private String getCameraLogCoverageRatioOfDive(ServerRequest request, boolean isHd){
        String rovName = request.path().param("rov");
        int diveNumber = Integer.parseInt(request.path().param("diveNumber"));

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

    private String getCTDCoverageRatioOfDive(ServerRequest request){
        String rovName = request.path().param("rov");
        int diveNumber = Integer.parseInt(request.path().param("diveNumber"));

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
    

    private String getNavCoverageRatioOfDive(ServerRequest request){
        String rovName = request.path().param("rov");
        int diveNumber = Integer.parseInt(request.path().param("diveNumber"));


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
