package org.mbari.m3.dive.visualizer;

import io.helidon.webserver.Routing;
import io.helidon.webserver.Service;
import java.io.IOException;
import java.util.logging.Logger;

public class DiveAnnotationService implements Service {
    private final Logger log = Logger.getLogger(getClass().getName());
    Utilities utilities = new Utilities();

    PhotoVideoData videoAnnotationFunctionality = new PhotoVideoData();

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/{rov}/{diveNumber}", (req, res) -> {
            try {
                utilities.headersRespondSend(videoAnnotationFunctionality.getRovDiveAnnotations(req), res);
            } catch (IOException e ){
                Routing routing = Routing.builder()
                .error(IOException.class, (req1, res1, ex) -> { 
                    // handle the error, set the HTTP status code
                    res.send(ex.getMessage()); 
                })
                .build();
            } catch ( InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Routing routing = Routing.builder()
                    .error(InterruptedException.class, (req1, res1, ex) -> { 
                        // handle the error, set the HTTP status code
                        res.send(ex.getMessage()); 
                    })
                    .build();
            }
        });

    }
}