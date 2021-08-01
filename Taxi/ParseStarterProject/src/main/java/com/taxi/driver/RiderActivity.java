package com.taxi.driver;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.starter.R;

import java.util.ArrayList;
import java.util.List;

public class RiderActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    LocationManager locationManager;
    LocationListener locationListener;
    Button callTaxiButton;
    TextView infoTextView;
    boolean driverActive = false;
    boolean requestActive = false;// to check you have whether ordered a taxi fo not
    Handler handler = new Handler();

    public void checkForUpdates() {
        ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Call");


        query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());


        query.whereExists("driverUsername");


        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null && objects.size() > 0)

                {
                    driverActive = true;
                    //this is for getting distance from driver
                    ParseQuery<ParseUser> query1 = ParseUser.getQuery();


                    query1.whereEqualTo("username", objects.get(0).getString("driverUsername"));


                    query1.findInBackground(new FindCallback<ParseUser>() {
                        @Override
                        public void done(List<ParseUser> objects, ParseException e) {
                            if (e == null && objects.size() > 0) {
                                // we get driver location from user class geopoint
                                ParseGeoPoint taxiLocation = objects.get(0).getParseGeoPoint("location");
                                if (ContextCompat.checkSelfPermission(RiderActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);


                                    if (lastKnownLocation != null) {
                                        ParseGeoPoint userLocation = new ParseGeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());

                                        Double distacne = taxiLocation.distanceInKilometersTo(userLocation);
                                        if (distacne < 0.01) {
                                            infoTextView.setText("your driver has arrived");
                                            // all of this is to delete the query because e no longer required it
                                            ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Call");


                                            query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());


                                            query.findInBackground(new FindCallback<ParseObject>() {
                                                @Override
                                                public void done(List<ParseObject> objects, ParseException e) {
                                                    if (e == null && objects.size() > 0) {


                                                        for (ParseObject object : objects) {
                                                            object.deleteInBackground();
                                                        }
                                                    }
                                                }
                                            });
                                            handler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    infoTextView.setText("");
                                                    callTaxiButton.setVisibility(View.VISIBLE);
                                                    callTaxiButton.setText("Call a Taxi");
                                                    requestActive = false;
                                                    driverActive = false;
                                                }
                                            }, 7000);

                                        } else {
                                            Double distanceRoundOff = (double) Math.round(distacne * 10) / 10;

                                            infoTextView.setText("Your Ride is" + distanceRoundOff.toString() + "km away");
                                            //now final work is left is showing rider the driver location
                                            LatLng driverLocationCordi = new LatLng(taxiLocation.getLatitude(), taxiLocation.getLongitude());
                                            // THIS IS LOCATION WHICH WE HAVE ACEEPTED TO SERVE
                                            LatLng requestLocationCordi = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());

                                            ArrayList<Marker> markers = new ArrayList<>();
                                            mMap.clear();

                                            markers.add(mMap.addMarker(new MarkerOptions().position(driverLocationCordi).title("Driver Locarion")));
                                            //just changing the color of marker
                                            markers.add(mMap.addMarker(new MarkerOptions().position(requestLocationCordi).title("Your Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))));
                                            //WE ARE LOOPING THROUGH MARKERS AND GENERATING BOUNDS

                                            LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                            for (Marker marker : markers) {
                                                builder.include(marker.getPosition());
                                            }
                                            LatLngBounds bounds = builder.build();


                                            int padding = 60; // offset from edges of the map in pixels
                                            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);

                                            mMap.animateCamera(cu);
                                            callTaxiButton.setVisibility(View.INVISIBLE);
                                            handler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    checkForUpdates();
                                                }
                                            }, 2000);
                                        }
                                    }
                                }

                            }
                        }
                    });


                }


            }
        });

    }

    public void logout(View view) {

        ParseUser.logOut();

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);


    }

    public void callTaxi(View view) {
        if (requestActive == true) {
            // it means we need to cancel it here
            ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Call");
            query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if (e == null) {
                        if (objects.size() > 0) {
                            for (ParseObject object : objects) {
                                object.deleteInBackground();
                            }
                            requestActive = false;
                            callTaxiButton.setText("Call Taxi");
                        }
                    }
                }
            });
        } else {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocation != null) {
//                  updateMap(lastKnownLocation);

                    ParseObject user = new ParseObject("Call");
                    user.put("username", ParseUser.getCurrentUser().getUsername());
                    //for location parse has built geo point so use that
                    ParseGeoPoint parseGeoPoint = new ParseGeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                    user.put("location", parseGeoPoint);
                    user.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                callTaxiButton.setText("Cancel Taxi");
                                requestActive = true;
                                checkForUpdates();
                            }
                        }
                    });
                } else {
                    Toast.makeText(this, "could not find location.Please try again later.", Toast.LENGTH_SHORT).show();
                }


            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                updateMap(lastKnownLocation);
            }
        }

    }

    public void updateMap(Location location) {
        // while placing the map we dont want to update the map
        if (driverActive == false) {

            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.clear();
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
            mMap.addMarker(new MarkerOptions().position(userLocation).title("Your Location"));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider);
        infoTextView = findViewById(R.id.infoTextView);
        //Intent directionsIntent = new Intent(android.content.Intent.ACTION_VIEW,Uri.parse("http://maps.google.com/maps?saddr=" + intent.getDoubleExtra("driverLatitude", 0) + "," + intent.getDoubleExtra("driverLongitude", 0) + "&daddr=" + intent.getDoubleExtra("requestLatitude", 0) + "," + intent.getDoubleExtra("requestLongitude", 0))); directionsIntent.setPackage("com.google.android.apps.maps"); startActivity(directionsIntent);


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        callTaxiButton = (Button) findViewById(R.id.callTaxiButton);
        ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Call");
        query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    if (objects.size() > 0) {
                        requestActive = true;
                        callTaxiButton.setText("Cancel Taxi");
                        checkForUpdates();
                    }
                }
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateMap(location);

            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };
        if (Build.VERSION.SDK_INT < 27) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
              return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
          }
          else
          {
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED)
                {
                    ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
                }
                else
                {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                     Location lastKnownLocation=locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                       if(lastKnownLocation!=null)
                       {
                           updateMap(lastKnownLocation);
                       }
                }

          }
          }
}
