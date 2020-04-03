package org.mbari.m3.dive.visualizer;

import io.helidon.webserver.Routing;
import io.helidon.webserver.Service;
import java.io.IOException;
import java.util.logging.Logger;

public class DiveService implements Service {
    private final Logger log = Logger.getLogger(getClass().getName());
    Utilities utilities = new Utilities();
    DiveData diveData = new DiveData();

    @Override
    public void update(Routing.Rules rules) {

        rules.get("/getRovNames", (req, res) -> {
            try {
                utilities.headersRespondSend(diveData.getRovNames(),res);
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

        rules.get("/getlatsandlongs/{rov}/{diveNumber}", (req, res) -> {
            try {
                utilities.headersRespondSend(diveData.getLatsAndLongs(
                    req.path().param("rov"),
                    Integer.parseInt(req.path().param("diveNumber"))), res);
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

        rules.get("/checkdivenumber/{rov}/{diveNumber}", (req, res) -> {
            try {
                utilities.headersRespondSend(diveData.checkDiveNumber(
                    req.path().param("rov"),
                    Integer.parseInt(req.path().param("diveNumber"))), res);
                
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

        rules.get("/getminanddepth/{rov}/{diveNumber}", (req, res) -> {
            try {
                utilities.headersRespondSend(diveData.getMinAndDepth(                    
                    req.path().param("rov"),
                    Integer.parseInt(req.path().param("diveNumber"))), res);
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

        rules.get("/gethouranddepth/{rov}/{diveNumber}", (req, res) -> {
            try {
                utilities.headersRespondSend(diveData.getHourAndDepth(                    
                    req.path().param("rov"),
                    Integer.parseInt(req.path().param("diveNumber"))), res);
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

        rules.get("/getdivedates/{rov}/{diveNumber}", (req, res) -> {
            try {
                utilities.headersRespondSend(diveData.getDiveDates(                    
                    req.path().param("rov"),
                    Integer.parseInt(req.path().param("diveNumber"))), res);
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

        rules.get("/getctd/{rov}/{diveNumber}", (req, res) -> {
            try {
                utilities.headersRespondSend(diveData.getCTD(                    
                    req.path().param("rov"),
                    Integer.parseInt(req.path().param("diveNumber"))), res);
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

        rules.get("/getgeneraldiveinformation/{rov}/{diveNumber}", (req, res) -> {
            try {
                utilities.headersRespondSend(diveData.getGeneralDiveInformation(                    
                    req.path().param("rov"),
                    Integer.parseInt(req.path().param("diveNumber"))), res);
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