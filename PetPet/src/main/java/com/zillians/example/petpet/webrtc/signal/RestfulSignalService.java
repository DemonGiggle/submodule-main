package com.zillians.example.petpet.webrtc.signal;

import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.zillians.example.petpet.NoTrustSocketFactory;

import org.apache.http.params.HttpConnectionParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by giggle on 9/30/13.
 */
public class RestfulSignalService implements SignalService {
    private static final String TAG = "RestfulSignalService";
    private static final String USERNAME = "cb4ac838727047809242bd2fb307cb5c";
    private static final String PASSWORD = "c6bc55076dd84d379b64a57fa5de58bd9162e65eef5845738594183b0b913f4f";
    private static final String USER_AGENT = "petcam/1.0";
    private static final String OFFER_SERVER = "https://petcam-internal.aws.zillians.com:7999/p2p/offer";
    private static final String CANDIDATE_SERVER = "https://petcam-internal.aws.zillians.com:7999/p2p/candidate/caller";

    @Override
    public void asyncSendOffer(SessionDescription sdp, final OnCallback callback) {
        // prepare the parameters
        RequestParams params = new RequestParams();
        params.put("sdp", sdp.description);

        AsyncHttpClient client = createHttpsClient();
        client.post(OFFER_SERVER, params, new AsyncHttpResponseHandler() {

            @Override
            public void onFailure(Throwable e, String error) {
                Log.e(TAG, "Send offer fail: " + error);
                callback.onFailCallback(error);
            }

            @Override
            public void onSuccess(String response) {
                Log.d(TAG, "Send offer success: " + response);
                callback.onSuccessCallback(response);
            }
        });
    }

    @Override
    public void asyncSendIceCandidates(List<IceCandidate> candidates, final OnCallback callback) {

        JSONArray jsonArray = new JSONArray();
        for (IceCandidate candidate : candidates) {
            JSONObject jsonObject = new JSONObject();
            jsonPut(jsonObject, "sdpMLineIndex", candidate.sdpMLineIndex);
            jsonPut(jsonObject, "sdpMid", candidate.sdpMid);
            jsonPut(jsonObject, "candidate", candidate.sdp);
            jsonArray.put(jsonObject);
        }

        RequestParams params = new RequestParams();
        params.put("candidate", jsonArray.toString());

        // http client
        AsyncHttpClient client = createHttpsClient();
        client.post(CANDIDATE_SERVER, params, new AsyncHttpResponseHandler() {

            @Override
            public void onFailure(Throwable e, String error) {
                Log.e(TAG, "Send candidates fail: " + error);
                callback.onFailCallback(error);
            }

            @Override
            public void onSuccess(String response) {
                Log.d(TAG, "Send candidates success: " + response);
                callback.onSuccessCallback(response);
            }
        });
    }

    private static AsyncHttpClient createHttpsClient() {
        AsyncHttpClient client = new AsyncHttpClient();
        HttpConnectionParams.setConnectionTimeout(client.getHttpClient().getParams(), 0);
        HttpConnectionParams.setSoTimeout(client.getHttpClient().getParams(), 0);

        client.setUserAgent(USER_AGENT);
        client.setBasicAuth(USERNAME, PASSWORD);

        try {
            client.setSSLSocketFactory(NoTrustSocketFactory.createSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return client;
    }

    private static void jsonPut(JSONObject json, String key, Object value) {
        try {
            json.put(key, value);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
