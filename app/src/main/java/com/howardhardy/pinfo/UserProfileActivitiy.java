package com.howardhardy.pinfo;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;

/**
 * Created by Howard on 26/03/2017.
 */

public class UserProfileActivitiy extends Activity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_profile);

        ListView pinList = (ListView) findViewById(R.id.pinList);
    }


}
