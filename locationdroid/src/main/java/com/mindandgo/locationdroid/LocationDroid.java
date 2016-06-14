package com.mindandgo.locationdroid;

import android.Manifest;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.RequiresPermission;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Xavier Bauquet <xavier.bauquet@gmail.com> on 20/05/2016.
 */
public abstract class LocationDroid implements LocationListener {

    private final LocationManager locationManager;
    private Location currentLocation = null;
    private float precision;
    private float maxTimeBetweenUpdates;
    private boolean usingGps = true;
    private boolean usingNetwork = true;
    private boolean usingPassive = true;

    // ==========================================================
    // Error messages
    // ==========================================================
    public static final String PRECISION_ERROR = "Precision cannot be lower than 1";
    public static final String MAX_TIME_ERROR = "Default max time between updates cannot be lower than 1";

    // ==========================================================
    // Constants
    // ==========================================================
    public static final float DEFAULT_PRECISION = 10f;
    public static final float DEFAULT_MAX_TIME = 30f;

    // ==========================================================
    // Constructors
    // ==========================================================
    /**
     *
     * @param context , the context to be used to construct the Location Service
     */
    @RequiresPermission(anyOf = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION})
    public LocationDroid(Context context) throws SecurityException {
        this(context, DEFAULT_PRECISION);
    }

    /**
     *
     * @param context , the context to be used to construct the Location Service
     * @param precision , the number of meters between two currentLocation updates, this value is used to
     *                  calculate the maximum time between two currentLocation updates according the speed.
     *                  The maxTimeBetweenUpdates is used if no speed available.
     *                  Default = 10 meters.
     */
    @RequiresPermission(anyOf = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION})
    public LocationDroid(Context context, float precision) throws SecurityException{
        this(context, precision, DEFAULT_MAX_TIME);
    }

    /**
     *
     * @param context , the context to be used to construct the Location Service
     * @param precision , the number of meters between two currentLocation updates, this value is used to
     *                  calculate the maximum time between two currentLocation updates according the speed.
     *                  The maxTimeBetweenUpdates is used if no speed available.
     *                  Default = 10 meters.
     * @param maxTimeBetweenUpdates , the maximum time between two currentLocation updates,
     *                              if their is not best currentLocation provided during this maximum time
     *                              the current currentLocation is updated even if this one has a worst
     *                              accuracy.
     *                              Default = 30 seconds
     */
    @RequiresPermission(anyOf = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION})
    public LocationDroid(Context context, float precision, float maxTimeBetweenUpdates) throws SecurityException{
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.precision = precision;
        this.maxTimeBetweenUpdates = maxTimeBetweenUpdates;
    }

    // ==========================================================
    // Last best known location
    // ==========================================================
    private void getLastBestKnowLocation() throws SecurityException{
        Location gpsLocation = null;
        Location networkLocation;
        Location passiveLocation;
        Location location = null;

        if (usingGps && isGpsServiceOn()) {
            gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if(gpsLocation != null){
                location = gpsLocation;
            }
        }
        if (usingNetwork && isNetworkServiceOn()) {
            networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (networkLocation != null && location != null) {
                location = accuracyAndTimeComparator(networkLocation, gpsLocation);
            }else if(networkLocation != null){
                location = networkLocation;
            }
        }
        if (usingPassive && isPassiveServiceOn()) {
            passiveLocation = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            if (passiveLocation != null && location != null) {
                location = accuracyAndTimeComparator(passiveLocation, location);
            }else if(passiveLocation != null){
                location = passiveLocation;
            }
        }
        this.currentLocation = location;
    }

    private Location accuracyAndTimeComparator(Location location1, Location location2) {
        Location bestTime = null;
        Location bestAccuracy = null;

        // if location times are different, get the newest location
        if (location1.getTime() != location2.getTime()) {
            bestTime = timeComparator(location1, location2);
        }
        // if the accuracies are different, get the best accuracy
        if (location1.hasAccuracy() && location2.hasAccuracy() &&
                location1.getAccuracy() != location2.getAccuracy()) {
            bestAccuracy = accuracyComparator(location1, location2);
        }
        // if only location1 has an accuracy
        else if(location1.hasAccuracy() && !location2.hasAccuracy()){
            bestAccuracy = location1;
        }
        // if only location2 has an accuracy
        else if(!location1.hasAccuracy() && location2.hasAccuracy()) {
            bestAccuracy = location2;
        }

        // if the location with the best accuracy is also the location with the best time,
        // this location is the best location of the both
        if(bestAccuracy == bestTime){
            return bestAccuracy;
        }else if(bestAccuracy == null){
            return bestTime;
        }else if(bestTime == null){
            return bestAccuracy;
        }
        // In all other cases it's not possible to determine which one is the best, return null
        else{
            return null;
        }
    }

    private Location accuracyComparator(Location location1, Location location2) {
        if (location1.getAccuracy() < location2.getAccuracy()) {
            return location1;
        } else {
            return location2;
        }
    }

    private Location timeComparator(Location location1, Location location2) {
        if (location1.getTime() < location2.getTime()) {
            return location2;
        } else {
            return location1;
        }
    }

    // ==========================================================
    // Best location selection
    // ==========================================================

    /**
     * Called when a new location is provided.
     *
     * @see android.location.LocationListener#onLocationChanged(android.location.Location)
     */
    @Override
    public void onLocationChanged(Location newLocation) {

        float maxTimeBetweenUpdates = getMaxTimeBetweenUpdates(newLocation);

        if (currentLocation != null) {
            // if the newLocation accuracy is better than the location accuracy
            if (newLocation.getAccuracy() <= currentLocation.getAccuracy()) {
                replaceLocation(newLocation);
            }
            // if the time difference between the both location is bigger than
            // maxTimeBetweenUpdates
            else if (newLocation.getTime() - this.currentLocation.getTime() > maxTimeBetweenUpdates) {
                replaceLocation(newLocation);
            }
        } else {
            replaceLocation(newLocation);
        }
    }

    private void replaceLocation(Location newLocation) {
        this.currentLocation = newLocation;
        onNewLocation(newLocation);
    }

    private float getMaxTimeBetweenUpdates(Location newLocation) {
        // The maximum time between location update is calculated according the 'precision'
        // and the speed given by the last location.
        // The maxTimeBetweenUpdates is the time that the user need to do the distance set
        // by the precision.
        float time = precision / newLocation.getSpeed();

        // Default value for maxTimeBetweenUpdates
        float maxTimeBetweenUpdates = this.maxTimeBetweenUpdates;

        // if the calculated maxTimeBetweenUpdates is lower than the default value and not
        // equal to zero, the calculated value is the new maxTimeBetweenUpdates
        if (time < maxTimeBetweenUpdates && time != 0) {
            maxTimeBetweenUpdates = time;
        }
        return maxTimeBetweenUpdates;
    }

    /**
     * Call back to be used to listen at location changes
     *
     * @param currentLocation , the current best location.
     */
    public abstract void onNewLocation(Location currentLocation);

    // ==========================================================
    // Start / Stop location updates
    // ==========================================================

    /**
     * Start the location service
     * @return the best last known location. Could return null.
     *
     */
    public Location start() throws SecurityException{
        if (usingGps) {
            if (isGpsServiceOn()) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, (long) maxTimeBetweenUpdates, precision, this);
            }
        }
        if (usingNetwork) {
            if (isNetworkServiceOn()) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, (long) maxTimeBetweenUpdates, precision, this);
            }
        }
        if (usingPassive) {
            if (isPassiveServiceOn()) {
                locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, (long) maxTimeBetweenUpdates, precision, this);
            }
        }
        getLastBestKnowLocation();
        return currentLocation;
    }

    /**
     * Stop the location service
     */
    public void stop() throws SecurityException {
        locationManager.removeUpdates(this);
    }

    // ==========================================================
    //
    // ==========================================================
    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        serviceProviderStatusListener(s, i, bundle);
    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    /**
     * Callback called when a provider (GPS, Network or Passive) change is status.
     *
     * @param provider , name of the provider
     * @param status , status of the provider: OUT_OF_SERVICE, TEMPORARILY_UNAVAILABLE or AVAILABLE
     * @param bundle , optional Bundle which will contain provider specific status variables.
     *
     * @see android.location.LocationListener#onStatusChanged(String s, int i, Bundle bundle)
     */
    public abstract void serviceProviderStatusListener(String provider, int status, Bundle bundle);

    // ==========================================================
    // Service providers
    // ==========================================================

    private boolean isGpsServiceOn() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private boolean isNetworkServiceOn() {
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private boolean isPassiveServiceOn() {
        return locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER);
    }

    /**
     *
     * @return The list of unavailable providers
     */
    public List<String> unavailableProviders(){
        List<String> unavailable = new ArrayList<>();
        if(!isGpsServiceOn()){
            unavailable.add(LocationManager.GPS_PROVIDER);
        }
        if(!isNetworkServiceOn()){
            unavailable.add(LocationManager.NETWORK_PROVIDER);
        }
        if(!isPassiveServiceOn()){
            unavailable.add(LocationManager.PASSIVE_PROVIDER);
        }
        return unavailable;
    }
    // ==========================================================
    // Getters
    // ==========================================================
    public float getPrecision() {
        return precision;
    }

    public float getMaxTimeBetweenUpdates() {
        return maxTimeBetweenUpdates;
    }

    // ==========================================================
    // Setters -- Options
    // ==========================================================
    public LocationDroid setPrecision(float precision) {
        if (precision <= 0) {
            throw new IllegalArgumentException(PRECISION_ERROR);
        } else {
            this.precision = precision;
        }
        return this;
    }

    public LocationDroid setMaxTimeBetweenUpdates(float maxTimeBetweenUpdates) {
        if (precision <= 0) {
            throw new IllegalArgumentException(MAX_TIME_ERROR);
        } else {
            this.maxTimeBetweenUpdates = maxTimeBetweenUpdates;
        }
        return this;
    }

    public LocationDroid setUsingGps(boolean usingGps) {
        this.usingGps = usingGps;
        return this;
    }

    public LocationDroid setUsingNetwork(boolean usingNetwork) {
        this.usingNetwork = usingNetwork;
        return this;
    }

    public LocationDroid setUsingPassive(boolean usingPassive) {
        this.usingPassive = usingPassive;
        return this;
    }
}

