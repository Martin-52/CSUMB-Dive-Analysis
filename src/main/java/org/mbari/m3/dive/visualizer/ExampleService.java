package org.mbari.m3.dive.visualizer;

import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

import org.mbari.expd.Dive;
import org.mbari.expd.DiveDAO;
import org.mbari.expd.jdbc.DiveDAOImpl;

public class ExampleService implements Service {

    @Override
    public void update(Routing.Rules rules) {
        rules
            .get("/{rov}/{diveNumber}", this::getDefaultMessageHandler);
    }

    private void getDefaultMessageHandler(ServerRequest request, ServerResponse response) {
        String rov = request.path().param("rov");
        Integer diveNumber = Integer.parseInt(request.path().param("diveNumber"));
        
//        DiveDAO dao = new DiveDAOImpl();
//        Dive dive = dao.findByPlatformAndDiveNumber(diveNumber, 123);
        
        
        String msg = String.format("Returning result for: %s %d!", rov, diveNumber);

        
        response.send(msg);
        
        //response.send(dive);
    }
}
