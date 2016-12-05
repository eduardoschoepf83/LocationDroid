#LocationDroid

LocationDroid is a simple open source library for location tracking. This library use the 3 default providers: GPS, Network and Passive. It aims to provide the best location from the 3 providers. The selection of the new best location is calculated according the speed and two parameters: the maximum time between updates (if the last best location is older than this value the next location from the providers become the new best location) and the distance between two locations.

##Use

```
LocationDroid locationDroid = new LocationDroid(context) {
    @Override
    public void onNewLocation(Location location) {
        // TODO : do something with the location
    }
    
    @Override
    public void onProviderEnabled(String s){
        // TODO : do something when a provider is enabled
    }
    
    @Override
    public void onProviderDisabled(String s){
        // TODO : do something when a provider is disabled
    }
    
    @Override
    public void onStatusChanged(String provider, int status, Bundle bundle){
        // TODO : do something when a provider change its status
    }
};
```

###Start the LocationDroid
```
try{
    locationDroid.start();
}catch (SecurityException s) {
    Log.e("Permissions Error", s.toString());
}
```

###Stop the LocationDroid
```
locationDroid.stop();
```

##Options
###Choose to not use one or more providers
```
locationDroid.setUsingGps(false);
locationDroid.setUsingNetwork(false);
locationDroid.setUsingPassive(false);
```

###Change the maximum time between two location updates
If the time since the last good location is greater than this value, the next found location will be given.
Default: 30 seconds
```
locationDroid.setMaxTimeBetweenUpdates(10f);
```

###Change the distance between two location updates
Default: 10 meters
```
locationDroid.setDistanceBetweenUpdates(5f);
```


Gradle
--------

```groovy
compile 'com.xavierbauquet.locationdroid:locationdroid:1.0.0'
```

License
--------

    Copyright 2016 Xavier Bauquet

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
