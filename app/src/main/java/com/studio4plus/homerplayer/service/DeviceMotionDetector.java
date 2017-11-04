package com.studio4plus.homerplayer.service;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.annotation.NonNull;

import java.util.concurrent.TimeUnit;

/**
 * Detect whether the device is being shaken or still.
 */
class DeviceMotionDetector implements SensorEventListener {

    public interface Listener {
        void onSignificantMotion();
        void onFaceDownStill();
    }

    private static final float MAX_STILL_TOLERANCE = 1f;
    private static final float MAX_FACEDOWN_DEVIATION = 2f;
    private static final float MIN_SIGNIFICANT_MOTION = 5f;
    private static final long MIN_TIME_WINDOW = TimeUnit.MILLISECONDS.toNanos(500);

    private final @NonNull SensorManager sensorManager;
    private final @NonNull Sensor accelerometer;
    private final @NonNull Listener listener;
    private @NonNull SamplesQueue queue;

    private long AVG_SMOOTH_TIME_NANOS = TimeUnit.SECONDS.toNanos(3);
    private float avgAcceleration[] = new float[3];

    private long previousTimestamp = 0;
    private float previousValues[] = new float[3];

    enum MotionType {
        FACE_DOWN,
        ACCELERATING,
        OTHER
    }

    static boolean hasSensors(SensorManager sensorManager) {
        return sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null;
    }

    DeviceMotionDetector(@NonNull SensorManager sensorManager, @NonNull Listener listener) {
        this.sensorManager = sensorManager;
        this.accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.listener = listener;
        this.queue = new SamplesQueue();
    }

    void enable() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    void disable() {
        sensorManager.unregisterListener(this, accelerometer);
        queue = new SamplesQueue();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        final float x = event.values[0];
        final float y = event.values[1];
        final float z = event.values[2];

        float sampleDeltaSum = 0f;
        float accDeltaSum = 0f;
        if (previousTimestamp > 0) {
            long deltaTimeNano = event.timestamp - previousTimestamp;
            float alpha = (float) deltaTimeNano / AVG_SMOOTH_TIME_NANOS;
            for (int i = 0; i < avgAcceleration.length; ++i) {
                avgAcceleration[i] = alpha * event.values[i] + (1f - alpha) * avgAcceleration[i];
                final float a = Math.abs(event.values[i]);
                accDeltaSum += Math.abs(a - Math.abs(avgAcceleration[i]));
                sampleDeltaSum += Math.abs(a - Math.abs(previousValues[i]));
            }

            System.arraycopy(event.values, 0, previousValues, 0, event.values.length);
        }
        previousTimestamp = event.timestamp;

        boolean isAccelerating = accDeltaSum > MIN_SIGNIFICANT_MOTION;
        final float acceleration = (float) Math.sqrt(x * x + y * y + z * z);
        boolean isStill = sampleDeltaSum < MAX_STILL_TOLERANCE;
        boolean isFaceDown = isStill && z < 0 &&
                Math.abs(acceleration - Math.abs(z)) < MAX_FACEDOWN_DEVIATION;

        MotionType sampleType = isFaceDown ? MotionType.FACE_DOWN :
                isAccelerating ? MotionType.ACCELERATING : MotionType.OTHER;

        queue.add(event.timestamp, sampleType);
        MotionType detectedType = queue.getMotionType();
        queue.purgeOld(event.timestamp - MIN_TIME_WINDOW);

        switch (detectedType) {
            case FACE_DOWN:
                queue = new SamplesQueue();
                listener.onFaceDownStill();
                break;
            case ACCELERATING:
                queue = new SamplesQueue();
                listener.onSignificantMotion();
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }


    // Inspired by Square's seismic ShakeDetector.
    private static class SamplesQueue {
        private static final int MIN_LENGTH = 4;

        private static class SampleEntry {
            MotionType motionType;
            private long timestamp;

            SampleEntry next = null;
        }

        private SampleEntry newest = null;
        private SampleEntry oldest = null;
        private int count = 0;
        private int acceleratingCount = 0;
        private int faceDownCount = 0;

        private SampleEntry unused = null;

        void add(long timestamp, MotionType type) {
            SampleEntry newSample;
            if (unused != null) {
                newSample = unused;
                unused = newSample.next;
            } else {
                newSample = new SampleEntry();
            }
            newSample.timestamp = timestamp;
            newSample.motionType = type;
            newSample.next = null;

            if (newest != null)
              newest.next = newSample;
            newest = newSample;
            if (oldest == null)
                oldest = newest;
            if (type == MotionType.ACCELERATING)
                ++acceleratingCount;
            if (type == MotionType.FACE_DOWN)
                ++faceDownCount;
            ++count;
        }

        void purgeOld(long timestamp) {
            while(oldest != null && count > MIN_LENGTH && oldest.timestamp < timestamp) {
                SampleEntry removed = oldest;
                oldest = removed.next;
                remove(removed);
            }
        }

        MotionType getMotionType() {
            if (newest.timestamp - oldest.timestamp > MIN_TIME_WINDOW) {
                int threshold = (count >> 1) + (count >> 2);  // count * 0.75
                if (faceDownCount >= threshold)
                    return MotionType.FACE_DOWN;
                if (acceleratingCount >= threshold)
                    return MotionType.ACCELERATING;
            }

            return MotionType.OTHER;
        }

        private void remove(SampleEntry sample) {
            --count;
            if (sample.motionType == MotionType.ACCELERATING)
                --acceleratingCount;
            if (sample.motionType == MotionType.FACE_DOWN)
                --faceDownCount;
            sample.next = unused;
            unused = sample;
        }
    }

}
