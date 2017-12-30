package com.studio4plus.homerplayer.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.HomerPlayerApplication;
import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.battery.BatteryStatusProvider;
import com.studio4plus.homerplayer.ui.classic.ClassicMainUiModule;
import com.studio4plus.homerplayer.ui.classic.DaggerClassicMainUiComponent;
import com.studio4plus.homerplayer.util.SimpleDeferred;
import com.studio4plus.homerplayer.util.SimpleFuture;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class MainActivity extends AppCompatActivity implements SpeakerProvider {

    private static final int TTS_CHECK_CODE = 1;

    @SuppressWarnings("FieldCanBeLocal")
    private MainUiComponent mainUiComponent;

    private BatteryStatusIndicator batteryStatusIndicator;
    private @Nullable SimpleDeferred<Speaker> ttsDeferred;
    private OrientationActivityDelegate orientationDelegate;

    @Inject public UiControllerMain controller;
    @Inject public BatteryStatusProvider batteryStatusProvider;
    @Inject public GlobalSettings globalSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mainUiComponent = DaggerClassicMainUiComponent.builder()
                .applicationComponent(HomerPlayerApplication.getComponent(this))
                .classicMainUiModule(new ClassicMainUiModule(this))
                .build();
        mainUiComponent.inject(this);

        controller.onActivityCreated();
        ApplicationLocker.onActivityCreated(this);

        batteryStatusIndicator = new BatteryStatusIndicator(
                (ImageView) findViewById(R.id.batteryStatusIndicator), EventBus.getDefault());

        orientationDelegate = new OrientationActivityDelegate(this, globalSettings);

        View touchEventEater = findViewById(R.id.touchEventEater);
        touchEventEater.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Tell the other views that the event has been handled.
                return true;
            }
        });
    }

    @Override
    protected void onStart() {
        controller.onActivityStart();
        orientationDelegate.onStart();
        batteryStatusProvider.start();
        super.onStart();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Do nothing, this activity takes state from the PlayerService and the AudioBookManager.
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        // Do nothing, this activity takes state from the PlayerService and the AudioBookManager.
    }

    @Override
    protected void onPause() {
        controller.onActivityPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        controller.onActivityStop();
        orientationDelegate.onStop();
        super.onStop();
        batteryStatusProvider.stop();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Start animations.
            batteryStatusIndicator.startAnimations();

            // Set fullscreen mode.
            int visibilitySetting =
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            if (globalSettings.isAnyKioskModeEnabled() &&
                    Build.VERSION.SDK_INT >= 19) {
                visibilitySetting |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            }
            getWindow().getDecorView().setSystemUiVisibility(visibilitySetting);
        }
    }

    @Override
    protected void onDestroy() {
        batteryStatusIndicator.shutdown();
        controller.onActivityDestroy();
        super.onDestroy();
    }

    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TTS_CHECK_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                // success, create the TTS instance
                if (ttsDeferred != null) {
                    ttsDeferred.setResult(new Speaker(this));
                    ttsDeferred = null;
                }
            } else {
                // missing data, install it
                Intent installIntent = new Intent();
                installIntent.setAction(
                        TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                try {
                    startActivity(installIntent);
                } catch (ActivityNotFoundException e) {
                    Log.w("MainActivity", "No activity to handle Text-to-Speech data installation.");
                    if (ttsDeferred != null) {
                        ttsDeferred.setException(e);
                        ttsDeferred = null;
                    }
                }
            }
        }
    }

    @Override
    @NonNull
    public SimpleFuture<Speaker> obtainTts() {
        SimpleDeferred<Speaker> result = ttsDeferred;
        if (ttsDeferred == null) {
            result = new SimpleDeferred<>();
            Intent checkIntent = new Intent();
            checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
            try {
                startActivityForResult(checkIntent, TTS_CHECK_CODE);
                ttsDeferred = result;
            } catch (ActivityNotFoundException e) {
                Log.w("MainActivity", "Text-to-Speech not available");
                result.setException(e);
                // ttsDeferred stays unset because the result is delivered.
            }
        }
        return result;
    }
}
