package com.howardhardy.pinfo;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.engine.Resource;
import com.firebase.client.Firebase;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
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
import java.util.Locale;
import java.util.Map;

import static com.howardhardy.pinfo.R.id.email;
import static com.howardhardy.pinfo.R.id.password;
import static com.howardhardy.pinfo.R.id.plain;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.InfoWindowAdapter{

    private static final int REQUEST_CODE_ASK_PERMISSIONS = 7;
    private boolean activePermission = false;
    private GoogleMap mMap;

    //Database Instance / Reference
    private static final String TAG = "MapsActivity";
    FirebaseDatabase database = FirebaseDatabase.getInstance();

    //The current users pin infomation if it has been input
    private String pinLocation;
    private Boolean pinCurrentLocation;
    private String pinDescription;

    //These are for the navigation drawer
    private String[] drawerTitles;
    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;

    private String currEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Gets the instance of the current user logged in
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        try{

            if(mUser.equals(null)){}
        }
        catch(NullPointerException n){
            //If there is no user then switch to the login
            //User not Signed in, provide them the opportunity to
            startActivity(new Intent(MapsActivity.this, LoginPage.class)); //Go to login page
            finish();
            return;
        }

        //If there is just continue setting up the main maps page
        currEmail = mUser.getEmail();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        //Fills the drawer with its variables
        drawerTitles = getResources().getStringArray(R.array.drawer_titles);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (ListView) findViewById(R.id.left_drawer);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        //Sets up the toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.pinfoToolbar);
        setSupportActionBar(myToolbar);


        //Used to place the users pin if they have input one
        Intent intent = getIntent();
        pinDescription = intent.getStringExtra("pinDescription");
        pinLocation = intent.getStringExtra("pinLocationTyped");
        pinCurrentLocation = intent.getBooleanExtra("pinCurrentLoc", false);

        if(pinDescription != null) {
                searchMarker(pinCurrentLocation, pinLocation , pinDescription);
        }

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

        // Set the adapter for the list view
        drawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_items, drawerTitles));

        // Set the list's click listener
        drawerList.setOnItemClickListener(new DrawerItemClickListener());
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

    public void putPin(LatLng pin, String place, String Message, String pinEmail){

        float bit = BitmapDescriptorFactory.HUE_ORANGE;

        if(currEmail.toLowerCase().equals(pinEmail.toLowerCase()))
        {
            bit = BitmapDescriptorFactory.HUE_BLUE;
        }


        if(!(pin.equals(null))){

            //Sets up the pin with its propertioes
            Marker marker = mMap.addMarker(
                    new MarkerOptions()
                            .position(pin)
                            .title(place)
                            .snippet(Message)
                            .icon(BitmapDescriptorFactory.defaultMarker(bit)));

            mMap.setInfoWindowAdapter(this);
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
                    String email = (String) data.child("Email").getValue();

                    LatLng lalo = new LatLng(Double.parseDouble(Lat), Double.parseDouble(Long));

                    putPin(lalo, place, message, email);
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
        values.put("Email", currEmail);
        postPinRef.push().setValue(values);

        /*//Write a message to the database
        DatabaseReference myRef = database.getReference("pins/" + placeName);
        myRef.setValue(pin);*/

    }

    public void searchMarker(boolean currentLoc, String locationTyped, String description){
        List<Address> addressList = null;
        Geocoder geocoder = new Geocoder(this);

        if(!currentLoc) {

            try {
                addressList = geocoder.getFromLocationName(locationTyped,1);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            Address address = addressList.get(0);
            LatLng ll = new LatLng(address.getLatitude(),address.getLongitude());
            savePin(ll, address.getFeatureName(), description);
            if(mMap != null){
                putPin(ll, address.getFeatureName(), description, currEmail);
            }
        }
        else {
            Double lat = getLat();
            Double lon = getLong();
            try {
                addressList = geocoder.getFromLocation(lat, lon, 1);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Address address = addressList.get(0);
            LatLng ll = new LatLng(lat,lon);
            savePin(ll, address.getFeatureName(), description);
            if(mMap != null){
                putPin(ll, address.getFeatureName(), description, currEmail);
            }
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

    @Override
    public View getInfoWindow(Marker marker) {

        //This creates the view of the custom infowindow
        View view = getLayoutInflater().inflate(R.layout.info_window, null, false);

        //The fields in the custom infowindow
        TextView place = (TextView) view.findViewById(R.id.layoutPlaceName);
        TextView desc = (TextView) view.findViewById(R.id.layoutDescription);

        //Then the fields are given the correct values
        place.setText(marker.getTitle());
        desc.setText(marker.getSnippet());

        return view;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }



    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    /** Swaps fragments in the main content view */
    private void selectItem(int position) {

        switch(position) {
            case 0:
                //User wants to go back to the map
                startActivity(new Intent(this, MapsActivity.class)); //Go back to the login page
                finish();
                break;
            case 1:
                //User wants to check their account information

                break;
            case 2:
                //User wants to add a new pin

                // Create a new fragment and specify which fragment to show based on position
                Fragment fragment = new PinFragment();
                Bundle args = new Bundle();
                args.putInt(PinFragment.ARG_MENU_ITEM_NUMBER, position);
                fragment.setArguments(args);

                findViewById(R.id.below_content_frame).setVisibility(View.GONE);

                // Insert the fragment by replacing any existing fragment
                FragmentManager fragmentManager = getFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.content_frame, fragment)
                        .commit();

                // Highlight the selected item, update the title, and close the drawer
                drawerList.setItemChecked(position, true);
                //setTitle(drawerTitles[position]);
                drawerLayout.closeDrawer(drawerList);

                break;
            case 3:
                //User wants to logout
                mAuth.signOut();
                startActivity(new Intent(MapsActivity.this, LoginPage.class)); //Go back to the login page
                finish();
                break;
        }

    }

    @Override
    public void setTitle(CharSequence title) {
        CharSequence mTitle = title;
        getActionBar().setTitle(mTitle);
    }

}