# nuancecurl
An CURL connection to NUANCE Dictation Interface

This small API provides the functionality to access the 
Nuance Dragon [SDK](http://www.nuance.de/for-developers/dragon/client-sdk/index.htm) using the CURL command from Linux.

 
**Example**

     File shellScript = new File("ABSOLUTE_PATH_TO/nuance_curl.sh");
     
     NuanceConnection nuance = new NuanceConnection(hostServer, apiKey, apiId, randomId, language, topic, contentType, shellScript);
     nuance.addResponseListener(new NuanceResponseListener() {
       @Override
       public void onResponse(NuanceResponse nr) {
         // do something with nr
       }
     });
