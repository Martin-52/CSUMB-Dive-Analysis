package org.mbari.m3.dive.visualizer;

import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import java.util.Collection;
import org.mbari.expd.Dive;
import org.mbari.expd.DiveDAO;
import org.mbari.expd.jdbc.DiveDAOImpl;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Calendar;
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
//import org.mbari.expd.jdbc.NavigationDAOImpl;
import org.mbari.expd.jdbc.NavigationDatumImpl;

import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

public class DiveService implements Service {

    @Override // this is called everytime this path is accessed
    public void update(Routing.Rules rules) {
        // rules.get("/{rov}", this::getRovDives);

        rules.get("/getlatsandlongs/{rov}/{diveNumber}", (req, res) -> {
            try {
                getLatsAndLongs(req, res);
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });
        rules.get("/checkdivenumber/{rov}/{diveNumber}", (req, res) -> {
            try {
                checkDiveNumber(req, res);
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });
        rules.get("/getminanddepth/{rov}/{diveNumber}", (req, res) -> {
            try {
                getMinAndDepth(req, res);
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });
        rules.get("/gethouranddepth/{rov}/{diveNumber}", (req, res) -> {
            try {
                getHourAndDepth(req, res);
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });
    }

    /**
     * Returns a list of dives for the given ROV.
     * @param request
     * @param response
     */
    private void getRovDives(ServerRequest request, ServerResponse response) {
        String rov = request.path().param("rov");
        DiveDAO dao = new DiveDAOImpl();
        Collection<Dive> divesForRov = dao.findByPlatform(rov);
        JSONObject json = new JSONObject();
        json.put("Dives", divesForRov);
        //System.out.println(divesForRov.toArray()[0]);
        
        response.send(json.toJSONString());
    }

    void getLatsAndLongs(ServerRequest request, ServerResponse response)throws IOException, InterruptedException {
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

    void checkDiveNumber(ServerRequest request, ServerResponse response)throws IOException, InterruptedException {

        String rovName = request.path().param("rov");
        int diveNumber = Integer.parseInt(request.path().param("diveNumber"));

        DiveDAO dao = new DiveDAOImpl();
        Collection<Dive> divesForRov = dao.findByPlatform(rovName);
        List<Integer> diveNumbersForRov = divesForRov.stream().map(Dive::getDiveNumber).sorted()
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

    public void getMinAndDepth(ServerRequest request, ServerResponse response)throws IOException, InterruptedException {
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

        JsonArray minAndDepth = new JsonArray();

        for(int i = 0 ; i < nav.size();i++){
            JsonObject newMinDepthObj = new JsonObject();
            newMinDepthObj.addProperty("minute", Double.toString(nav.get(i).getDate().getMinutes()));
            newMinDepthObj.addProperty("depth", Double.toString(nav.get(i).getDepth()));
            minAndDepth.add(newMinDepthObj);
        }

        response.headers().add("Access-Control-Allow-Origin", "*");
        response.headers().add("Access-Control-Allow-Headers", "*");
        response.headers().add("Access-Control-Allow-Credentials", "true");
        response.headers().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        response.send(minAndDepth.toString());
    }

    public void getHourAndDepth(ServerRequest request, ServerResponse response)throws IOException, InterruptedException {
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

        JsonArray hourAndDepth = new JsonArray();

        for(int i = 0 ; i < nav.size();i++){
            JsonObject newMinDepthObj = new JsonObject();
            newMinDepthObj.addProperty("hour", Double.toString(nav.get(i).getDate().getHours()));
            newMinDepthObj.addProperty("depth", Double.toString(nav.get(i).getDepth()));
            hourAndDepth.add(newMinDepthObj);
        }

        response.headers().add("Access-Control-Allow-Origin", "*");
        response.headers().add("Access-Control-Allow-Headers", "*");
        response.headers().add("Access-Control-Allow-Credentials", "true");
        response.headers().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        response.send(hourAndDepth.toString());
    }
}
