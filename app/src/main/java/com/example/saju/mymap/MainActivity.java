package com.example.saju.mymap;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE = 9000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (isServiceOk()) {
            init();
        }

    }

    private void init() {
        Button mapButton = findViewById(R.id.map_id);

        mapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, MapActivity.class);
                startActivity(intent);
            }
        });

    }


    public boolean isServiceOk() {
        Log.d(TAG, "isServiceOk: checking service version");

        int available = GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(this);

        if (available == ConnectionResult.SUCCESS) {
            // everything is fine, now user can make request
            Log.d(TAG, "isServiceOk: Google play Service working");
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            // error occured, but we can resolve it
            Log.d(TAG, "isServiceOk: an error occured, but we can resolve it");
            Dialog dialog = GoogleApiAvailability.getInstance()
                    .getErrorDialog(MainActivity.this, available, REQUEST_CODE);
            dialog.show();
        } else {
            // we can't do anything
            Toast.makeText(MainActivity.this, "You can not make map Request", Toast.LENGTH_LONG).show();

        }

        return false;
    }

}
