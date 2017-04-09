package com.howardhardy.pinfo;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.InfoWindowAdapter{

    private static final int REQUEST_CODE_ASK_PERMISSIONS = 7;
    private boolean activePermission = false;
    private GoogleMap mMap;

    //Live infow
    private MapWrapperLayout mapWrapperLayout;
    private OnInfoWindowElemTouchListener infoButtonListener;
    private ViewGroup infoWindow;
    private TextView infoTitle;
    private TextView infoMessage;
    private TextView infoRating;
    private Button infoUpButton;
    private Button infoDownButton;


    //Database Instance / Reference
    private static final String TAG = "MapsActivity";
    FirebaseDatabase database = FirebaseDatabase.getInstance();

    //The current users pin infomation if it has been input
    private String pinLocation;
    private Boolean pinCurrentLocation;
    private String pinDescription;
    private String pinAddressFull;

    //These are for the navigation drawer
    private String[] drawerTitles;
    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;

    private String currEmail;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

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
            //finish();
            return;
        }

        //If there is just continue setting up the main maps page
        currEmail = mUser.getEmail();

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
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Used to place the users pin if they have input one
        Intent intent = getIntent();
        pinDescription = intent.getStringExtra("pinDescription");
        pinLocation = intent.getStringExtra("pinLocationTyped");
        pinCurrentLocation = intent.getBooleanExtra("pinCurrentLoc", false);
        pinAddressFull = intent.getStringExtra("pinAddressFull");

        if(pinDescription != null) {
                searchMarker(pinCurrentLocation, pinLocation , pinDescription, pinAddressFull);
        }

        reqPermissions();

    }

    public static int getPixelsFromDp(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int)(dp * scale + 0.5f);
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



        //This code that follows is used to create an extension to infowindows,
        //not only making them custom but also allows for the live click of
        //buttons
        mapWrapperLayout = (MapWrapperLayout)findViewById(R.id.map_relative_layout);

        // MapWrapperLayout initialization
        // 39 - default marker height
        // 20 - offset between the default InfoWindow bottom edge and it's content bottom edge
        mapWrapperLayout.init(mMap, getPixelsFromDp(this, 39 + 20));

        // We want to reuse the info window for all the markers,
        // so let's create only one class member instance
        this.infoWindow = (ViewGroup)getLayoutInflater().inflate(R.layout.info_window, null);
        this.infoTitle = (TextView)infoWindow.findViewById(R.id.layoutPlaceName);
        this.infoMessage = (TextView)infoWindow.findViewById(R.id.layoutDescription);
        this.infoUpButton = (Button)infoWindow.findViewById(R.id.infoUpButton);
        this.infoDownButton = (Button)infoWindow.findViewById(R.id.infoDownButton);
        this.infoRating = (TextView)infoWindow.findViewById(R.id.layoutRating);

        //The up vote button lister
        this.infoButtonListener = new OnInfoWindowElemTouchListener(infoUpButton,
                getResources().getDrawable(R.drawable.btn_default_normal_holo_light),
                getResources().getDrawable(R.drawable.btn_default_pressed_holo_light))
        {
            @Override
            protected void onClickConfirmed(View v, Marker marker) {
                // Here we can perform some action triggered after clicking the button
                Toast.makeText(MapsActivity.this, "Up!", Toast.LENGTH_SHORT).show();
                marker = mapWrapperLayout.getMarker();
                votePin(marker, 1);
                String s = "Test";
                //getInfoWindow(marker);
                /*    if(successful.equals("true")){
                        String oldR = (String) marker.getTag();
                        int newR = Integer.getInteger(oldR);
                        newR++;
                        marker.setTag(String.valueOf(newR));
                        infoRating.setText(marker.getTag().toString());
                        mapWrapperLayout.setMarkerWithInfoWindow(marker, infoWindow);
                        //Remove/readdPin
                        //marker.remove();
                        //putPin();

                    }
                    else if(successful.equals("updated")){
                        String oldR = (String) marker.getTag();
                        int newR = Integer.getInteger(oldR);
                        newR--;
                        marker.setTag(String.valueOf(newR));
                        infoRating.setText(marker.getTag().toString());
                        mapWrapperLayout.setMarkerWithInfoWindow(marker, infoWindow);

                    }
                */
                return;
            }
        };
        this.infoUpButton.setOnTouchListener(infoButtonListener);

        //The down vote button lister
        this.infoButtonListener = new OnInfoWindowElemTouchListener(infoDownButton,
                getResources().getDrawable(R.drawable.btn_default_normal_holo_light),
                getResources().getDrawable(R.drawable.btn_default_pressed_holo_light))
        {
            @Override
            protected void onClickConfirmed(View v, Marker marker) {
                // Here we can perform some action triggered after clicking the button
                Toast.makeText(MapsActivity.this, "Down!", Toast.LENGTH_SHORT).show();
                marker = mapWrapperLayout.getMarker();
                votePin(marker, -1);

                return;
            }
        };
        this.infoDownButton.setOnTouchListener(infoButtonListener);
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

    public void putPin(LatLng pin, String place, String Message, String pinEmail, String rating){

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

            marker.setTag(rating);

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
                    String rating = (String) data.child("Rating").getValue();

                    LatLng lalo = new LatLng(Double.parseDouble(Lat), Double.parseDouble(Long));

                    putPin(lalo, place, message, email, rating);
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

    private void votePin(final Marker marker, final int value) {
        final DatabaseReference postPinRatingRef = database.getReference().child("pins");

        final String thePlace = marker.getTitle();
        final String theMessage = marker.getSnippet();

        postPinRatingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot){

                for(DataSnapshot data : dataSnapshot.getChildren()){
                    String message = (String) data.child("Message").getValue();
                    String place = (String) data.child("Place").getValue();
                    String rating = (String) data.child("Rating").getValue();
                    String currentPinKey = data.getKey();

                    //Checks if the user has already voted on this pin (whether it be up or down respectively)
                    String textVal = Integer.toString(value);


                    //Takes all of the pins and if they have the same email as our current user they are added to his account infomation
                    if(message.equals(theMessage) && place.equals(thePlace) ) {

                        float newRating = Float.parseFloat(rating) + value;
                        String textRating = Float.toString(newRating);
                        addVoteIfNotDuplicate(textVal,currentPinKey,value,textRating, marker);
                        //postPinRatingRef.limitToFirst(1);

                        //Make changes to the vote table to flag that the user has voted
                        //addUsersVotes(value,currentPinKey);
                    }

                }

            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }

        });
    }

    private void addVoteIfNotDuplicate(final String theVote, final String pinKey, final int value, final String textRating, final
        Marker marker) {


        final DatabaseReference readVoteRef = database.getReference().child("vote");
        final DatabaseReference addVoteRef = database.getReference().child("pins");
        final DatabaseReference voteFlag = database.getReference().child("flag").child("Status");
        readVoteRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot){

                voteFlag.setValue("hasNot");
                for(DataSnapshot data : dataSnapshot.getChildren()){
                    String email = (String) data.child("Email").getValue();
                    String vote = (String) data.child("Vote").getValue();
                    String pin = (String) data.child("Pin").getValue();


                    if(email.equals(currEmail) && pin.equals(pinKey)) {
                        if(vote.equals(theVote)){
                            voteFlag.setValue("voted");
                        }
                        else
                        {
                            voteFlag.setValue("update");
                        }

                    }

                }

                if(!dataSnapshot.hasChild("Email")){
                    readVoteRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            voteFlag.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(final DataSnapshot dataSnapshot) {
                                    final String flag = dataSnapshot.getValue(String.class);

                                        addVoteRef.child(pinKey).child("Rating").addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot snapshot) {

                                                if(flag.equals("hasNot")){
                                                    addUsersVotes(value,pinKey);

                                                    String originalRating = (String) snapshot.getValue();
                                                    float val = Float.valueOf(originalRating);
                                                    val = val + Float.parseFloat(theVote);
                                                    String textRat= Float.toString(val);

                                                    addVoteRef.child(pinKey).child("Rating").setValue(textRat);
                                                    marker.setTag(textRating);
                                                    infoRating.setText(marker.getTag().toString());
                                                    marker.hideInfoWindow();
                                                    marker.showInfoWindow();
                                                }
                                                else if(flag.equals("update")){
                                                    updateUsersVote(value,pinKey);
                                                    addVoteRef.child(pinKey).child("Rating").setValue(textRating);

                                                    float opposite = Float.valueOf((String) snapshot.getValue());
                                                    opposite = opposite + Float.parseFloat(theVote);
                                                    String textOppositeRating = Float.toString(opposite);

                                                    marker.setTag(textOppositeRating);
                                                    infoRating.setText(marker.getTag().toString());
                                                    marker.hideInfoWindow();
                                                    marker.showInfoWindow();
                                                }
                                                else if(flag.equals("voted")){
                                                    voteFlag.setValue("on");
                                                }
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {

                                            }
                                        });
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });



                        }
                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }
                else
                {
                    addUsersVotes(1,pinKey);
                    addVoteRef.child(pinKey).child("Rating").setValue(textRating);
                    marker.setTag(textRating);
                    //infoRating.setText(marker.getTag().toString());
                    marker.hideInfoWindow();
                    marker.showInfoWindow();
                }


            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }

        });
    }

    private void addUsersVotes(int vote, String pinKey){
        DatabaseReference postVoteRef = database.getReference().child("vote");
        Map<String, String> values = new HashMap<String, String>();
        //Putting the values into the hashmap to then be put into the database, with push. Giving a unique id
        values.put("Email", currEmail);
        values.put("Vote", String.valueOf(vote));
        values.put("Pin", pinKey);
        postVoteRef.push().setValue(values);
    }

    private void updateUsersVote(final int vote, final String pinKey){
        final DatabaseReference postUpdatedVoteRef = database.getReference().child("vote");

        postUpdatedVoteRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    String email = (String) data.child("Email").getValue();
                    String pin = (String) data.child("Pin").getValue();
                    String key = (String) data.getKey();

                    if(email.equals(currEmail) && pin.equals(pinKey)) {
                        postUpdatedVoteRef.child(key).child("Vote").setValue(String.valueOf(vote));
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }


    private List<String> getCurrentUsersPins() {

        final List<String> usersPins = new ArrayList<String>();

        DatabaseReference readPinRef = database.getReference().child("pins");
        readPinRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot){

                for(DataSnapshot data : dataSnapshot.getChildren()){
                    String rating = (String) data.child("Rating").getValue();
                    String message = (String) data.child("Message").getValue();
                    String place = (String) data.child("Place").getValue();
                    String email = (String) data.child("Email").getValue();

                    //Takes all of the pins and if they have the same email as our current user they are added to his account infomation
                    if(email.equals(currEmail)) {
                        usersPins.add("Place name: " + place + "  \n" + "Message: " + message + "  \n" + "Rating: " + rating);
                    }

                }

            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }

        });

        return usersPins;
    }


    public void searchMarker(boolean currentLoc, String locationTyped, String description, String pinAddressFull){
        List<Address> addressList = null;
        Geocoder geocoder = new Geocoder(this);

        if(!currentLoc) {

            try {
                addressList = geocoder.getFromLocationName(pinAddressFull,1);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            Address address = addressList.get(0);
            LatLng ll = new LatLng(address.getLatitude(),address.getLongitude());
            savePin(ll, address.getFeatureName(), description);
            if(mMap != null){
                putPin(ll, address.getFeatureName(), description, currEmail, "1");
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
                putPin(ll, address.getFeatureName(), description, currEmail, "1");
            }
        }

    }

    public Double getLat() {
        LocationManager lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null) {
                return location.getLatitude();
            }
        }
        return null;
    }

    public Double getLong() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null) {
                return location.getLongitude();
            }
        }
        return null;
    }

    @Override
    public View getInfoWindow(final Marker marker) {

        //This creates the view of the custom infowindow
        //View view = getLayoutInflater().inflate(R.layout.info_window, null, false);

        //Nessisary for our live infowindow
        //Context c = this.getApplicationContext();


        //The fields in the custom infowindow
        //TextView place = (TextView) view.findViewById(R.id.layoutPlaceName);
        //TextView desc = (TextView) view.findViewById(R.id.layoutDescription);

        //Then the fields are given the correct values

        final String theTitle = marker.getTitle();
        final String theMessage = marker.getSnippet();

        //updateRatingOfInfoWindow(theTitle, theMessage);

        //infoRating.setTag("Added");

        infoTitle.setText(theTitle);
        infoMessage.setText(theMessage);
        infoRating.setText(marker.getTag().toString());

        //infoRating.setText(marker.get);
        //layoutRating.setText();
        infoButtonListener.setMarker(marker);

        mapWrapperLayout.setMarkerWithInfoWindow(marker, infoWindow);


        return infoWindow;
    }

    private void updateRatingOfInfoWindow(final String theTitle, final String theMessage) {
        DatabaseReference readPinRef = database.getReference().child("pins");
        readPinRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot){

                for(DataSnapshot data : dataSnapshot.getChildren()){
                    String rating = (String) data.child("Rating").getValue();
                    String message = (String) data.child("Message").getValue();
                    String place = (String) data.child("Place").getValue();

                    //Takes all of the pins and if they have the same email as our current user they are added to his account infomation
                    if(message.equals(theMessage) && place.equals(theTitle)) {
                        infoRating.setText(rating);
                        //infoRating.setTag("Added");
                        return;
                    }

                }

            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }

        });
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

        //Common for the fragments that you can switch to
        Fragment fragment;
        Bundle args = new Bundle();
        FragmentManager fragmentManager;

        switch(position) {
            case 0:
                //User wants to go back to the map
                startActivity(new Intent(this, MapsActivity.class)); //Go back to the login page
                finish();
                break;
            case 1:
                //User wants to check their account information
                // Create a new fragment and specify which fragment to show based on position
                fragment = new UserProfileFragment();

                //The menu item number
                args.putInt(PinFragment.ARG_MENU_ITEM_NUMBER, position);

                //Getting all the users pins and infomation
                List<String> usersPins = getCurrentUsersPins();
                args.putStringArrayList("list", (ArrayList<String>) usersPins);
                args.putString("email",currEmail);
                fragment.setArguments(args);

                //removes the map fragment
                findViewById(R.id.below_content_frame).setVisibility(View.GONE);

                // Insert the fragment by replacing any existing fragment
                fragmentManager = getFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.content_frame, fragment)
                        .commit();

                // Highlight the selected item, update the title, and close the drawer
                drawerList.setItemChecked(position, true);
                //setTitle(drawerTitles[position]);
                drawerLayout.closeDrawer(drawerList);
                break;
            case 2:
                //User wants to add a new pin

                // Create a new fragment and specify which fragment to show based on position
                fragment = new PinFragment();

                //The menu item number
                args.putInt(PinFragment.ARG_MENU_ITEM_NUMBER, position);
                fragment.setArguments(args);

                //removes the map fragment
                findViewById(R.id.below_content_frame).setVisibility(View.GONE);

                // Insert the fragment by replacing any existing fragment
                fragmentManager = getFragmentManager();
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