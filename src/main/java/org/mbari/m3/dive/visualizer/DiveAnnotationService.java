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
import java.util.Map;

import org.mbari.expd.Dive;
import org.mbari.expd.DiveDAO;
import org.mbari.expd.jdbc.DiveDAOImpl;
import org.json.simple.JSONObject;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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

        JsonObject linksAndAnnotations = annotationServiceClass.getVideoLinksAndAnnotations();
        System.out.println("************************************************************************************");
        for(Map.Entry<String,JsonElement> entry : linksAndAnnotations.entrySet()){
    
            
            System.out.println("Video Link: " + entry.getKey());
            if(entry.getValue().getAsJsonArray().size()==0){
                System.out.println("No Annotations");
                System.out.println("************************************************************************************");
                continue;
            }
            
            // this will loop through each annotation
            for(int j = 0; j < entry.getValue().getAsJsonArray().size(); j++){
                System.out.println("Video Annotation " + (j+1));
                System.out.println(entry.getValue().getAsJsonArray().get(j));
                System.out.println("");
            }
            System.out.println("************************************************************************************");
        }

        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");

        String vaa = linksAndAnnotations.toString();
        System.out.println(vaa);

        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");

        JsonObject ans = new JsonParser().parse(vaa).getAsJsonObject();
        System.out.println("************************************************************************************");
        for(Map.Entry<String,JsonElement> entry : ans.entrySet()){
    
            
            System.out.println("Video Link: " + entry.getKey());
            if(entry.getValue().getAsJsonArray().size()==0){
                System.out.println("No Annotations");
                System.out.println("************************************************************************************");
                continue;
            }
            
            // this will loop through each annotation
            for(int j = 0; j < entry.getValue().getAsJsonArray().size(); j++){
                System.out.println("Video Annotation " + (j+1));
                System.out.println(entry.getValue().getAsJsonArray().get(j));
                System.out.println("");
            }
            System.out.println("************************************************************************************");
        }

        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");

        //System.out.println()

                
        //JsonArray vids = annotationServiceClass.getVideoLinks();
        response.send(linksAndAnnotations.toString());
    }
}
