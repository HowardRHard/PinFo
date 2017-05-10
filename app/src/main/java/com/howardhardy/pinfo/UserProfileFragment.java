package com.howardhardy.pinfo;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Howard on 26/03/2017.
 */

public class UserProfileFragment extends Fragment {

    private ListView userPinLv;
    private ListView BadgesLv;
    private TextView usersTotal;

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

            //Gets the pins and total score
            List<String> userPins = getArguments().getStringArrayList("list");
            List<String> thetotal = getArguments().getStringArrayList("total");

            //If there is not total then it is 0
            String tot = "0";
            if(thetotal.size()>0){tot = thetotal.get(0);}

            //Sets up the users pins being displayed
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
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
        }
        return rootView;
    }
}
