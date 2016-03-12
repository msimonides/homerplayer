package com.studio4plus.homerplayer.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.HomerPlayerApplication;
import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.events.PlaybackElapsedTimeSyncEvent;
import com.studio4plus.homerplayer.events.PlaybackStoppingEvent;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class FragmentPlayback extends Fragment {

    private View view;
    private Button stopButton;
    private TextView elapsedTimeView;
    private TimerTask timerTask;

    @Inject public GlobalSettings globalSettings;
    @Inject public EventBus eventBus;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_playback, container, false);
        HomerPlayerApplication.getComponent(view.getContext()).inject(this);

        stopButton = (Button) view.findViewById(R.id.stopButton);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getMainActivity().stopPlayback();
            }
        });

        elapsedTimeView = (TextView) view.findViewById(R.id.elapsedTime);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        showHintIfNecessary();
        eventBus.register(this);
        getMainActivity().requestElapsedTimeSyncEvent();
    }

    @Override
    public void onPause() {
        if (timerTask != null)
            timerTask.stop();
        eventBus.unregister(this);
        super.onPause();
    }

    @SuppressWarnings({"UnusedParameters", "UnusedDeclaration"})
    public void onEvent(PlaybackStoppingEvent event) {
        if (stopButton != null)
            stopButton.setEnabled(false);
        if (timerTask != null) {
            timerTask.stop();
            timerTask = null;
        }
    }

    @SuppressWarnings({"UnusedParameters", "UnusedDeclaration"})
    public void onEvent(PlaybackElapsedTimeSyncEvent event) {
        if (timerTask != null)
            timerTask.stop();

        timerTask = new TimerTask(new Handler(Looper.myLooper()), event.playbackPositionMs);
        timerTask.run();
    }

    private String elapsedTime(long elapsedMs) {
        long hours = TimeUnit.MILLISECONDS.toHours(elapsedMs);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMs) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedMs) % 60;

        return getString(R.string.playback_elapsed_time, hours, minutes, seconds);
    }

    private void showHintIfNecessary() {
        if (isResumed() && isVisible()) {
            if (!globalSettings.flipToStopHintShown()) {
                HintOverlay overlay = new HintOverlay(
                        view, R.id.flipToStopHintOverlayStub, R.string.hint_flip_to_stop, R.drawable.hint_flip_to_stop);
                overlay.show();
                globalSettings.setFlipToStopHintShown();
            }
        }
    }

    private MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }

    private static final long TIMER_INTERVAL_MS = TimeUnit.SECONDS.toMillis(1);

    private class TimerTask implements Runnable {

        private final Handler handler;
        private final long startTimeMs;
        private final long baseDisplayTimeMs;

        private TimerTask(Handler handler, long baseDisplayTimeMs) {
            this.handler = handler;
            this.baseDisplayTimeMs = baseDisplayTimeMs;
            this.startTimeMs = System.currentTimeMillis();
        }

        @Override
        public void run() {
            long elapsedMs = System.currentTimeMillis() - startTimeMs;
            elapsedTimeView.setText(elapsedTime(baseDisplayTimeMs + elapsedMs));

            long adjustmentMs = (System.currentTimeMillis() - startTimeMs) % TIMER_INTERVAL_MS;
            handler.postDelayed(this, TIMER_INTERVAL_MS - adjustmentMs);
        }

        public void stop() {
            handler.removeCallbacks(this);
        }
    }
}
