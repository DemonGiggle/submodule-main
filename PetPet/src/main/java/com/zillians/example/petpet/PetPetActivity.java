package com.zillians.example.petpet;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;

import com.zillians.example.petpet.webrtc.WebRTCWrapper;

public class PetPetActivity extends Activity {
    private static final String TAG = "PetPetActivity";

    private WebRTCWrapper mWebRTCWrapper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    @Override
    protected void onResume() {
        super.onResume();
        mWebRTCWrapper = new WebRTCWrapper(this);
        mWebRTCWrapper.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.pet_pet, menu);
        return true;
    }
    
}
