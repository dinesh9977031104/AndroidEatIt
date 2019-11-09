package com.patidar.dinesh.androideatit;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.FacebookSdk;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.patidar.dinesh.androideatit.Common.Common;
import com.patidar.dinesh.androideatit.Model.User;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;

import io.paperdb.Paper;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends AppCompatActivity {


    Button btnSignUp, btnSignIn;
    // FButton btnSignUp, btnSignIn;
    TextView txtSlogan;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //note: add this code before setcontent view
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/NABILA.TTF")
                .setFontAttrId(R.attr.fontPath)
                .build());
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_main);

        printKeyHash();

        btnSignUp = (Button) findViewById(R.id.btnSignUp);
        btnSignIn = (Button) findViewById(R.id.btnSignIn);
        txtSlogan = (TextView) findViewById(R.id.txtSlogan);

        Typeface face = Typeface.createFromAsset(getAssets(), "fonts/NABILA.TTF");
        txtSlogan.setTypeface(face);

        //init paper
        Paper.init(this);

        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signIn = new Intent(MainActivity.this, SignIn.class);
                startActivity(signIn);
            }
        });


        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signUp = new Intent(MainActivity.this, SignUp.class);
                startActivity(signUp);
            }
        });

        //check remember
        String user = Paper.book().read(Common.USER_KEY);
        String pwd = Paper.book().read(Common.PWD_KEY);

        if (user != null && pwd != null){
            if (!user.isEmpty() && !pwd.isEmpty())
                login(user,pwd);
        }
    }

    private void printKeyHash() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo("com.patidar.dinesh.androideatit",
                    PackageManager.GET_SIGNATURES);

            for (android.content.pm.Signature signature:info.signatures)
            {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash", Base64.encodeToString(md.digest(),Base64.DEFAULT));
                //jenerated key -->  KepsbpZkQsuxZuWgocoO/V8gUnE=
            }

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private void login(final String phone, final String pwd) {

        //just copy login code from signin class
        // Init databases
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference table_user = database.getReference("User");

        if (Common.isConnectedToInternet(getBaseContext())) {


            final ProgressDialog mDialog = new ProgressDialog(MainActivity.this);
            mDialog.setMessage("Please Waiting...");
            mDialog.show();

            table_user.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    // check if user is does not exist in databases
                    if (dataSnapshot.child(phone).exists()) {
                        // Get User Informaton
                        mDialog.dismiss();

                        User user = dataSnapshot.child(phone).getValue(User.class);

                        user.setPhone(phone); // set phone

                        if (user.getPassword().equals(pwd)) {
                            //  Toast.makeText(SignIn.this, "Sign In Successfully !", Toast.LENGTH_SHORT).show();

                            Intent homeIntent = new Intent(MainActivity.this, RestaurantList.class);
                            Common.currentUser = user;
                            startActivity(homeIntent);
                            finish();

                        } else {
                            Toast.makeText(MainActivity.this, "Wrong Password !!!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        mDialog.dismiss();
                        Toast.makeText(MainActivity.this, "User not exist in databases ", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
        else {
            Toast.makeText(MainActivity.this, "Please check your Internet Connection!!!", Toast.LENGTH_SHORT).show();
            return;
        }
    }
}
