package org.mbari.m3.dive.visualizer;

import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import java.util.Collection;
import com.google.gson.JsonArray;
import org.mbari.expd.Dive;
import org.mbari.expd.DiveDAO;
import org.mbari.expd.jdbc.BaseDAOImpl;
import org.mbari.expd.jdbc.DiveDAOImpl;
import org.json.simple.JSONObject;

public class DiveService implements Service {

    @Override
    public void update(Routing.Rules rules) {
        rules
            .get("/getRovNames", this::getRovNames)
            .get("/{rov}", this::getRovDives);
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
        System.out.println(divesForRov.toArray()[0]);
        
        response.send(json.toJSONString());
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
}
