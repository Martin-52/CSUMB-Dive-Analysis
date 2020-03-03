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

public class DiveService implements Service {

    @Override // this is called everytime this path is accessed
    public void update(Routing.Rules rules) {
        rules
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
}
