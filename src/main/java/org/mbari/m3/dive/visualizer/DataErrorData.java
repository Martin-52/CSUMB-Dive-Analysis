package org.mbari.m3.dive.visualizer;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.helidon.webserver.ServerRequest;
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



public class DataErrorData{
    private final Logger log = Logger.getLogger(getClass().getName());
    AnnotationData annotationDataHelper = new AnnotationData();
    SingletonCache cacheWrapper = SingletonCache.getInstance();
    Cache<String, AnnotationData> cache = Caffeine
        .newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .maximumSize(100)
        .build();

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

    public String getAnnotationsWithMissingTimestamps(ServerRequest request) {
        String rovName = request.path().param("rov");
        int diveNumber = Integer.parseInt(request.path().param("diveNumber"));
        
        String annotationsWithMissingTimestamps = cacheWrapper.cache.get("AnnotationsWithMissingTimestamps"+rovName+diveNumber, k -> {
            JsonObject allAnnotationData = annotationDataHelper.getAnnotationDataFromCache(request);

            if (allAnnotationData == null) {
                log.log(Level.WARNING, "Annotation Data empty - DataErrorService.getMissingTimestamps()");
                return "{}";
            }

            JsonArray allAnnotations = getAnnotations(allAnnotationData);
            JsonArray annosWithMissingTimestamps = new JsonArray();

            for (int i = 0; i < allAnnotations.size(); i++) {
                if (allAnnotations.get(i).getAsJsonObject().get("recorded_timestamp") == null) {
                    annosWithMissingTimestamps.add(allAnnotations.get(i).getAsJsonObject());
                }
            }

            return annosWithMissingTimestamps.toString();  
        });

        return annotationsWithMissingTimestamps;
    }


    public String getAnnotationsWithMissingAncillaryData(ServerRequest request) {
        String rovName = request.path().param("rov");
        int diveNumber = Integer.parseInt(request.path().param("diveNumber"));

        String annotationsWithMissingAncillaryData = cacheWrapper.cache.get("AnnotationsWithMissingAncillaryData"+rovName+diveNumber, k -> {
            JsonObject allAnnotationData = annotationDataHelper.getAnnotationDataFromCache(request);

            if (allAnnotationData == null) {
                log.log(Level.WARNING, "Annotation Data empty - DataErrorService.getMissingTimestamps()");
                return "{}";
            }
            JsonArray allAnnotations = getAnnotations(allAnnotationData);
            JsonArray annotationsWithMissingData = new JsonArray();
    
            for (int i = 0; i < allAnnotations.size(); i++) {
                if (allAnnotations.get(i).getAsJsonObject().get("ancillary_data") == null) {
                    annotationsWithMissingData.add(allAnnotations.get(i).getAsJsonObject());
                }
            }
            return annotationsWithMissingData.toString();
        });

        return annotationsWithMissingAncillaryData;
    }

    public String getCameraLogCoverageRatioOfDive(ServerRequest request, boolean isHd){
        String rovName = request.path().param("rov");
        int diveNumber = Integer.parseInt(request.path().param("diveNumber"));

        String camLogCoverage = cacheWrapper.cache.get("CameraLogCoverageRatioOfDiveisHD"+isHd+rovName+diveNumber, k -> {
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
        });

        return camLogCoverage;
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

    public String getCTDCoverageRatioOfDive(ServerRequest request){
        String rovName = request.path().param("rov");
        int diveNumber = Integer.parseInt(request.path().param("diveNumber"));

        String ctdCoverageRatioOfDive = cacheWrapper.cache.get("CTDCoverageRatioOfDive"+rovName+diveNumber, k -> {
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

        });

        return ctdCoverageRatioOfDive;
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
    

    public String getNavCoverageRatioOfDive(ServerRequest request){
        String rovName = request.path().param("rov");
        int diveNumber = Integer.parseInt(request.path().param("diveNumber"));

        String navCoverageRatioOfDive = cacheWrapper.cache.get("NavCoverageRatioOfDive"+rovName+diveNumber, k ->{
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
        });
        return navCoverageRatioOfDive;
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