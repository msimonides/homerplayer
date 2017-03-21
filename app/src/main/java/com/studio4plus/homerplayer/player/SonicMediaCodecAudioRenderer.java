package com.studio4plus.homerplayer.player;

import android.media.MediaCodec;
import android.media.MediaFormat;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;

import java.nio.ByteBuffer;

import sonic.Sonic;

class SonicMediaCodecAudioRenderer extends MediaCodecAudioRenderer {

    private Sonic sonic;
    private int lastSeenBufferIndex = -1;
    private byte[] inputBytes;
    private byte[] outputBytes;
    private ByteBuffer outputBuffer;
    private int channelCount;
    private float speed = 1.0f;

    static final int MSG_SET_PLAYBACK_SPEED = C.MSG_CUSTOM_BASE + 1;

    SonicMediaCodecAudioRenderer(MediaCodecSelector mediaCodecSelector) {
        super(mediaCodecSelector);
    }

    @Override
    public void handleMessage(int messageType, Object message) throws ExoPlaybackException {
        switch (messageType) {
            case MSG_SET_PLAYBACK_SPEED:
                setPlaybackSpeed((float) message);
                break;
            default:
                super.handleMessage(messageType, message);
        }
    }

    @Override
    protected boolean processOutputBuffer(long positionUs, long elapsedRealtimeUs, MediaCodec codec,
                                          ByteBuffer buffer, int bufferIndex, int bufferFlags,
                                          long bufferPresentationTimeUs, boolean shouldSkip)
            throws ExoPlaybackException {
        if (speed == 1.0f) {
            return super.processOutputBuffer(positionUs, elapsedRealtimeUs, codec, buffer,
                    bufferIndex, bufferFlags, bufferPresentationTimeUs, shouldSkip);
        }

        if (bufferIndex == lastSeenBufferIndex) {
            return super.processOutputBuffer(positionUs, elapsedRealtimeUs, codec, outputBuffer,
                    bufferIndex, bufferFlags, bufferPresentationTimeUs, shouldSkip);
        } else {
            lastSeenBufferIndex = bufferIndex;
        }

        final int inputByteCount = buffer.remaining();
        if (inputBytes == null || inputBytes.length < inputByteCount)
            inputBytes = new byte[inputByteCount];

        buffer.get(inputBytes, 0, buffer.remaining());
        sonic.writeBytesToStream(inputBytes, inputByteCount);

        final int availableByteCount = sonic.samplesAvailable() * 2 * channelCount;
        if (outputBytes == null || outputBytes.length < availableByteCount) {
            outputBytes = new byte[availableByteCount];
            outputBuffer = ByteBuffer.wrap(outputBytes);
        }
        final int outputByteCount = sonic.readBytesFromStream(outputBytes, availableByteCount);

        outputBuffer.position(0);
        outputBuffer.limit(outputByteCount);

        return super.processOutputBuffer(positionUs, elapsedRealtimeUs, codec, outputBuffer,
                bufferIndex, bufferFlags, bufferPresentationTimeUs, shouldSkip);
    }

    @Override
    protected void onOutputFormatChanged(MediaCodec codec, MediaFormat outputFormat) {
        super.onOutputFormatChanged(codec, outputFormat);

        final int sampleRate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        channelCount = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        sonic = new Sonic(sampleRate, channelCount);
        sonic.setNumChannels(channelCount);
        sonic.setSpeed(speed);
    }

    private void setPlaybackSpeed(float newSpeed) {
        speed = newSpeed;
        if (sonic != null)
            sonic.setSpeed(newSpeed);
    }
}
