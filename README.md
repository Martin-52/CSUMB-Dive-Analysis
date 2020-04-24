# CSUMB-Dive-Analysis
To run backend: 
1. mvn package
2. java -jar target/dive-video-visualizer.jar


To retrieve dive stats:<br>

%cam log coverage: 
* hd footage: localhost:8080/dataerror/camcoveragehd/{rovName}/{diveNumber}
* Sd footage: localhost:8080/dataerror/camcoveragesd/{rovName}/{diveNumber}<br>

%nav log coverage: 
* localhost:8080/dataerror/navcoverage/{rovName}/{diveNumber}<br>

%ctd log coverage: 
* localhost:8080/dataerror/ctdcoverage/{rovName}/{diveNumber}
Annotations missing timestamps:
* localhost:8080/dataerror/annotations/{rovName}/{diveNumber} 
Annotations missing ancillary data:
* localhost:8080/dataerror/ancillary/{rovName}/{diveNumber}

=== Backend Overview ===

Each Service (i.e DataErrorService, DiveService, PhotoAnnotationService, etc) are the endpoints. 
These services have their own file that handles functionality. These file names end with 'Data'.
For example, DataErrorService has the endpoints and DataErrorData holds all functionality.

*DiveAnnotationService and PhotoAnnotationService share the same file for getting the correct data.
The file they share is PhotoVideoData.*

Utilities.java only carries the correct response headers to send to the front end. 

AnnotationData.java has two methods. One method initializes annotation data from a dive (private), and
the other -getAnnotationDataFromCache- returns the annotation json object if it exists in the cache, if not,
it calls the other.

SingletonCache.java carries the cache object. The cache object consists of a string key and value.


=== Classes in Depth ===
SingletonCache.java:
For the cache, we keep the results of json objects (as Strings). For example, in the class DataErrorData,
for the method getCameraLogCoverageRatioOfDive, we save the ratio in the cache and not the camera samples we 
fetched from CameraDatumDAO.fetchCameraData.

Improvements for Cache:
When getting data from the cache, we call our method, check if it's null, and either return the value or compute the value.
There is a method --Cache.get(String key, Function<? super String,? extends String> mappingFunction) -- that makes the code a lot cleaner.
Since we didn't want all classes to have direct access to the cache, we wanted to pass in this Function (Function<? super String,? extends String> mappingFunction)
to a method in the SingletonCache, but we ran out of time to learn how to access that passed function. 


PhotoVideoData.java:
This class handles all information for the Photos and Videos page for our frontend. We merged the functionality of these separate services because they shared similar methods. 

The information 'sent' to the front end -for both services- is structured in the same way. It is a JsonObject
of video (or photo) links and annotations. Each video link is a key, and their value is a list of ordered annotations. These annotations
are gathered by their corresponding reference id, and the annotations made during the duration of the video.
In addition to the video links and annotations, there is also a key named "video (or photo) Ordering". This 'videoOrdering' key has a list of ordered video (or photo) LINKS. The front end will loop through these ordered links and use the values to 
access the correct annotations in the jsonObject (map). 

The main methods in this class are getRovDiveAnnotations, getRovPhotoAnnotations, getVidsAndAnnotations, and getPhotoUrlsAndAnnotations.
The methods getRovDiveAnnotations and getRovPhotoAnnnotations do the same thing. First they check the cache to see if
the JsonObject already exists (links and annotations), if it does not, then they call their respective methods to gather that information
(getVidsAndAnnotations and getPhotoUrlsAndAnnotations)


This method is important. 
* getVideosAndAnnotations(...) Overview And Improvements: 
The mbari json object has two parts: annotations and media.
First we get only the media, and then we get the mp4 links. For each link, we have to find its corresponding .mov link to eventually get the information we want. We get this video_reference_uuid, recorded_timestamp, and duration_millis. 

After that nested for loop, there is another loop to get each video link's corresponding annotations. Finally, we get the ordered list of video links for the use of the frontend.

This method is slow. Luckily they only compute once, but it can be improved.

Ordering Methods:
When ordering the video or photo links, we order by their timestamp. We only order by hour, minute, and seconds. We DO NOT order by day. For example, a timestamp may have this time '16:45:05.001'. We turn that into 164505, and order those objects least to greatest based on 
this number. 

BUGS: There is an issue ordering the annotations. The timestamps are not completely ordered. The first few annotations are not ordered by timestamp. The method
that handles ordering annotations is sortAnnotationArray().
On the video page, we do not think ordering by timestamp is a good idea anyway. Since the video highlights the current annotation, it would be best to order the 
annotations by their 'duration' variable. The variable that tells the amount of time from the beginning of the video that the annotation was made. This would prevent the list of annotations to 'jump' around while the video is playing. 


