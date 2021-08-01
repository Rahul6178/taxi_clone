package com.taxi.driver;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.starter.R;

import java.util.ArrayList;
import java.util.List;

public class ViewRequestActivity extends AppCompatActivity {

    ListView requestListView;
    ArrayList<String> request = new ArrayList<>();
    ArrayAdapter arrayAdapter;
    ArrayList<Double> requestLatitudes = new ArrayList<>();
    ArrayList<Double> requestLongitudes = new ArrayList<>();
    ArrayList<String> usernames = new ArrayList<String>();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_file,menu);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.logut) {
            ParseUser.logOut();

            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    // private GoogleMap mMap;
    LocationManager locationManager;
    LocationListener locationListener;

    public void updateListView(Location location) {
        if (location != null) {
            ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Call");


            final ParseGeoPoint geoPoint = new ParseGeoPoint(location.getLatitude(), location.getLongitude());


            query.whereNear("location", geoPoint);
  // WE DONT WANT TO SHOW OURSELF IN THE LIST
            query.whereDoesNotExist("driverUsername");


            query.setLimit(10);


            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if (e == null) {
                        request.clear();


                        requestLongitudes.clear();


                        requestLatitudes.clear();

                        if (objects.size() > 0) {
                            for (ParseObject object : objects) {

                                ParseGeoPoint requestLocation = (ParseGeoPoint) object.get("location");

                                if (requestLocation != null) {

                                    Double distacne = geoPoint.distanceInKilometersTo(requestLocation);
                                    Double distanceRoundOff = (double) Math.round(distacne * 10) / 10;
                                    request.add(distanceRoundOff.toString() + "Km");
                                    requestLatitudes.add(requestLocation.getLatitude());
                                    requestLongitudes.add(requestLocation.getLongitude());
                                    usernames.add(object.getString("username"));
                                }
                            }
                        } else {
                            request.add("NO active request nearby");
                        }
                        arrayAdapter.notifyDataSetChanged();
                    }
                }
            });
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
                updateListView(lastKnownLocation);
            }
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_request);
        setTitle("Current Request");

        requestListView = findViewById(R.id.requestListView);


        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, request);


        request.clear();


        request.add("Getting nearby request.....");


        requestListView.setAdapter(arrayAdapter);
        Log.i("status3","we have been clicked");
        requestListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.i("status2","we have been clicked");
                if ( Build.VERSION.SDK_INT<27||(ContextCompat.checkSelfPermission(ViewRequestActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED))
                {
                    Log.i("status","we have been clicked");

//
                   Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    Log.i("status ", String.valueOf(lastKnownLocation.getLatitude())+"  "+String.valueOf(lastKnownLocation.getLongitude()));
//
//
                  if (requestLatitudes.size() > i && requestLongitudes.size() > i && lastKnownLocation != null&&usernames.size()>0) {
                        Log.i("status ","we are inside of if");
                        //TRANSFERRING OF DATA FROM THIS ACTIVITY TO DRIVERLOCATION ACTIVITY
                   Intent intent = new Intent(getApplicationContext(), DriverLocationActivity.class);
                        intent.putExtra("requestLatitude", requestLatitudes.get(i));
                        intent.putExtra("requestLongitude", requestLongitudes.get(i));
                        intent.putExtra("driverLatitude", lastKnownLocation.getLatitude());
                        intent.putExtra("driverLongitude",lastKnownLocation.getLongitude());
                        intent.putExtra("username", usernames.get(i));
                        startActivity(intent);
                    }
                }
                else
                {
                    Log.i("status","we have been not clicked");

                    Toast.makeText(getApplicationContext(),"something is not right",Toast.LENGTH_SHORT).show();
                }
            }
        });


        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);


        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateListView(location);
                //SAVINg the driver location here so that wecan use latter
                ParseUser.getCurrentUser().put("location",new ParseGeoPoint(location.getLatitude(),location.getLongitude()));
                ParseUser.getCurrentUser().saveInBackground();

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

                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }
        else
        {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
            }
            else
            {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);


                Location lastKnownLocation=locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);


                if(lastKnownLocation!=null)
                {
                    updateListView(lastKnownLocation);
                }

            }

        }



    }

}

