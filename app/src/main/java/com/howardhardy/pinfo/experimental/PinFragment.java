package com.howardhardy.pinfo.experimental;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static com.howardhardy.pinfo.experimental.R.id.checkBox;

/**
 * Created by Howard on 20/03/2017.
 */

public class PinFragment extends Fragment {
    public static final String ARG_MENU_ITEM_NUMBER = "Galaxy_number";
    int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    String TAG = "PinFragment";
    Context fragContext;

    private EditText locTextView;
    private CharSequence locationAddress;

    public PinFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.input_pin, container, false);
        int i = getArguments().getInt(ARG_MENU_ITEM_NUMBER);
        String titles = getResources().getStringArray(R.array.drawer_titles)[i];
        fragContext = container.getContext();

        if(rootView != null){
            //Collecting all of the widgets
            final CheckBox locCheck = (CheckBox) rootView.findViewById(checkBox);
            locTextView = (EditText) rootView.findViewById(R.id.locEditText);
            final EditText descTextView = (EditText) rootView.findViewById(R.id.descEditText);
            final Button submitPin = (Button) rootView.findViewById(R.id.submitPin);
            final TextView errTextView = (TextView) rootView.findViewById(R.id.errTextView);
            final EditText titleTextView = (EditText) rootView.findViewById(R.id.titleEditText);

            MobileAds.initialize(rootView.getContext(), "ca-app-pub-4075831064006070/2065245942");

            AdView adView = (AdView) rootView.findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);

            //This will remove the default google search widget
            rootView.findViewById(R.id.place_autocomplete_fragment).setVisibility(View.GONE);

            //This will check to see if the user would rather use their currently location or one
            //they can type in
            locCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked){
                        //Disable the Location textview and make its value equal nothing
                        //The user has decided to use their current location
                        locTextView.setText(null);
                        locTextView.setInputType(0);
                    }
                    else {
                        locTextView.setInputType(1);
                    }
                }
            });

            //When the location editText has the focus launch the places fragment and then put the data back into the location editText (in the onActivityResult)
            locTextView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if(hasFocus){
                        try {
                            Intent intent =
                                    new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                                            .build(getActivity());
                            startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
                        } catch (GooglePlayServicesRepairableException e) {
                            // TODO: Handle the error.
                        } catch (GooglePlayServicesNotAvailableException e) {
                            // TODO: Handle the error.
                        }
                    }
                }
            });

            //Will allow you to post a pin if you pass the error checks
            submitPin.setOnClickListener(new View.OnClickListener(){
                public void onClick (View v){
                    if(locCheck.isChecked() || !( locTextView.getText().toString().equals("") ) ){
                        if(!(descTextView.getText().toString().equals(""))){
                            if(!titleTextView.getText().toString().equals("")){
                                //Gets the maps activity and passes in the pin information to be plotted on the map
                                Intent i = new Intent(getActivity(), MapsActivity.class);
                                i.putExtra("pinLocationTyped", locTextView.getText().toString());
                                i.putExtra("pinDescription", descTextView.getText().toString());
                                i.putExtra("pinCurrentLoc", locCheck.isChecked());
                                i.putExtra("pinTitle", titleTextView.getText().toString());
                                i.putExtra("pinAddressFull", locationAddress);
                                startActivity(i);
                            }
                            else {errTextView.setText("Please fill out the title field.");}
                        }
                        else {errTextView.setText("Please fill out the description field.");}
                    }
                    else {errTextView.setText("Please fill out the location field or click the check box to use your current location instead.");}
                }
            });
        }
        return rootView;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(fragContext, data);
                CharSequence placeName = place.getName();
                locationAddress = place.getAddress();
                locTextView.setText(placeName);

            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(fragContext, data);
                // TODO: Handle the error.
                Log.i(TAG, status.getStatusMessage());

            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }
    }
}