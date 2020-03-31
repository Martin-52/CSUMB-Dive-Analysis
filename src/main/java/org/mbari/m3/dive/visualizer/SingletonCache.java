package org.mbari.m3.dive.visualizer;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mbari.expd.CameraDatum;
import org.mbari.expd.CameraDatumDAO;
import org.mbari.expd.CtdDatum;
import org.mbari.expd.CtdDatumDAO;
import org.mbari.expd.Dive;
import org.mbari.expd.DiveDAO;
import org.mbari.expd.NavigationDatum;
import org.mbari.expd.jdbc.CameraDatumDAOImpl;
import org.mbari.expd.jdbc.CtdDatumDAOImpl;
import org.mbari.expd.jdbc.DiveDAOImpl;
import org.mbari.expd.jdbc.NavigationDatumDAOImpl;
import mbarix4j.math.Matlib;
import mbarix4j.math.Statlib;

public class SingletonCache {
    // static variable single_instance of type Singleton 
    private static SingletonCache single_instance = null;
    public Cache<String, AnnotationData> cache; 
    
    private SingletonCache(){
        this.cache = Caffeine
            .newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(100)
            .build();
    }

    public static SingletonCache getInstance(){
        if(single_instance == null) {
            single_instance = new SingletonCache();
        }
        return single_instance;
    }

    public String getData(String rovName, int diveNumber){
        if(single_instance == null) return "no cache";
        AnnotationData data = cache.getIfPresent(rovName+diveNumber);
        if(data == null) return "no AnnotationData";
        return data.getData().toString();
    }
}