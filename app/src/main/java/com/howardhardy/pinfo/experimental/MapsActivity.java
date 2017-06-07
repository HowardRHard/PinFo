package com.howardhardy.pinfo.experimental;

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
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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

import static bolts.Task.delay;

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
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;

    //The current users pin infomation if it has been input
    private String pinLocation;
    private Boolean pinCurrentLocation;
    private String pinDescription;
    private String pinTitle;
    private String pinAddressFull;

    private String fullName;

    //These are for the navigation drawer
    private String[] drawerTitles;
    private DrawerLayout drawerLayout;
    private ListView drawerList;

    //Needed for getting a persons score as well as their pins
    private List<ArrayList<String>> arrayListOfLists;

    //Current user logged in
    private String currEmail;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        //Gets the instance of the current user logged in
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        arrayListOfLists = getCurrentUsersPins();

        try{if(mUser.equals(null)){}}
        catch(NullPointerException n){
            //If there is no user then switch to the login
            //User not Signed in, provide them the opportunity to
            startActivity(new Intent(MapsActivity.this, LoginPage.class)); //Go to login page
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
        myToolbar.setTitle(R.string.title_activity_maps);

        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, myToolbar, R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerClosed(View v){
                super.onDrawerClosed(v);
                invalidateOptionsMenu();
                syncState();
            }
            public void onDrawerOpened(View v){
                super.onDrawerOpened(v);
                invalidateOptionsMenu();
                syncState();
            }
        };

        drawerLayout.setDrawerListener(drawerToggle);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        drawerToggle.syncState();



        //Used to place the users pin if they have input one
        Intent intent = getIntent();
        pinDescription = intent.getStringExtra("pinDescription");
        pinTitle = intent.getStringExtra("pinTitle");
        pinLocation = intent.getStringExtra("pinLocationTyped");
        pinCurrentLocation = intent.getBooleanExtra("pinCurrentLoc", false);
        pinAddressFull = intent.getStringExtra("pinAddressFull");

        //Collects the users name used for the profile page
        fullName = intent.getStringExtra("fullName");

        //Adds the user to the database
        if(fullName != null)
            addUser(currEmail, fullName);

        //If the user has made a new pin, then make sure to add it
        if(pinDescription != null)
                searchMarker(pinCurrentLocation, pinLocation , pinDescription, pinTitle, pinAddressFull);

        //Request permissions for the users location data
        reqPermissions();

    }

    @Override
    //Used to Open and close the drawer
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case android.R.id.home: {
                if(drawerLayout.isDrawerOpen(drawerList))
                    drawerLayout.closeDrawer(drawerList);
                else
                    drawerLayout.openDrawer(drawerList);
                return true;
            }
            default: return super.onOptionsItemSelected(item);
        }
    }

    public static int getPixelsFromDp(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int)(dp * scale + 0.5f);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //If the user has the permissions, set up the map
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            //Get the users location and then zoom in closer to it
            mMap.setMyLocationEnabled(true);
            Double[] ll = new Double[]{getLat(),getLong()};
            LatLng lalo = new LatLng(ll[0], ll[1]);

            if(!(ll[0].toString().equals(null) || ll[1].toString().equals(null)))
            {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lalo, 16));
            }

            //Get the pins from the database and put them on the map
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

       //enables the reuse of the infowindow
        this.infoWindow = (ViewGroup)getLayoutInflater().inflate(R.layout.info_window, null);
        this.infoTitle = (TextView)infoWindow.findViewById(R.id.layoutPlaceName);
        this.infoMessage = (TextView)infoWindow.findViewById(R.id.layoutDescription);
        this.infoUpButton = (Button)infoWindow.findViewById(R.id.infoUpButton);
        this.infoDownButton = (Button)infoWindow.findViewById(R.id.infoDownButton);
        this.infoRating = (TextView)infoWindow.findViewById(R.id.layoutRating);

        //The up vote button lister
        this.infoButtonListener = new OnInfoWindowElemTouchListener(infoUpButton)
        {
            @Override
            protected void onClickConfirmed(View v, Marker marker) {
                Toast.makeText(MapsActivity.this, "+1", Toast.LENGTH_SHORT).show();
                marker = mapWrapperLayout.getMarker();
                votePin(marker, 1);
                return;
            }
        };
        this.infoUpButton.setOnTouchListener(infoButtonListener);

        //The down vote button lister
        this.infoButtonListener = new OnInfoWindowElemTouchListener(infoDownButton)
        {
            @Override
            protected void onClickConfirmed(View v, Marker marker) {
                Toast.makeText(MapsActivity.this, "-1", Toast.LENGTH_SHORT).show();
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

    //Puts a pin down on the map
    public void putPin(LatLng pin, String place, String Message, String pinEmail, String rating){
        //Default colour for the pin
        float bit = BitmapDescriptorFactory.HUE_ORANGE;

        //Which then changes if it is owned by the current user logged in
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

    //Gathers all the pins from the database then places them
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

    //Saves an individual pin
    public void savePin(LatLng pin, String placeName, String message){

        DatabaseReference savePinRef = database.getReference().child("pins");
        Map<String, String> values = new HashMap<String, String>();

        //Putting the values into the hashmap to then be put into the database, with push. Giving a unique id
        values.put("Place", placeName);
        values.put("Latitude", String.valueOf(pin.latitude));
        values.put("Longitude", String.valueOf(pin.longitude));
        values.put("Message", message);
        values.put("Rating", String.valueOf(1.0));
        values.put("Email", currEmail);
        savePinRef.push().setValue(values);
    }

    //Lets a user vote on a pin if they are allowed to
    private void votePin(final Marker marker, final int value) {
        final DatabaseReference votePin = database.getReference().child("pins");

        final String thePlace = marker.getTitle();
        final String theMessage = marker.getSnippet();

        votePin.addListenerForSingleValueEvent(new ValueEventListener() {
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

    //After some checks, this method makes a few more then adds the vote if it is allowed
    private void addVoteIfNotDuplicate(final String theVote, final String pinKey, final int value, final String textRating, final Marker marker) {
        final DatabaseReference readVoteRef = database.getReference().child("vote");
        final DatabaseReference addVoteRef = database.getReference().child("pins");
        final DatabaseReference voteFlag = database.getReference().child("flag").child("Status");

        readVoteRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            //Sets the vote flag
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
                                                //If the user hasn't voted before then let them vote
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

                                                    //Attempt at auto deletion

                                                    float valRatingFloat = Float.valueOf(textRating);
                                                    if(valRatingFloat >= -5) {
                                                        marker.hideInfoWindow();
                                                        marker.setVisible(false);
                                                        removePin(pinKey);
                                                        marker.remove();
                                                    }
                                                }
                                                //If they are voting the opposite to what they voted before then let them vote
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

                                                    //Attempt at auto deletion

                                                    float valRatingFloat = Float.valueOf(textRating);
                                                    if(valRatingFloat >= -5) {
                                                        marker.hideInfoWindow();
                                                        marker.setVisible(false);
                                                        removePin(pinKey);
                                                        marker.remove();
                                                    }
                                                }
                                                //If they are trying to revote then don't allow them to
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
                //If the vote table hasn't even been made yet (i.e. first ever vote)
                //Then make it and let the user vote
                else {
                    addUsersVotes(1, pinKey);
                    addVoteRef.child(pinKey).child("Rating").setValue(textRating);
                    marker.setTag(textRating);
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

    //Log a users vote on a specific pin
    private void addUsersVotes(int vote, String pinKey){
        DatabaseReference addUserVoteRef = database.getReference().child("vote");
        Map<String, String> values = new HashMap<String, String>();

        values.put("Email", currEmail);
        values.put("Vote", String.valueOf(vote));
        values.put("Pin", pinKey);
        addUserVoteRef.push().setValue(values);
    }

    //Adds a new user to the system
    private void addUser(String email, String fullName){
        DatabaseReference addUserRef = database.getReference().child("user");
        Map<String, String> values = new HashMap<String, String>();

        values.put("Email", email);
        values.put("Name", fullName);
        addUserRef.push().setValue(values);
    }

    //Change a users vote to the opposite vote
    private void updateUsersVote(final int vote, final String pinKey){
        final DatabaseReference postUpdatedVoteRef = database.getReference().child("vote");

        postUpdatedVoteRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                //Gets the right vote then updates it
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

    //This was my attempt at removing a pin, used in the auto delete part of the app

    private void removePin(final String key){
        final DatabaseReference removePinRef = database.getReference().child("pins");

        removePinRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                removePinRef.child(key).removeValue();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

    }

    //Gets the users pins as well as their overall score
    private List<ArrayList<String>> getCurrentUsersPins() {

        final List<String> usersPins = new ArrayList<String>();
        final List<String> totalA = new ArrayList<String>();
        final List<ArrayList<String>> arrayListOfLists = new ArrayList<ArrayList<String>>();

        DatabaseReference readPinRef = database.getReference().child("pins");
        readPinRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot){
                //Initial value of the score, in case the user has no pins
                Float total = Float.valueOf("0");

                for(DataSnapshot data : dataSnapshot.getChildren()){
                    String rating = (String) data.child("Rating").getValue();
                    String message = (String) data.child("Message").getValue();
                    String place = (String) data.child("Place").getValue();
                    String email = (String) data.child("Email").getValue();

                    //Takes all of the pins and if they have the same email as our current user they are added to his account infomation
                    if(email.equals(currEmail)) {
                        //Adds the pins to the array as well as tallying up the score
                        usersPins.add("Place name: " + place + "  \n" + "Message: " + message + "  \n" + "Rating: " + rating);
                        total = total + Float.valueOf(rating);
                    }

                }
                    //If the user has a score, then set it
                    if(total != null){
                        String s = String.valueOf(total);
                        totalA.add(s);
                    }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }

        });

        arrayListOfLists.add((ArrayList<String>)usersPins);
        arrayListOfLists.add((ArrayList<String>)totalA);

        return arrayListOfLists;
    }

    //The geocoding process that turns a user pin into Lat and Long coordinates
    public void searchMarker(boolean currentLoc, String locationTyped, String description, String pinTitle, String pinAddressFull){
        List<Address> addressList = null;
        Geocoder geocoder = new Geocoder(this);

        //If the user hasn't used their current location
        if(!currentLoc) {
            try {
                addressList = geocoder.getFromLocationName(pinAddressFull,1);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            //Puts the pin on the map and saves it
            Address address = addressList.get(0);
            LatLng ll = new LatLng(address.getLatitude(),address.getLongitude());
            savePin(ll, pinTitle, description);
            if(mMap != null){
                putPin(ll, pinTitle, description, currEmail, "1");
            }
        }

        //If they have
        else {
            Double lat = getLat();
            Double lon = getLong();
            try {
                addressList = geocoder.getFromLocation(lat, lon, 1);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //Puts the pin on the map and saves it
            Address address = addressList.get(0);
            LatLng ll = new LatLng(lat,lon);
            savePin(ll, pinTitle, description);
            if(mMap != null){
                putPin(ll, pinTitle, description, currEmail, "1");
            }
        }

    }

    //Gets the latitude
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

    //Gets the longitude
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
    //Sets the custom infowindow
    public View getInfoWindow(final Marker marker) {

        final String theTitle = marker.getTitle();
        final String theMessage = marker.getSnippet();

        infoTitle.setText(theTitle);
        infoMessage.setText(theMessage);
        String temp = marker.getTag().toString();
        infoRating.setText(temp.substring(0, temp.indexOf(".")));

        infoButtonListener.setMarker(marker);

        mapWrapperLayout.setMarkerWithInfoWindow(marker, infoWindow);

        return infoWindow;
    }

    //Sets the users name to the action bar when you change to the user account page
    private void setUsersFullName(final String currEmail) {
        DatabaseReference readUsersNameRef = database.getReference().child("user");
        readUsersNameRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot){

                for(DataSnapshot data : dataSnapshot.getChildren()){
                    String email = (String) data.child("Email").getValue();
                    String fullName = (String) data.child("Name").getValue();

                    //Takes all of the pins and if they have the same email as our current user they are added to his account infomation
                    if(email.equals(currEmail)) {
                        getSupportActionBar().setTitle(fullName);
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
                getSupportActionBar().setTitle("Map");
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
                args.putStringArrayList("list", (ArrayList<String>) arrayListOfLists.get(0));
                args.putStringArrayList("total", (ArrayList<String>) arrayListOfLists.get(1));

                fragment.setArguments(args);

                // Insert the fragment by replacing any existing fragment
                fragmentManager = getFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.map, fragment)
                        .commit();

                // Highlight the selected item, update the title, and close the drawer
                drawerList.setItemChecked(position, true);
                //setTitle(drawerTitles[position]);
                drawerLayout.closeDrawer(drawerList);
                setUsersFullName(currEmail);
                break;
            case 2:
                //User wants to add a new pin

                // Create a new fragment and specify which fragment to show based on position
                fragment = new PinFragment();

                //The menu item number
                args.putInt(PinFragment.ARG_MENU_ITEM_NUMBER, position);
                fragment.setArguments(args);

                //removes the map fragment
                //findViewById(R.id.below_content_frame).setVisibility(View.GONE);

                // Insert the fragment by replacing any existing fragment
                fragmentManager = getFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.map, fragment)
                        .commit();

                // Highlight the selected item, update the title, and close the drawer
                drawerList.setItemChecked(position, true);
                //setTitle(drawerTitles[position]);
                drawerLayout.closeDrawer(drawerList);
                getSupportActionBar().setTitle("New Pin");
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