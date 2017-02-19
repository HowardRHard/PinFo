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
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.vision.barcode.Barcode;

import java.io.IOException;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int REQUEST_CODE_ASK_PERMISSIONS = 7;
    private boolean activePermission = false;
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


    }

    public void searchMarker(View view){
        EditText editText = (EditText)findViewById(R.id.editText);
        String location = editText.getText().toString();
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
            putPin(ll);
        }

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        reqPermissions();
        if(activePermission){
            mMap.setMyLocationEnabled(true);
            Double[] ll = new Double[]{getLat(),getLong()};
            LatLng lalo = new LatLng(ll[0], ll[1]);

            if(!(ll[0].toString().equals(null) || ll[1].toString().equals(null)))
            {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lalo, 16));
            }
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

    public void putPin(LatLng pin){
        if(!(pin.equals(null))){
            mMap.addMarker(new MarkerOptions().position(pin).title("Marker"));
            //mMap.moveCamera(CameraUpdateFactory.newLatLng(pin));
        }
    }

    public Double getLat() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (activePermission) {
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                return location.getLatitude();
            }
        }
        return null;
    }

    public Double getLong() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (activePermission) {
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                return location.getLongitude();
            }
        }
        return null;
    }
}