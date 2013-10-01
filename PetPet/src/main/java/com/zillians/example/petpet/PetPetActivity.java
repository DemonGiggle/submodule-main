package com.zillians.example.petpet;

import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

import com.zillians.example.petpet.webrtc.WebRTCWrapper;
import com.zillians.example.petpet.webrtc.video.VideoRenderCallback;
import com.zillians.example.petpet.webrtc.video.VideoStreamsView;

import org.webrtc.MediaStream;
import org.webrtc.VideoRenderer;

public class PetPetActivity extends Activity {
    private static final String TAG = "PetPetActivity";

    private WebRTCWrapper mWebRTCWrapper;
    private VideoStreamsView mVideoStreamView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);

        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);
        mVideoStreamView = new VideoStreamsView(this, displaySize);

        setContentView(mVideoStreamView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mVideoStreamView.onResume();

        mWebRTCWrapper = new WebRTCWrapper(this);
        mWebRTCWrapper.setOnWebRTCCallback(new WebRTCWrapper.OnWebRTCCallback() {
            @Override
            public void onAddStream(MediaStream stream) {
                Log.d(TAG, "onAddStream: video track size = " + stream.videoTracks.size());
                stream.videoTracks.get(0).addRenderer(new VideoRenderer(
                        new VideoRenderCallback(mVideoStreamView, VideoStreamsView.Endpoint.REMOTE)
                ));
            }
        });

        mWebRTCWrapper.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mVideoStreamView.onPause();
        mWebRTCWrapper.stop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.pet_pet, menu);
        return true;
    }
    
}
