package com.zillians.example.petpet.webrtc.signal;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.util.List;

/**
 * Created by giggle on 10/1/13.
 */
public interface SignalService {

    public interface OnCallback {
        public void onFailCallback(String error);
        public void onSuccessCallback(String response);
    }

    /**
     * Send offer description to peer
     * @param sdp
     * @param callback
     */
    public void asyncSendOffer(SessionDescription sdp, OnCallback callback);

    /**
     * Send candidates to peer
     * @param candidates
     * @param callback
     */
    public void asyncSendIceCandidates(List<IceCandidate> candidates, OnCallback callback);
}
