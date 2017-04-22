package com.studio4plus.homerplayer.ui;

import android.content.res.Resources;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;

import com.crashlytics.android.Crashlytics;
import com.google.common.base.Preconditions;
import com.studio4plus.homerplayer.R;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.EnumMap;

import javax.inject.Inject;

public class SoundBank {

    public enum SoundId {
        FF_REWIND
    }

    public static class Sound {
        public final AudioTrack track;
        public final long frameCount;
        public final int sampleRate;

        public Sound(AudioTrack track, long frameCount, int sampleRate) {
            this.track = track;
            this.frameCount = frameCount;
            this.sampleRate = sampleRate;
        }
    }

    private final EnumMap<SoundId, Sound> tracks = new EnumMap<>(SoundId.class);

    public static void stopTrack(AudioTrack track) {
        // https://code.google.com/p/android/issues/detail?id=155984
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            track.pause();
            track.flush();
        } else {
            track.stop();
        }
    }

    @Inject
    public SoundBank(Resources resources) {
        Sound sound = createSoundFromWavResource(resources, R.raw.rewind_sound, true);
        if (sound != null)
            tracks.put(SoundId.FF_REWIND, sound);
    }

    public Sound getSound(SoundId soundId) {
        return tracks.get(soundId);
    }

    private static Sound createSoundFromWavResource(
            Resources resources, int resourceId, boolean isLooping) {
        try {
            byte[] data = loadResourceData(resources.openRawResource(resourceId));
            ByteBuffer buffer = ByteBuffer.wrap(data);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            int channelCount = buffer.getShort(WAVE_CHANNELS_OFFSET);
            int sampleRate = buffer.getInt(WAVE_SAMPLERATE_OFFSET);

            int sizeInBytes = data.length - WAVE_DATA_OFFSET;
            AudioTrack track = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    channelCount == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    sizeInBytes,
                    AudioTrack.MODE_STATIC);
            track.write(data, WAVE_DATA_OFFSET, sizeInBytes);

            final int frameCount = sizeInBytes / channelCount / 2; // assumes PCM_16BIT (2 bytes)
            if (isLooping)
                track.setLoopPoints(0, frameCount, -1);

            return new Sound(track, frameCount, sampleRate);
        } catch (IOException e) {
            Crashlytics.logException(e);
            return null;
        }
    }

    private static byte[] loadResourceData(InputStream inputStream) throws IOException {
        final int length = inputStream.available();
        byte[] bytes = new byte[length];
        final int bytesRead = inputStream.read(bytes);
        Preconditions.checkState(bytesRead == length);
        return bytes;
    }

    private static final int WAVE_CHANNELS_OFFSET = 22;
    private static final int WAVE_SAMPLERATE_OFFSET = 24;
    private static final int WAVE_DATA_OFFSET = 44;
}
