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
    public void serviceProviderStatusListener(String s, int i, Bundle bundle) {
        // TODO : do something when a status of one of the providers change
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
locationDroid.setPrecision(5f);
```
