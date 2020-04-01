package org.mbari.m3.dive.visualizer;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.helidon.webserver.Routing;
import io.helidon.webserver.Service;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class PhotoAnnotationService implements Service {
    private final Logger log = Logger.getLogger(getClass().getName());
    AnnotationData annotationData = new AnnotationData();
    Utilities utilities = new Utilities();
    PhotoVideoData photoAnnotationFunctionality = new PhotoVideoData();

    Cache<String, AnnotationData> cache = Caffeine
        .newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .maximumSize(100)
        .build();

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/{rov}/{diveNumber}", (req, res) -> {
            try {
                utilities.headersRespondSend(photoAnnotationFunctionality.getRovPhotoAnnotations(req), res); 
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });

    }
}
