package org.mbari.m3.dive.visualizer;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;


public class SingletonCache {
    // static variable single_instance of type Singleton 
    private static SingletonCache single_instance = null;
    public Cache<String, String> cache; 
    
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

    public String getData(String key){
        return cache.getIfPresent(key);
    }

    public void setData(String key, String value) {
        cache.put(key, value);
    }
}