package com.zillians.example.petpet;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by giggle on 9/30/13.
 */
public class SignalService {
    private static final String TAG = "SignalService";
    private static final String SIGNALING_SERVER = "https://apprtc.appspot.com/message?r=6666&u=80794597";

    private ExecutorService mExecutors = Executors.newSingleThreadExecutor();

    private void sendMessage(final JSONObject jsonObject) {
        mExecutors.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    URLConnection connection = null;
                    connection = new URL(SIGNALING_SERVER).openConnection();
                    connection.setDoOutput(true);
                    connection.getOutputStream().write(jsonObject.toString().getBytes("UTF-8"));
                    Log.d(TAG, "SendMessage Response: " + connection.getHeaderField(null).toString());
                    if (!connection.getHeaderField(null).startsWith("HTTP/1.1 200 ")) {
                        throw new IOException(
                                "Non-200 response to POST: " + connection.getHeaderField(null) +
                                        " for msg: " + jsonObject.toString());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static void jsonPut(JSONObject json, String key, Object value) {
        try {
            json.put(key, value);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendIceCandidate(IceCandidate iceCandidate) {
        JSONObject json = new JSONObject();
        jsonPut(json, "type", "candidate");
        jsonPut(json, "label", iceCandidate.sdpMLineIndex);
        jsonPut(json, "id", iceCandidate.sdpMid);
        jsonPut(json, "candidate", iceCandidate.sdp);
        sendMessage(json);
    }

    public void sendSdp(final SessionDescription sdp) {
        Log.d(TAG, "Send sdp to the other peer");
        JSONObject json = new JSONObject();
        jsonPut(json, "type", sdp.type.canonicalForm());
        jsonPut(json, "sdp", sdp.description);
        sendMessage(json);
    }

}
