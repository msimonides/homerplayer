package com.studio4plus.audiobookplayer.service;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;

/**
 * Gather accelerometer data for a short period of time.
 */
class SampleGatherer implements SensorEventListener {

    public interface Listener {
        public void onSampleGathered(float[] minAcceleration, float[] maxAcceleration);
    }

    private static final int SAMPLE_TIME_MS = 200;
    private static final int AXIS_COUNT = 3;

    private final Listener listener;
    private long timeStart;
    private boolean hasSamples;
    private final float[] minAcceleration = new float[AXIS_COUNT];
    private final float[] maxAcceleration = new float[AXIS_COUNT];

    SampleGatherer(Listener listener) {
        this.listener = listener;
    }

    public void start() {
        timeStart = System.currentTimeMillis();
        hasSamples = false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!hasSamples) {
            System.arraycopy(event.values, 0, minAcceleration, 0, AXIS_COUNT);
            System.arraycopy(event.values, 0, maxAcceleration, 0, AXIS_COUNT);
            hasSamples = true;
        } else {
            for (int i = 0; i < AXIS_COUNT; ++i) {
                float value = event.values[i];
                if (value < minAcceleration[i])
                    minAcceleration[i] = value;
                else if (value > maxAcceleration[i])
                    maxAcceleration[i] = value;
            }
        }

        if (System.currentTimeMillis() - timeStart > SAMPLE_TIME_MS) {
            listener.onSampleGathered(minAcceleration, maxAcceleration);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
