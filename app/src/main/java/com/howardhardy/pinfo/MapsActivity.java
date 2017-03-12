package com.howardhardy.pinfo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.client.Firebase;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firebase_core.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.howardhardy.pinfo.R.id.email;
import static com.howardhardy.pinfo.R.id.password;
import static com.howardhardy.pinfo.R.id.plain;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int REQUEST_CODE_ASK_PERMISSIONS = 7;
    private boolean activePermission = false;
    private GoogleMap mMap;

    //Database Instance / Reference
    private static final String TAG = "MapsActivity";
    FirebaseDatabase database = FirebaseDatabase.getInstance();

    private String userEmail;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        //Tracking sign in and outo

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Used to get the users email
        Intent intent = getIntent();
        userEmail = intent.getStringExtra("userEmail");

        reqPermissions();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            mMap.setMyLocationEnabled(true);
            Double[] ll = new Double[]{getLat(),getLong()};
            LatLng lalo = new LatLng(ll[0], ll[1]);

            if(!(ll[0].toString().equals(null) || ll[1].toString().equals(null)))
            {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lalo, 16));
            }

            readPins();
        }
    }

    private void reqPermissions() {
        //Checks to see if the permission has been granted
        int hasLocPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (hasLocPermission != PackageManager.PERMISSION_GRANTED) {
            //Requests the permission
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_ASK_PERMISSIONS);
            return;
        }else{
            activePermission = true;
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    activePermission = true;
                } else {
                    // Permission Denied
                    activePermission = false;
                    Toast.makeText(this, "ACCESS_FINE_LOCATION Denied", Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void putPin(LatLng pin, String place, String Message){
        if(!(pin.equals(null))){

            //Sets up the pin with its properties
            Marker marker = mMap.addMarker(
                                new MarkerOptions()
                                    .position(pin)
                                    .title(place)
                                    .snippet(Message));

            //marker.showInfoWindow();
        }
    }

    public void readPins(){
        DatabaseReference readPinRef = database.getReference().child("pins");
        readPinRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot){

                for(DataSnapshot data : dataSnapshot.getChildren()){
                    String Lat = (String) data.child("Latitude").getValue();
                    String Long = (String) data.child("Longitude").getValue();
                    String message = (String) data.child("Message").getValue();
                    String place = (String) data.child("Place").getValue();

                    LatLng lalo = new LatLng(Double.parseDouble(Lat), Double.parseDouble(Long));

                    putPin(lalo, place, message);
                }

            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }

        });
    }

    public void savePin(LatLng pin, String placeName, String message){

        DatabaseReference postPinRef = database.getReference().child("pins");

        Map<String, String> values = new HashMap<String, String>();

        //Putting the values into the hashmap to then be put into the database, with push. Giving a unique id
        values.put("Place", placeName);
        values.put("Latitude", String.valueOf(pin.latitude));
        values.put("Longitude", String.valueOf(pin.longitude));
        values.put("Message", message);
        values.put("Rating", String.valueOf(1.0));
        values.put("Email", userEmail);
        postPinRef.push().setValue(values);

        /*//Write a message to the database
        DatabaseReference myRef = database.getReference("pins/" + placeName);
        myRef.setValue(pin);*/

    }

    public void searchMarker(View view){
        EditText editText = (EditText)findViewById(R.id.editText);
        String location = editText.getText().toString();
        String message = "Hello, this is the message for this pin.";
        List<Address> addressList = null;

        if(!(location.equals(null) || location.equals("")))
        {
            Geocoder geocoder = new Geocoder(this);
            try {
                addressList = geocoder.getFromLocationName(location,1);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Address address = addressList.get(0);
            LatLng ll = new LatLng(address.getLatitude(),address.getLongitude());
            savePin(ll, address.getFeatureName(), message);
            putPin(ll, address.getFeatureName(), message);
        }

    }

    public Double getLat() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                return location.getLatitude();
            }
        }
        return null;
    }

    public Double getLong() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                return location.getLongitude();
            }
        }
        return null;
    }
}