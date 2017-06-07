package com.howardhardy.pinfo.experimental;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;


/**
 * Created by Howard on 24/02/2017.
 */

public class LoginPage  extends FragmentActivity {

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private static final String TAG = "LoginPage";
    private String fullName;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    private boolean activePermission = false;
    private static final int REQUEST_CODE_ASK_PERMISSIONS = 7;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_page);

        //Tracking sign in and out
        mAuth = FirebaseAuth.getInstance();


        reqPermissions();

        //Checks the users login status
         mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }

        };

        //Lets the user login if they are authorised to do so
        final Button loginBtn = (Button) findViewById(R.id.loginBtn);
        loginBtn.setOnClickListener(new View.OnClickListener(){
            public void onClick (View v){

                //The credentials text boxes
                final EditText emailEdit = (EditText) findViewById(R.id.emailBox);
                final EditText passEdit = (EditText) findViewById(R.id.passBox);

                //Collects the values from the text boxes
                String emailString = emailEdit.getText().toString();
                String passString = passEdit.getText().toString();

                //ensures that the user has entered some data
                if(!(emailString.equals(null) || passString.equals(null))){
                    signIn(emailString, passString);
                }

            }
        });

        //Takes the user to the sign up page
        Button goToSignUpBtn = (Button) findViewById(R.id.goToSignUpBtn);
        goToSignUpBtn.setOnClickListener(new View.OnClickListener(){
            public void onClick (View v){
                Intent i = new Intent(new Intent(LoginPage.this, SignActivity.class));
                startActivityForResult(i,0);
            }
        });

    }

    //Creates the account by formating the data correctly the calling the create account method
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case 0:
                if(resultCode == Activity.RESULT_OK){
                    String d = data.getStringExtra("emailPass");
                    String[] credentials = new String[1];
                    credentials = d.split("  ");
                    this.createAccount(credentials[0], credentials[1]);

                    fullName =  data.getStringExtra("fullName");
                }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    //Creates the users account
    public void createAccount(String email, String password){
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "createUserWithEmail:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Toast.makeText(LoginPage.this, "Authentication failed,  please try again.",
                                    Toast.LENGTH_SHORT).show();
                        }
                        else{
                            Toast.makeText(LoginPage.this, "Authentication success.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    //Performs some error checking then lets the user login if they pass the checks
    public void signIn(final String email, String password) {
        final TextView err = (TextView) findViewById(R.id.err);
        if (email.equals("")) {err.setText("Please fill out the email field");}
        else {
            if (password.equals("")) {err.setText("Please fill out the password field");}
            else {
                mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithEmail:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithEmail", task.getException());
                            Toast.makeText(LoginPage.this, "Authentication failed, please try again.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(LoginPage.this, "Authentication success.",
                                    Toast.LENGTH_SHORT).show();

                            Intent i = new Intent(LoginPage.this, MapsActivity.class);
                            i.putExtra("userEmail", email);
                            i.putExtra("fullName", fullName);
                            startActivity(i);
                            setContentView(R.layout.activity_maps);
                        }

                    }
                });
            }
        }
    }


    private void reqPermissions() {
        //Checks to see if the permission has been granted
        int hasLocPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
        if (hasLocPermission != PackageManager.PERMISSION_GRANTED) {
            //Requests the permission
            this.requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_ASK_PERMISSIONS);
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
}
