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
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Created by Howard on 24/02/2017.
 */

public class LoginPage  extends FragmentActivity {

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private static final String TAG = "LoginPage";
    private FirebaseUser user;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_page);
        //Tracking sign in and outo
        mAuth = FirebaseAuth.getInstance();

         mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                    //setContentView(R.layout.activity_maps);
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                    //setContentView(R.layout.login_page);
                }
            }
        };

        final Button loginBtn = (Button) findViewById(R.id.loginBtn);
        loginBtn.setOnClickListener(new View.OnClickListener(){
            public void onClick (View v){
               // Toast.makeText(LoginPage.this, "Login here.",
               //         Toast.LENGTH_SHORT).show();

                final EditText emailEdit = (EditText) findViewById(R.id.emailBox);
                String emailString = emailEdit.getText().toString();

                final EditText passEdit = (EditText) findViewById(R.id.passBox);
                String passString = passEdit.getText().toString();

                signIn(emailString, passString);
            }
        });

        final Button signBtn = (Button) findViewById(R.id.signBtn);
        signBtn.setOnClickListener(new View.OnClickListener(){
            public void onClick (View v){

                final EditText emailEdit = (EditText) findViewById(R.id.emailBox);
                String emailString = emailEdit.getText().toString();

                final EditText passEdit = (EditText) findViewById(R.id.passBox);
                String passString = passEdit.getText().toString();

                createAccount(emailString, passString);

                //Toast.makeText(LoginPage.this, "Sign up here.",
                //        Toast.LENGTH_SHORT).show();
            }
        });
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
                            Toast.makeText(LoginPage.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                        else{
                            Toast.makeText(LoginPage.this, "Authentication success.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public void signIn(String email, String password){
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
                            Toast.makeText(LoginPage.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                        else{
                            Toast.makeText(LoginPage.this, "Authentication success.",
                                    Toast.LENGTH_SHORT).show();
                            Intent i = new Intent(LoginPage.this, MapsActivity.class);
                            startActivity(i);
                            setContentView(R.layout.activity_maps);

                        }

                    }
                });
    }

////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////

}
