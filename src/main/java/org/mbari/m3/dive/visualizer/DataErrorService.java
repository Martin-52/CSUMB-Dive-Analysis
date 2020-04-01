package org.mbari.m3.dive.visualizer;

import io.helidon.webserver.Routing;
import io.helidon.webserver.Service;
import java.io.IOException;


public class DataErrorService implements Service {
    DataErrorData dataErrorFunctionality = new DataErrorData();
    Utilities utilities = new Utilities();


    @Override
    public void update(Routing.Rules rules) {

        rules.get("/timestamps/{rov}/{diveNumber}", (req, res) -> {
            try {
                utilities.headersRespondSend(dataErrorFunctionality.getAnnotationsWithMissingTimestamps(req), res);
                //missingTimestampsHttpResponse(req);
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });

        rules.get("/ancillary/{rov}/{diveNumber}", (req, res) -> {
            try {
                utilities.headersRespondSend(dataErrorFunctionality.getAnnotationsWithMissingAncillaryData(req), res);
                //missingAncillaryHttpResponse(req, res);
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });

        rules.get("/navcoverage/{rov}/{diveNumber}", (req, res) -> {
            try {
                utilities.headersRespondSend(dataErrorFunctionality.getNavCoverageRatioOfDive(req), res);
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
        });

        rules.get("/ctdcoverage/{rov}/{diveNumber}", (req, res) -> {
            try {
                utilities.headersRespondSend(dataErrorFunctionality.getCTDCoverageRatioOfDive(req), res); 
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
        });

        rules.get("/camcoveragehd/{rov}/{diveNumber}", (req, res) -> {
            try {
                utilities.headersRespondSend(dataErrorFunctionality.getCameraLogCoverageRatioOfDive(req,true), res); 
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
        });
        rules.get("/camcoveragesd/{rov}/{diveNumber}", (req, res) -> {
            try {
                utilities.headersRespondSend(dataErrorFunctionality.getCameraLogCoverageRatioOfDive(req,false), res); 
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
        });
        
    }
}
