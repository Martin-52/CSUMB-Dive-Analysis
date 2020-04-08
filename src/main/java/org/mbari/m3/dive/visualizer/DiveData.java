package org.mbari.m3.dive.visualizer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.sql.Date;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.mbari.expd.CtdDatum;
import org.mbari.expd.CtdDatumDAO;
import org.mbari.expd.Dive;
import org.mbari.expd.DiveDAO;
import org.mbari.expd.jdbc.BaseDAOImpl;
import org.mbari.expd.jdbc.CtdDatumDAOImpl;
import org.mbari.expd.jdbc.DiveDAOImpl;
import org.mbari.expd.jdbc.NavigationDatumDAOImpl;
import org.mbari.expd.NavigationDatum;

public class DiveData {

    private SingletonCache cache;
    private final Logger log;

    public DiveData() {
        cache = SingletonCache.getInstance();
        log = Logger.getLogger(getClass().getName());
    }

    public String getGeneralDiveInformation(String rovName, int diveNumber){
        StringBuilder key = new StringBuilder();
        key.append("GeneralDiveInformation");
        key.append(rovName);
        key.append(Integer.toString(diveNumber));
        String data = cache.getData(key.toString());
        if(data != null){
            return data;
        } else {
            DiveDAO dao = new DiveDAOImpl();
            Dive dive = dao.findByPlatformAndDiveNumber(rovName, diveNumber);
            JsonObject diveInformation = new JsonObject();
            diveInformation.addProperty("chiefScientist", dive.getChiefScientist());
            diveInformation.addProperty("briefAccomplishments", dive.getBriefAccomplishments());
            diveInformation.addProperty("startDate", dive.getStartDate().toString());
            diveInformation.addProperty("endDate", dive.getEndDate().toString());
            diveInformation.addProperty("latitude", dive.getLatitude().toString());
            diveInformation.addProperty("longitude", dive.getLongitude().toString());
            diveInformation.addProperty("rovName", dive.getRovName());
            diveInformation.addProperty("diveNumber", Integer.toString(dive.getDiveNumber()));
            data = diveInformation.toString();
            cache.setData(key.toString(), data);
            return data;
        }
    }

    /**
     * Returns a list of ROV names.
     * @param request
     * @param response
    */
    public String getRovNames() {
        JsonArray arr = new JsonArray();
        for (String name: BaseDAOImpl.getAllRovNames()) {
            arr.add(name);
        }

        return arr.toString();
    }

    public String getLatsAndLongs(String rovName, int diveNum) {

        StringBuilder key = new StringBuilder();
        key.append("latsAndLongs");
        key.append(rovName);
        key.append(Integer.toString(diveNum));
        String data = cache.getData(key.toString());
        if (data != null) {
            return data;
        } else {
            Dive dive = fetchDiveObject(rovName, diveNum);

            if(dive==null){
                System.out.println("getLatsAndLongs(): null dive");
                return "No Dive Available";
            }
    
            NavigationDatumDAOImpl dao1 = new NavigationDatumDAOImpl();
            List<NavigationDatum> nav = dao1.fetchBestNavigationData(dive);
    
            JsonArray latsAndLongs = new JsonArray();
    
            for(int i = 0 ; i < nav.size();i++){
                JsonObject newLatLongObj = new JsonObject();
                newLatLongObj.addProperty("latitude", Double.toString(nav.get(i).getLongitude()));
                newLatLongObj.addProperty("longitude", Double.toString(nav.get(i).getLatitude()));
                latsAndLongs.add(newLatLongObj);
            }
    
            cache.setData(key.toString(), latsAndLongs.toString());
            return latsAndLongs.toString();
        }
    }

    public String checkDiveNumber(String rovName, int diveNum) {
        DiveDAO dao = new DiveDAOImpl();
        Collection<Dive> divesForRov = dao.findByPlatform(rovName);
        List<Integer> diveNumbersForRov = divesForRov
            .stream()
            .map(Dive::getDiveNumber)
            .sorted()
            .collect(Collectors.toList());
        
        boolean diveExists = false;
        for(int i = 0; i < diveNumbersForRov.size();i++){
            if(diveNum == diveNumbersForRov.get(i)){
                diveExists = true;
            }
        }

        JsonObject existObj = new JsonObject();
        existObj.addProperty("exists", diveExists);

        return existObj.toString();        
    }

    public String getMinAndDepth(String rovName, int diveNum) {
        StringBuilder key = new StringBuilder();
        key.append("minAndDepth");
        key.append(rovName);
        key.append(Integer.toString(diveNum));

        String data = cache.getData(key.toString());

        if (data != null) {
            return data;
        } else {
            Dive dive = fetchDiveObject(rovName, diveNum);

            if(dive==null){
                log.log(Level.WARNING, "getLatsAndLongs(): null dive - DiveService");
                return "No dive available";
            }
    
            NavigationDatumDAOImpl dao1 = new NavigationDatumDAOImpl();
            List<NavigationDatum> nav = dao1.fetchBestNavigationData(dive);
            JsonArray minAndDepth = new JsonArray();
            
            for(int i = 0 ; i < nav.size();i++){
                JsonObject newMinDepthObj = new JsonObject();
                Date dateObj = new Date(nav.get(i).getDate().getTime());
                Calendar calendar = Calendar.getInstance();    
                calendar.setTime(dateObj);
    
                newMinDepthObj.addProperty("minute", calendar.get(Calendar.MINUTE));
                newMinDepthObj.addProperty("depth", Double.toString(nav.get(i).getDepth()));
                minAndDepth.add(newMinDepthObj);
            }
    
            cache.setData(key.toString(), minAndDepth.toString());
            return minAndDepth.toString();
        }
    }

    public String getHourAndDepth(String rovName, int diveNum) {
        StringBuilder key = new StringBuilder();
        key.append("hourAndDepth");
        key.append(rovName);
        key.append(Integer.toString(diveNum));

        String data = cache.getData(key.toString());

        if (data != null) {
            return data;
        } else { 
            Dive dive = fetchDiveObject(rovName, diveNum);

            if(dive==null){
                log.log(Level.WARNING, "getHourAndDepth(): null dive - DiveService");
                return "No dive available";
            }
    
            NavigationDatumDAOImpl dao1 = new NavigationDatumDAOImpl();
            List<NavigationDatum> nav = dao1.fetchBestNavigationData(dive);
            JsonArray hourAndDepth = new JsonArray();
    
            for(int i = 0 ; i < nav.size();i++){
                JsonObject newHourDepthObj = new JsonObject();
                Date dateObj = new Date(nav.get(i).getDate().getTime());
                Calendar calendar = Calendar.getInstance();    
                calendar.setTime(dateObj);
    
                newHourDepthObj.addProperty("hour", calendar.get(Calendar.HOUR_OF_DAY));
                newHourDepthObj.addProperty("depth", Double.toString(nav.get(i).getDepth()));
                hourAndDepth.add(newHourDepthObj);
            }
    
            cache.setData(key.toString(), hourAndDepth.toString());
            return hourAndDepth.toString();
        }
    }

    public String getDiveDates(String rovName, int diveNum) {
        StringBuilder key = new StringBuilder();
        key.append("diveDates");
        key.append(rovName);
        key.append(Integer.toString(diveNum));

        String data = cache.getData(key.toString());

        if (data != null) {
            return data;
        } else { 
            Dive dive = fetchDiveObject(rovName, diveNum);

            if(dive==null){
                log.log(Level.WARNING, "getDiveDates(): null dive - DiveService");
                return "No dive available";
            }
    
            NavigationDatumDAOImpl dao1 = new NavigationDatumDAOImpl();
            List<NavigationDatum> nav = dao1.fetchBestNavigationData(dive);
    
            JsonObject dates = new JsonObject();
            
            if(nav.size()>0){
                try {
                    Date startDateObj = new Date(nav.get(0).getDate().getTime());
                    Calendar calendar1 = Calendar.getInstance();    
                    calendar1.setTime(startDateObj);
                    StringBuilder startDate = new StringBuilder("");
                    String[] dateSplit1 = calendar1.getTime().toString().split(" ");
                    startDate.append(dateSplit1[0]);
                    startDate.append(", ");
                    startDate.append(dateSplit1[1]);
                    startDate.append(" ");
                    startDate.append(dateSplit1[2]);
                    startDate.append(", ");
                    startDate.append(dateSplit1[5]);
                    dates.addProperty("startDate", startDate.toString());
        
                    Date endDateObj = new Date(nav.get(nav.size()-1).getDate().getTime());
                    Calendar calendar2 = Calendar.getInstance();    
                    calendar2.setTime(endDateObj);
                    StringBuilder endDate = new StringBuilder("");
                    String[] dateSplit2 = calendar2.getTime().toString().split(" ");
                    endDate.append(dateSplit2[0]);
                    endDate.append(", ");
                    endDate.append(dateSplit2[1]);
                    endDate.append(" ");
                    endDate.append(dateSplit2[2]);
                    endDate.append(", ");
                    endDate.append(dateSplit2[5]);
                    dates.addProperty("endDate", endDate.toString());
    
                } catch(Exception e){
                    log.log(Level.WARNING, "getDiveDates(): unable to parse dates - DiveService");
                    e.printStackTrace();
                }
    
            }
    
            cache.setData(key.toString(), dates.toString());
            return dates.toString();
        }
    }

    public String getCTD(String rovName, int diveNum) {
        StringBuilder key = new StringBuilder();
        key.append("CTD");
        key.append(rovName);
        key.append(Integer.toString(diveNum));

        String data = cache.getData(key.toString());

        if (data != null) {
            return data;
        } else { 
            Dive dive = fetchDiveObject(rovName, diveNum);

            CtdDatumDAO ctdDao = new CtdDatumDAOImpl();
            List<CtdDatum> ctd = ctdDao.fetchCtdData(dive);
    
            if(dive == null || ctd == null){
                System.out.println("getCTD(): null dive");
                return "No dive available";
            }
    
            JsonArray ctdArray = new JsonArray();
    
            for(int i = 0 ; i < ctd.size();i++){
                JsonObject ctdObject = new JsonObject();
                ctdObject.addProperty("salinity", ctd.get(i).getSalinity());
                ctdObject.addProperty("pressure", ctd.get(i).getPressure());
                ctdObject.addProperty("temperature", ctd.get(i).getTemperature());
                ctdArray.add(ctdObject);
            }
    
            cache.setData(key.toString(), ctdArray.toString());
            return ctdArray.toString();
        }
    }

    private Dive fetchDiveObject(String rovName, int diveNum) {
        DiveDAO dao = new DiveDAOImpl();
        Dive dive = dao.findByPlatformAndDiveNumber(rovName, diveNum);
        
        return dive;
    }
}