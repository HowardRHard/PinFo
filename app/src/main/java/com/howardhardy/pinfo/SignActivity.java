package com.howardhardy.pinfo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Created by Howard on 06/03/2017.
 */

public class SignActivity extends FragmentActivity {

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private static final String TAG = "SignPage";
    private FirebaseUser user;
    private final LoginPage loginPage = null;

    public SignActivity(){
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup_page);

        //Tracking sign in and outo
        mAuth = FirebaseAuth.getInstance();

        Button backBtn = (Button) findViewById(R.id.backBtn);
        backBtn.setOnClickListener(new View.OnClickListener(){
            public void onClick (View v) {
                Intent i = new Intent(SignActivity.this, LoginPage.class);
                startActivity(i);
                setContentView(R.layout.login_page);
            }
        });

        final Button signUpBtn = (Button) findViewById(R.id.signUpBtn);
        signUpBtn.setOnClickListener(new View.OnClickListener(){
            public void onClick (View v) {

                //Collects the both of the users entered emails, passwords and creates the variable used to change the error label
                EditText emailEdit = (EditText) findViewById(R.id.emailTop);
                String emailT = emailEdit.getText().toString();
                emailEdit = (EditText) findViewById(R.id.emailBottom);
                String emailB = emailEdit.getText().toString();
                EditText passEdit = (EditText) findViewById(R.id.passTop);
                String passT = passEdit.getText().toString();
                passEdit = (EditText) findViewById(R.id.passBottom);
                String passB = passEdit.getText().toString();
                TextView error;


                //Compares the emails, if correct will proceed to the passwords. If they are correct then the account is created, if not then the error message will appear
                if(emailT.equals(emailB)) {
                    if(passT.equals(passB)) {
                        Intent i = new Intent();
                        i.putExtra("emailPass", emailT + "  " + passT);
                        setResult(RESULT_OK,i);
                        finish();
                        //setContentView(R.layout.login_page);
                    } else if (!(passT.equals("")) || !(passB.equals(""))){
                        error = (TextView) findViewById(R.id.error_msg);
                        error.setText("Please make sure you don't leave your password blank.");
                    } else {
                        error = (TextView) findViewById(R.id.error_msg);
                        error.setText("Please make sure your passwords match.");
                    }
                } else if (!(passT.equals(passB)) || !(emailT.equals(emailB))){
                    error = (TextView) findViewById(R.id.error_msg);
                    error.setText("Please make sure your emails and passwords match.");
                } else if (!(emailT.equals("")) && !(emailB.equals(""))){
                    error = (TextView) findViewById(R.id.error_msg);
                    error.setText("Please make sure you don't leave your email blank.");
                } else {
                    error = (TextView) findViewById(R.id.error_msg);
                    error.setText("Please make sure your emails match.");
                }

            }
        });

    }



};