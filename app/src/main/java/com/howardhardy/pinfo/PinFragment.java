package com.howardhardy.pinfo;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.text.Text;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static com.howardhardy.pinfo.R.id.auto;
import static com.howardhardy.pinfo.R.id.checkBox;
import static com.howardhardy.pinfo.R.id.none;

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

            submitPin.setOnClickListener(new View.OnClickListener(){
                public void onClick (View v){
                    if(locCheck.isChecked() || !( locTextView.getText().toString().equals("") ) ){
                        if(!(descTextView.getText().toString().equals(""))){
                            //Gets the maps activity and passes in the pin information to be plotted on the map
                            Intent i = new Intent(getActivity(), MapsActivity.class);
                            i.putExtra("pinLocationTyped", locTextView.getText().toString());
                            i.putExtra("pinDescription", descTextView.getText().toString());
                            i.putExtra("pinCurrentLoc", locCheck.isChecked());
                            i.putExtra("pinAddressFull", locationAddress);
                            startActivity(i);
                        }
                        else {
                            errTextView.setText("Please fill out the description field.");
                        }
                    }
                    else {
                        errTextView.setText("Please fill out the location field or click the check box to use your current location instead.");
                    }
                }
            });
        }
        //getActivity().setTitle(Galaxy);

        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(fragContext, data);
                //AutocompletePrediction acp = new AutocompletePrediction() {
                //}
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