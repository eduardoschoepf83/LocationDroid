package com.mindandgo.locationdroid;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by Xavier Bauquet <xavier.bauquet@gmail.com> on 20/05/2016.
 */
public class LocationDroidTest{

    private LocationManager mockLocationManager;
    private Context mockContext;

    private Location firstLocation;
    private Location thirdLocation;
    private Location secondLocation;

    private Location bestLocation;

    final void setBestLocation(Location location) {
        bestLocation = location;
    }

    final Location getBestLocation() {
        return bestLocation;
    }

    @Before
    public void init() {
        mockLocationManager = buildLocationManager();
        mockContext = buildContext(mockLocationManager);

        firstLocation = mock(Location.class);

        secondLocation = mock(Location.class);

        thirdLocation = mock(Location.class);

    }

    @Test
    public void shouldProvideLocationWithBestAccuracy() throws SecurityException{
        LocationDroid locationDroid = new LocationDroid(mockContext) {
            @Override
            public void onNewLocation(Location location) {
                setBestLocation(location);
            }

            @Override
            public void serviceProviderStatusListener(String provider, int status, Bundle bundle) {

            }
        };

        // Give a medium accuracy location
        when(secondLocation.getAccuracy()).thenReturn(10f);
        locationDroid.onLocationChanged(secondLocation);
        assertThat(getBestLocation()).isSameAs(secondLocation);

        // Give a bad accuracy location
        when(thirdLocation.getAccuracy()).thenReturn(20f);
        locationDroid.onLocationChanged(thirdLocation);
        assertThat(getBestLocation()).isSameAs(secondLocation);

        // Give a good accuracy location
        when(firstLocation.getAccuracy()).thenReturn(2f);
        locationDroid.onLocationChanged(firstLocation);
        assertThat(getBestLocation()).isSameAs(firstLocation);
    }

    @Test
    public void shouldProvideBestLocationAccordingTimeAndSpeed() throws SecurityException{
        LocationDroid locationDroid = new LocationDroid(mockContext) {
            @Override
            public void onNewLocation(Location location) {
                setBestLocation(location);
            }

            @Override
            public void serviceProviderStatusListener(String provider, int status, Bundle bundle) {

            }
        };

        // Add a first good accuracy location
        when(firstLocation.getAccuracy()).thenReturn(2f);
        when(firstLocation.getTime()).thenReturn(2l);
        locationDroid.onLocationChanged(firstLocation);
        assertThat(getBestLocation()).isSameAs(firstLocation);

        // Add a bad accuracy location with a time of s+3 and a speed of 1m/s
        // With the default precision (10m) this location should not be kept
        //
        // because:
        // 10(meters) / 1.4 (meter/second) = 7.14
        // So the user need 7.14 seconds to do the 10 meters of the default precision
        // and 7.14 > timeDifference (= 3)
        when(thirdLocation.getAccuracy()).thenReturn(20f);
        when(thirdLocation.getTime()).thenReturn(5l);
        when(thirdLocation.getSpeed()).thenReturn(1.4f); // = 5.04 km/h (person)
        locationDroid.onLocationChanged(thirdLocation);
        assertThat(getBestLocation()).isSameAs(firstLocation);

        // 10(meters) / 20 (meter/second) = 0.5
        // 0.5 < timeDifference (=5)
        // The new location is kept even if the accuracy is lower
        when(thirdLocation.getTime()).thenReturn(7l);
        when(thirdLocation.getSpeed()).thenReturn(20.0f); // = 72 km/h (car)
        locationDroid.onLocationChanged(thirdLocation);
        assertThat(getBestLocation()).isSameAs(thirdLocation);
    }

    @NonNull
    private Context buildContext(LocationManager mockLocationManager) {
        // Mock a context using the mocked location manager
        Context mockContext = mock(Context.class);
        when(mockContext.getSystemService(Context.LOCATION_SERVICE)).thenReturn(mockLocationManager);
        return mockContext;
    }

    @NonNull
    private LocationManager buildLocationManager() {
        // Mock a location manager with all location providers active
        LocationManager mockLocationManager = mock(LocationManager.class);
        when(mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);
        when(mockLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)).thenReturn(true);
        when(mockLocationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)).thenReturn(true);
        return mockLocationManager;
    }

}

