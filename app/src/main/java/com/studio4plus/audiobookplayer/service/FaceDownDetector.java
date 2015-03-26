package com.studio4plus.audiobookplayer.service;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;

// TODO: measure battery drain of various approaches to detecting the device's position:
// - gathering sensor data in short bursts, as currently (shorter bursts with higher frequency?),
// - gathering sensor data with low frequency but all the time.
public class FaceDownDetector implements SampleGatherer.Listener {

    public interface Listener {
        void onDeviceFaceDown();
    }

    private static final int Z_AXIS = 2;
    private static final long SAMPLE_INTERVAL_MS = 500;

    // TODO: eventually we'll want to have a calibration mechanism for these paramaters
    private static final float MAX_STILL_DELTA_ACCELERATION = 0.6f; // m/s^2
    private static final float MAX_SQR_DEVIATION = 1.5f;

    private final SensorManager sensorManager;
    private final Sensor accelerometer;
    private final SampleGatherer sampleGatherer;
    private final Handler mainThreadHandler;
    private final Listener listener;

    private final Runnable sampleGatheringStarter = new Runnable() {
        @Override
        public void run() {
            startGatheringSample();
        }
    };

    public static boolean hasSensors(SensorManager sensorManager) {
        return sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null;
    }

    public FaceDownDetector(
            SensorManager sensorManager, Handler mainThreadHandler, Listener listener) {
        this.sensorManager = sensorManager;
        this.mainThreadHandler = mainThreadHandler;
        this.listener = listener;
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sampleGatherer = new SampleGatherer(this);
    }

    public void enable() {
        startGatheringSample();
    }

    public void disable() {
        sensorManager.unregisterListener(sampleGatherer, accelerometer);
        mainThreadHandler.removeCallbacks(sampleGatheringStarter);
    }

    @Override
    public void onSampleGathered(float[] minAcceleration, float[] maxAcceleration) {
        sensorManager.unregisterListener(sampleGatherer);

        final int AXIS_COUNT = 3;
        boolean isStill = true;
        for (int i = 0; i < AXIS_COUNT; ++i) {
            if (Math.abs(maxAcceleration[i] - minAcceleration[i]) > MAX_STILL_DELTA_ACCELERATION) {
                Log.d("FaceDownDetector", "delta acceleration " + Math.abs(maxAcceleration[i] - minAcceleration[i]));
                isStill = false;
                break;
            }
        }

        @SuppressWarnings("UnnecessaryLocalVariable") float[] a = maxAcceleration;
        if (isStill && a[Z_AXIS] < 0) {
            float accelerationSquared = a[0]*a[0] + a[1]*a[1] + a[2]*a[2];
            float zSquared = a[Z_AXIS]*a[Z_AXIS];
            Log.d("FaceDownDetector", "acceleration: " + Math.sqrt(accelerationSquared) + " z: " + a[Z_AXIS]);
            if (Math.abs(accelerationSquared - zSquared) < MAX_SQR_DEVIATION)
                listener.onDeviceFaceDown();
        }

        mainThreadHandler.postDelayed(sampleGatheringStarter, SAMPLE_INTERVAL_MS);
    }

    private void startGatheringSample() {
        sampleGatherer.start();
        sensorManager.registerListener(
                sampleGatherer, accelerometer, SensorManager.SENSOR_DELAY_UI);
    }
}
