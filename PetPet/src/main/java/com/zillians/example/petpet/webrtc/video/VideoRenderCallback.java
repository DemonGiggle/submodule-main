package com.zillians.example.petpet.webrtc.video;

import org.webrtc.VideoRenderer;

/**
 * Created by giggle on 10/1/13.
 */
public class VideoRenderCallback implements VideoRenderer.Callbacks {
    private final VideoStreamsView mView;
    private final VideoStreamsView.Endpoint mStream;

    public VideoRenderCallback(VideoStreamsView mView, VideoStreamsView.Endpoint mStream) {
        this.mView = mView;
        this.mStream = mStream;
    }

    @Override
    public void setSize(final int width, final int height) {
        mView.queueEvent(new Runnable() {
            @Override
            public void run() {
                mView.setSize(mStream, width, height);
            }
        });
    }

    @Override
    public void renderFrame(VideoRenderer.I420Frame frame) {
        mView.queueFrame(mStream, frame);
    }
}
