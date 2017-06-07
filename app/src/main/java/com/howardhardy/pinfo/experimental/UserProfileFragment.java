package com.howardhardy.pinfo.experimental;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Howard on 26/03/2017.
 */

public class UserProfileFragment extends Fragment {

    private ListView userPinLv;
    private ListView BadgesLv;
    private TextView usersTotal;
    private Switch switchDelUpd;

    //These are for the navigation drawer
    private String[] drawerTitles;
    private DrawerLayout drawerLayout;
    private ListView drawerList;
    FirebaseDatabase database = FirebaseDatabase.getInstance();

    public UserProfileFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.user_profile, container, false);

        if (rootView != null) {
            //Sets up the items on the page
            usersTotal = (TextView) rootView.findViewById(R.id.userTotalVotesTV);
            userPinLv = (ListView) rootView.findViewById(R.id.pinList);
            BadgesLv = (ListView) rootView.findViewById(R.id.listView);
            switchDelUpd = (Switch) rootView.findViewById(R.id.switchDelUpd);

            //Gets the pins and total score
            final List<String> userPins = getArguments().getStringArrayList("list");
            List<String> thetotal = getArguments().getStringArrayList("total");

            //If there is not total then it is 0
            String tot = "0";
            if(thetotal.size()>0){tot = thetotal.get(0);}

            //Sets up the users pins being displayed
            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                    this.getActivity(),
                    android.R.layout.simple_list_item_1,
                    userPins);

            //Here to show what the badges may be like, but as of yet there are none
            List<String> demo = new ArrayList<String>();
            demo.add("Badges (To be implemented)");
            ArrayAdapter<String> arrayAdapterdemo = new ArrayAdapter<String>(
                    this.getActivity(),
                    android.R.layout.simple_list_item_1,
                    demo);

            userPinLv.setAdapter(arrayAdapter);
            BadgesLv.setAdapter(arrayAdapterdemo);

            //Takes the .0 off the end, from it being a float
            usersTotal.setText(tot.substring(0, tot.indexOf(".")));

            switchDelUpd.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(switchDelUpd.getText().equals("Delete Selected  ")){
                        switchDelUpd.setText("Update Selected  ");

                    }
                    else{
                        switchDelUpd.setText("Delete Selected  ");
                    }
                }
            });

            userPinLv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                               final int pos, long id) {

                    String alertColour = "#EE0000";
                    String option = (String) switchDelUpd.getText();
                    if(option.equals("Update Selected  ")){alertColour = "#E6B300";}

                    //The alert box
                    //It will either delete or update a pin depending on what value the switch is on
                    new AlertDialog.Builder(getContext())
                            .setTitle(Html.fromHtml("<font color='"+alertColour+"'>" + option.substring(0,6) + " Pin</font>"))
                            .setMessage(Html.fromHtml("<font color='"+alertColour+"'>Are you sure you want to " + option.substring(0,6) + " this pin?</font>"))
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    //Toast.makeText(getContext(), String.valueOf(pos), Toast.LENGTH_SHORT).show();
                                    if(switchDelUpd.getText().equals("Delete Selected  ")){
                                        removePin((String) userPinLv.getItemAtPosition(pos));
                                    }
                                    else{
                                        Bundle args = new Bundle();
                                        FragmentManager fragmentManager;
                                        int position = 2;

                                        Fragment fragment = new PinFragment();

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
                                    }

                                }
                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // do nothing
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                            arrayAdapter.notifyDataSetChanged();

                    return true;
                }
            });

        }
        return rootView;
    }


    //This was my attempt at removing a pin, used in the auto delete part of the app

    private void removePin(final String details){
        final DatabaseReference removePinRef = database.getReference().child("pins");

        removePinRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot data : dataSnapshot.getChildren()){
                    String message = (String) data.child("Message").getValue();
                    String place = (String) data.child("Place").getValue();
                    String rating = (String) data.child("Rating").getValue();
                    String currentPinKey = data.getKey();

                    if(details.contains(message) && details.contains(place) && details.contains(rating))
                        removePinRef.child(currentPinKey).removeValue();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

    }
}
