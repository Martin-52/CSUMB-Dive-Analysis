package org.mbari.m3.dive.visualizer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.helidon.webserver.Routing;
import io.helidon.webserver.Service;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import java.io.IOException;
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

public class DiveService implements Service {
    private final Logger log = Logger.getLogger(getClass().getName());

    @Override
    public void update(Routing.Rules rules) {

        rules.get("/getRovNames", this::getRovNames);

        rules.get("/getLatsAndLongs/{rov}/{diveNumber}", (req, res) -> {
            try {
                getLatsAndLongs(req, res);
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });
        rules.get("/checkDiveNumber/{rov}/{diveNumber}", (req, res) -> {
            try {
                checkDiveNumber(req, res);
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });
        rules.get("/getMinAndDepth/{rov}/{diveNumber}", (req, res) -> {
            try {
                getMinAndDepth(req, res);
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });
        rules.get("/getHourAndDepth/{rov}/{diveNumber}", (req, res) -> {
            try {
                getHourAndDepth(req, res);
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });
        rules.get("/getDiveDates/{rov}/{diveNumber}", (req, res) -> {
            try {
                getDiveDates(req, res);
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });
        rules.get("/getCTD/{rov}/{diveNumber}", (req, res) -> {
            try {
                getCTD(req, res);
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });
    }


    private void getLatsAndLongs(ServerRequest request, ServerResponse response)throws IOException, InterruptedException {
        String rovName = request.path().param("rov");
        int diveNumber = Integer.parseInt(request.path().param("diveNumber"));

        DiveDAO dao = new DiveDAOImpl();
        Dive dive = dao.findByPlatformAndDiveNumber(rovName, diveNumber);

        if(dive==null){
            System.out.println("getLatsAndLongs(): null dive");
            return;
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

        response.headers().add("Access-Control-Allow-Origin", "*");
        response.headers().add("Access-Control-Allow-Headers", "*");
        response.headers().add("Access-Control-Allow-Credentials", "true");
        response.headers().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        response.send(latsAndLongs.toString());
    }

    private void checkDiveNumber(ServerRequest request, ServerResponse response)throws IOException, InterruptedException {

        String rovName = request.path().param("rov");
        int diveNumber = Integer.parseInt(request.path().param("diveNumber"));

        DiveDAO dao = new DiveDAOImpl();
        Collection<Dive> divesForRov = dao.findByPlatform(rovName);
        List<Integer> diveNumbersForRov = divesForRov
            .stream()
            .map(Dive::getDiveNumber)
            .sorted()
            .collect(Collectors.toList());
        
        boolean diveExists = false;
        for(int i = 0; i < diveNumbersForRov.size();i++){
            if(diveNumber == diveNumbersForRov.get(i)){
                diveExists = true;
            }
        }

        JsonObject existObj = new JsonObject();
        existObj.addProperty("exists", diveExists);

        response.headers().add("Access-Control-Allow-Origin", "*");
        response.headers().add("Access-Control-Allow-Headers", "*");
        response.headers().add("Access-Control-Allow-Credentials", "true");
        response.headers().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        response.send(existObj.toString());
    }

    private void getMinAndDepth(ServerRequest request, ServerResponse response)throws IOException, InterruptedException {
        String rovName = request.path().param("rov");
        int diveNumber = Integer.parseInt(request.path().param("diveNumber"));

        DiveDAO dao = new DiveDAOImpl();
        Dive dive = dao.findByPlatformAndDiveNumber(rovName, diveNumber);

        if(dive==null){
            log.log(Level.WARNING, "getLatsAndLongs(): null dive - DiveService");
            return;
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

        response.headers().add("Access-Control-Allow-Origin", "*");
        response.headers().add("Access-Control-Allow-Headers", "*");
        response.headers().add("Access-Control-Allow-Credentials", "true");
        response.headers().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        response.send(minAndDepth.toString());
    }

    private void getHourAndDepth(ServerRequest request, ServerResponse response)throws IOException, InterruptedException {
        String rovName = request.path().param("rov");
        int diveNumber = Integer.parseInt(request.path().param("diveNumber"));

        DiveDAO dao = new DiveDAOImpl();
        Dive dive = dao.findByPlatformAndDiveNumber(rovName, diveNumber);

        if(dive==null){
            log.log(Level.WARNING, "getHourAndDepth(): null dive - DiveService");
            return;
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

        response.headers().add("Access-Control-Allow-Origin", "*");
        response.headers().add("Access-Control-Allow-Headers", "*");
        response.headers().add("Access-Control-Allow-Credentials", "true");
        response.headers().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        response.send(hourAndDepth.toString());
    }
    
    /**
     * Returns a list of ROV names.
     * @param request
     * @param response
     */
    private void getRovNames(ServerRequest request, ServerResponse response) {
        JsonArray arr = new JsonArray();
        for (String name: BaseDAOImpl.getAllRovNames()) {
            arr.add(name);
        }
        response.headers().add("Access-Control-Allow-Origin", "*");
        response.headers().add("Access-Control-Allow-Headers", "*");
        response.headers().add("Access-Control-Allow-Credentials", "true");
        response.headers().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        response.send(arr.toString());
    }


    private void getDiveDates(ServerRequest request, ServerResponse response)throws IOException, InterruptedException {
        String rovName = request.path().param("rov");
        int diveNumber = Integer.parseInt(request.path().param("diveNumber"));
        DiveDAO dao = new DiveDAOImpl();
        Dive dive = dao.findByPlatformAndDiveNumber(rovName, diveNumber);

        if(dive==null){
            log.log(Level.WARNING, "getDiveDates(): null dive - DiveService");
            return;
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

        response.headers().add("Access-Control-Allow-Origin", "*");
        response.headers().add("Access-Control-Allow-Headers", "*");
        response.headers().add("Access-Control-Allow-Credentials", "true");
        response.headers().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        response.send(dates.toString());
    }
    
    private void getCTD(ServerRequest request, ServerResponse response)throws IOException, InterruptedException {
        String rovName = request.path().param("rov");
        int diveNumber = Integer.parseInt(request.path().param("diveNumber"));

        DiveDAO dao = new DiveDAOImpl();
        Dive dive = dao.findByPlatformAndDiveNumber(rovName, diveNumber);

        CtdDatumDAO ctdDao = new CtdDatumDAOImpl();
        List<CtdDatum> ctd = ctdDao.fetchCtdData(dive);

        if(dive == null || ctd == null){
            System.out.println("getCTD(): null dive");
            return;
        }

        JsonArray ctdArray = new JsonArray();

        for(int i = 0 ; i < ctd.size();i++){
            JsonObject ctdObject = new JsonObject();
            ctdObject.addProperty("salinity", ctd.get(i).getSalinity());
            ctdObject.addProperty("pressure", ctd.get(i).getPressure());
            ctdObject.addProperty("temperature", ctd.get(i).getTemperature());
            ctdArray.add(ctdObject);
        }

        response.headers().add("Access-Control-Allow-Origin", "*");
        response.headers().add("Access-Control-Allow-Headers", "*");
        response.headers().add("Access-Control-Allow-Credentials", "true");
        response.headers().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        response.send(ctdArray.toString());
    }
}
