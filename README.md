# CSUMB-Dive-Analysis
To run backend: 
1. mvn package
2. java -jar target/dive-video-visualizer.jar


To retrieve dive stats:<br>

%cam log coverage: 
* hd footage: localhost:8080/dataerror/camcoveragehd/rovName/diveNumber
* Sd footage: localhost:8080/dataerror/camcoveragesd/rovName/diveNumber<br>

%nav log coverage: 
* localhost:8080/dataerror/navcoverage/rovName/diveNumber<br>

%ctd log coverage: 
* localhost:8080/dataerror/ctdcoverage/rovName/diveNumber
Annotations missing timestamps:
* localhost:8080/dataerror/annotations/rovName/diveNumber 
Annotations missing ancillary data:
* localhost:8080/dataerror/ancillary/rovName/diveNumber
