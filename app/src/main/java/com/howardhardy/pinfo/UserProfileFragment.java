package com.howardhardy.pinfo;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;

import java.util.ArrayList;
import java.util.List;

import static com.howardhardy.pinfo.PinFragment.ARG_MENU_ITEM_NUMBER;
import static com.howardhardy.pinfo.R.id.checkBox;

/**
 * Created by Howard on 26/03/2017.
 */

public class UserProfileFragment extends Fragment {

    private ListView userPinLv;
    private TextView usersName;

    public UserProfileFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.user_profile, container, false);

        if(rootView != null){
            userPinLv = (ListView) rootView.findViewById(R.id.pinList);
            usersName = (TextView) rootView.findViewById(R.id.userEmailTV);

            usersName.setText(getArguments().getString("email"));
            List<String> userPins = getArguments().getStringArrayList("list");
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                    this.getActivity(),
                    android.R.layout.simple_list_item_1,
                    userPins );

            userPinLv.setAdapter(arrayAdapter);

        }

        return rootView;
    }

}
