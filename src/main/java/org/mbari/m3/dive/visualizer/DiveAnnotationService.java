package org.mbari.m3.dive.visualizer;

import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.Collection;

import org.mbari.expd.Dive;
import org.mbari.expd.DiveDAO;
import org.mbari.expd.jdbc.DiveDAOImpl;
import org.json.simple.JSONObject;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.helidon.webserver.Routing;

public class DiveAnnotationService implements Service {

    @Override // this is called everytime this path is accessed
    public void update(Routing.Rules rules) {
        rules.get("/{rov}", (req, res) -> {
            try {
                getRovDiveAnnotations(req, res);
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });
    }
    

    /**
     * Returns a list of dives for the given ROV.
     * 
     * @param request
     * @param response
     * @throws InterruptedException
     * @throws IOException
     */
    private void getRovDiveAnnotations(ServerRequest request, ServerResponse response) throws IOException, InterruptedException {
        String rovname = "";
        String rovgiven = request.path().param("rov");
        Integer i = 0;
        for(; i < rovgiven.length(); i++) {
            if(rovgiven.charAt(i)==' '){
                break;
            }
            rovname+=rovgiven.charAt(i);
        }

        i+=1;
        String divenumber = "";
        for(; i < rovgiven.length(); i++) {
            divenumber+=rovgiven.charAt(i);
        }
        Integer result = Integer.parseInt(divenumber);
        //System.out.println(rovname + ":" + result + "!!");
        AnnotationServiceHelper annotationServiceClass = new AnnotationServiceHelper(rovname, result);
        // JsonArray annotations = annotationServiceClass.getAnnotations();// get json array from class
        // System.out.println(annotations.toString());
        // String anno = annotations.toString();// convert to string
        // JsonArray ans = new JsonParser().parse(anno).getAsJsonArray(); // can turn it back once sent
        // System.out.println(ans);

        


        // JsonArray vsjson = new JsonParser().parse('enter json string here').getAsJsonArray(); // can turn it back to JsonArray 
        // System.out.println(vsjson.get(0));
        // System.out.println(vsjson.get(1));
        // System.out.println(vsjson.get(2));


        JsonArray vids = annotationServiceClass.getVideoLinks();
        response.send(vids.toString());
    }
}
