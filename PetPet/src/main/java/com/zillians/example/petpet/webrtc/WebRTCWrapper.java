package com.zillians.example.petpet.webrtc;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.zillians.example.petpet.webrtc.sdp.SdpHelper;
import com.zillians.example.petpet.webrtc.signal.RestfulSignalService;
import com.zillians.example.petpet.webrtc.signal.SignalService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.StatsObserver;
import org.webrtc.StatsReport;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by giggle on 9/30/13.
 */
public class WebRTCWrapper {
    private static final String TAG = "WebRTCWrapper";

    class SdpObserver implements org.webrtc.SdpObserver {

        @Override
        public void onCreateSuccess(final SessionDescription sessionDescription) {
            Log.d(TAG, "SdpObserver: onCreateSuccess");
            Log.d(TAG, "Sending: " + sessionDescription.type);
//            Log.d(TAG, "Description: " + sessionDescription.description);

            mHandler.post(new Runnable() {
                public void run() {
                    final SessionDescription localSdp = new SessionDescription(
                            sessionDescription.type, SdpHelper.preferISAC(sessionDescription.description));

                    // Pass my description to the other peer
                    mSignalService.asyncSendOffer(sessionDescription, new SignalService.OnCallback() {
                        @Override
                        public void onFailCallback(String error) {

                        }

                        @Override
                        public void onSuccessCallback(String response) {
                            Log.d(TAG, "SdpObserver: Set local and remote description");
                            mPeerConnection.setLocalDescription(SdpObserver.this, localSdp);
                            mPeerConnection.setRemoteDescription(SdpObserver.this,
                                    new SessionDescription(SessionDescription.Type.ANSWER, response));
                        }
                    });
                }
            });
        }

//        private void drainRemoteCandidates() {
//            for (IceCandidate candidate : mQueuedRemoteCandidates) {
//                mPeerConnection.addIceCandidate(candidate);
//            }
//            mQueuedRemoteCandidates.clear();
//        }

        @Override
        public void onSetSuccess() {
            Log.d(TAG, "SdpObserver: onSetSuccess");
//            mHandler.post(new Runnable() {
//                @Override
//                public void run() {
//                    if (mPeerConnection.getLocalDescription() == null) {
//                        // We just set the remote offer, time to create our answer.
//                        Log.d(TAG, "SdpObserver: Creating answer");
//                        mPeerConnection.createAnswer(SdpObserver.this, mMediaConstraints);
//                    } else {
//                        Log.d(TAG, "SdpObserver: drain remote candidates");
//                        // Sent our answer and set it as local description; drain
//                        // candidates.
//                        drainRemoteCandidates();
//                    }
//                }
//            });
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

    class PeerConnectionObserver implements PeerConnection.Observer {

        private List<IceCandidate> mCandidates = new ArrayList<IceCandidate>();

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
            if (iceGatheringState != PeerConnection.IceGatheringState.COMPLETE) return;

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Prepare to send candidates to peer");
                    mSignalService.asyncSendIceCandidates(mCandidates, new SignalService.OnCallback() {
                        @Override
                        public void onFailCallback(String error) {

                        }

                        @Override
                        public void onSuccessCallback(String response) {
                            receiveRemoteCandidates(response);
                        }
                    });
                }
            });
        }

        private void receiveRemoteCandidates(String response) {
            JSONTokener tokenizer = new JSONTokener(response);
            try {
                JSONArray candidates = new JSONArray(tokenizer);
                for(int i=0; i<candidates.length(); i++){
                    JSONObject candidateObject = candidates.getJSONObject(i);
                    IceCandidate candidate =new IceCandidate(
                            candidateObject.getString("sdpMid"),
                            candidateObject.getInt("sdpMLineIndex"),
                            candidateObject.getString("candidate"));
                    Log.d(TAG, "adding new ice candidate: " + candidate);
                    mPeerConnection.addIceCandidate(candidate);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onIceCandidate(final IceCandidate iceCandidate) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "PeerConnectionObserver: onIceCandidate");

                    Log.d(TAG, " ---- sdp = " + iceCandidate.sdp);
                    Log.d(TAG, " ==== sdpMid = " + iceCandidate.sdpMid);
                    Log.d(TAG, " ==== sdp line index = " + iceCandidate.sdpMLineIndex);
                    mCandidates.add(iceCandidate);
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

    private static final String TURN_SERVER = "wtf-master.aws.zillians.com:3478";
    private static final String USERNAME = "zillians";
    private static final String PASSWORD = "izillians";

    private PeerConnectionFactory mPeerConnectionFactory;
    private PeerConnection mPeerConnection;
    private MediaConstraints mMediaConstraints;

    private Handler mHandler;
    private RestfulSignalService mSignalService = new RestfulSignalService();

    public WebRTCWrapper(Context context) {
        mHandler = new Handler();

        PeerConnectionFactory.initializeAndroidGlobals(context);
        mPeerConnectionFactory = new PeerConnectionFactory();

        // Specify what data to transfer
        mMediaConstraints = new MediaConstraints();
        mMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveAudio", "true"));
        mMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveVideo", "true"));
    }

    public void start() {
         // Hard code ice servers
        List<PeerConnection.IceServer> iceServers = new ArrayList<PeerConnection.IceServer>();
        iceServers.add(new PeerConnection.IceServer(TURN_SERVER, USERNAME, PASSWORD));

        mPeerConnection = mPeerConnectionFactory.createPeerConnection(iceServers, mMediaConstraints, new PeerConnectionObserver());

        // State report
        PeerConnection.SignalingState state = mPeerConnection.signalingState();
        Log.d(TAG, "Signal state (must stable) = " + state.toString());

        PeerConnection.IceConnectionState iceConnectionState = mPeerConnection.iceConnectionState();
        Log.d(TAG, "Ice connection state (must new) = " + iceConnectionState.toString());

//        PeerConnection.IceGatheringState iceGatheringState = mPeerConnection.iceGatheringState();
//        Log.d(TAG, "Ice gathering state (must new) = " + iceGatheringState.toString());

//        mHandler.postDelayed(checkPeerState, 10000);
        mPeerConnection.createOffer(new SdpObserver(), mMediaConstraints);
        Log.d(TAG, "Peer Connection created");
    }

    public void stop() {

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


}
