package com.zillians.example.petpet;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;

import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.StatsObserver;
import org.webrtc.StatsReport;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class PetPetActivity extends Activity {
    private static final String TAG = "PetPetActivity";

    class MySdpObserver implements SdpObserver {

        @Override
        public void onCreateSuccess(final SessionDescription sessionDescription) {
            Log.d(TAG, "SdpObserver: onCreateSuccess");
            Log.d(TAG, "Sending: " + sessionDescription.type);
//            Log.d(TAG, "Description: " + sessionDescription.description);

            runOnUiThread(new Runnable() {
                public void run() {
                    Log.d(TAG, "SdpObserver: Set local description");
                    SessionDescription sdp = new SessionDescription(
                            sessionDescription.type, SdpHelper.preferISAC(sessionDescription.description));
                    mPeerConnection.setLocalDescription(MySdpObserver.this, sdp);

                    // Pass my description to the other peer
                    mSignalService.sendSdp(sessionDescription);
                }
            });
        }

        private void drainRemoteCandidates() {
            for (IceCandidate candidate : mQueuedRemoteCandidates) {
                mPeerConnection.addIceCandidate(candidate);
            }
            mQueuedRemoteCandidates.clear();
        }

        @Override
        public void onSetSuccess() {
            Log.d(TAG, "SdpObserver: onSetSuccess");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mPeerConnection.getLocalDescription() == null) {
                        // We just set the remote offer, time to create our answer.
                        Log.d(TAG, "SdpObserver: Creating answer");
                        mPeerConnection.createAnswer(MySdpObserver.this, mMediaConstraints);
                    } else {
                        Log.d(TAG, "SdpObserver: drain remote candidates");
                        // Sent our answer and set it as local description; drain
                        // candidates.
                        drainRemoteCandidates();
                    }
                }
            });
        }

        @Override
        public void onCreateFailure(String s) {
            Log.d(TAG, "SdpObserver: onCreateFailure");
        }

        @Override
        public void onSetFailure(String s) {
            Log.d(TAG, "SdpObserver: onSetFailure");
        }
    }

    class MyPeerConnectionObserver implements PeerConnection.Observer {

        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {
            Log.d(TAG, "PeerConnectionObserver: onSignalingChange");
        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            Log.d(TAG, "PeerConnectionObserver: onIceConnectionChange");
        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
            Log.d(TAG, "PeerConnectionObserver: onIceGatheringChange");
        }

        @Override
        public void onIceCandidate(final IceCandidate iceCandidate) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "PeerConnectionObserver: onIceCandidate");

                    Log.d(TAG, " ---- sdp = " + iceCandidate.sdp);
                    Log.d(TAG, " ==== sdpMid = " + iceCandidate.sdpMid);
                    Log.d(TAG, " ==== sdp line index = " + iceCandidate.sdpMLineIndex);
                    mSignalService.sendIceCandidate(iceCandidate);
                }
            });
        }

        @Override
        public void onError() {
            Log.d(TAG, "PeerConnectionObserver: onError");
        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
            Log.d(TAG, "PeerConnectionObserver: onAddStream");
        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {
            Log.d(TAG, "PeerConnectionObserver: onRemoveStream");
        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {
            Log.d(TAG, "PeerConnectionObserver: onDataChannel");
        }
    }

    private static final String STUN_SERVER = "stun:stun.l.google.com:19302";

    private PeerConnectionFactory mPeerConnectionFactory;
    private PeerConnection mPeerConnection;
    private MediaConstraints mMediaConstraints;
    private LinkedList<IceCandidate> mQueuedRemoteCandidates = new LinkedList<IceCandidate>();

    private Handler mHandler;
    private SignalService mSignalService = new SignalService();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    private Runnable checkPeerState = new Runnable() {
        @Override
        public void run() {

            boolean success = mPeerConnection.getStats(new StatsObserver() {
                public void onComplete(StatsReport[] reports) {
                    for (StatsReport report : reports) {
                        Log.d(TAG, "Stats: " + report.toString());
                    }
                    mHandler.postDelayed(checkPeerState, 10000);
                }
            }, null);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        mHandler = new Handler();

        PeerConnectionFactory.initializeAndroidGlobals(this);
        mPeerConnectionFactory = new PeerConnectionFactory();

        // Specify what data to transfer
        mMediaConstraints = new MediaConstraints();
//        mMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
//                "OfferToReceiveAudio", "true"));
//        mMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
//                "OfferToReceiveVideo", "true"));

        // Hard code ice servers
        List<PeerConnection.IceServer> iceServers = new ArrayList<PeerConnection.IceServer>();
        iceServers.add(new PeerConnection.IceServer(STUN_SERVER, "", ""));

        mPeerConnection = mPeerConnectionFactory.createPeerConnection(iceServers, new MediaConstraints(), new MyPeerConnectionObserver());

        // State report
        PeerConnection.SignalingState state = mPeerConnection.signalingState();
        Log.d(TAG, "Signal state (must stable) = " + state.toString());

        PeerConnection.IceConnectionState iceConnectionState = mPeerConnection.iceConnectionState();
        Log.d(TAG, "Ice connection state (must new) = " + iceConnectionState.toString());

//        PeerConnection.IceGatheringState iceGatheringState = mPeerConnection.iceGatheringState();
//        Log.d(TAG, "Ice gathering state (must new) = " + iceGatheringState.toString());

        mHandler.postDelayed(checkPeerState, 10000);

        mPeerConnection.createOffer(new MySdpObserver(), mMediaConstraints);

        Log.d(TAG, "Peer Connection created");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.pet_pet, menu);
        return true;
    }
    
}
