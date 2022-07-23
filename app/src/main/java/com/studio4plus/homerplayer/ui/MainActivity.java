package com.studio4plus.homerplayer.ui;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.speech.tts.TextToSpeech;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.HomerPlayerApplication;
import com.studio4plus.homerplayer.KioskModeSwitcher;
import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.battery.BatteryStatusProvider;
import com.studio4plus.homerplayer.concurrency.SimpleDeferred;
import com.studio4plus.homerplayer.ui.classic.ClassicMainUiModule;
import com.studio4plus.homerplayer.ui.classic.DaggerClassicMainUiComponent;
import com.studio4plus.homerplayer.concurrency.SimpleFuture;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements SpeakerProvider {

    private static final int TTS_CHECK_CODE = 1;
    private static final String KIOSK_MODE_ENABLE_ACTION = "KioskModeEnable";
    private static final String ENABLE_EXTRA = "Enable";

    @SuppressWarnings("FieldCanBeLocal")
    private MainUiComponent mainUiComponent;

    private BatteryStatusIndicator batteryStatusIndicator;
    private @Nullable SimpleDeferred<Speaker> ttsDeferred;
    private OrientationActivityDelegate orientationDelegate;

    @Inject public UiControllerMain controller;
    @Inject public BatteryStatusProvider batteryStatusProvider;
    @Inject public EventBus eventBus;
    @Inject public GlobalSettings globalSettings;
    @Inject public KioskModeHandler kioskModeHandler;
    @Inject public KioskModeSwitcher kioskModeSwitcher;

    private final static long SUPPRESSED_BACK_MESSAGE_DELAY_NANO = TimeUnit.SECONDS.toNanos(2);
    private long lastSuppressedBackTimeNano = 0;
    private boolean isInForeground = false;

    @Nullable
    private ColorTheme currentTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainUiComponent = DaggerClassicMainUiComponent.builder()
                .applicationComponent(HomerPlayerApplication.getComponent(this))
                .activityModule(new ActivityModule(this))
                .classicMainUiModule(new ClassicMainUiModule(this))
                .build();
        mainUiComponent.inject(this);

        setTheme(globalSettings.colorTheme());
        setContentView(R.layout.main_activity);

        controller.onActivityCreated();

        batteryStatusIndicator = new BatteryStatusIndicator(
                findViewById(R.id.batteryStatusIndicator), eventBus, batteryStatusProvider.getLastStatus());

        orientationDelegate = new OrientationActivityDelegate(this, globalSettings);

        View touchEventEater = findViewById(R.id.touchEventEater);
        touchEventEater.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Tell the other views that the event has been handled.
                return true;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // onStart must be called before the UI controller can manipulate fragments.
        controller.onActivityStart();
        orientationDelegate.onStart();
        batteryStatusProvider.start();
        kioskModeHandler.onActivityStart(this);
        handleIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        isInForeground = true;
        ColorTheme theme = globalSettings.colorTheme();
        if (currentTheme != theme) {
            setTheme(theme);
            recreate();
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        controller.onActivityResumeFragments();
    }

    @SuppressLint("MissingSuperCall")
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
        // Call super.onPause() first. It may, among other things, call onResumeFragments(), so
        // calling super.onPause() before controller.onActivityPause() is necessary to ensure that
        // controller.onActivityResumeFragments() is called in the right order.
        super.onPause();
        isInForeground = false;
        controller.onActivityPause();
    }

    @Override
    protected void onStop() {
        controller.onActivityStop();
        orientationDelegate.onStop();
        super.onStop();
        batteryStatusProvider.stop();
    }

    @Override
    public void onBackPressed() {
        if (globalSettings.isAnyKioskModeEnabled()) {
            long now = System.nanoTime();
            if (now - lastSuppressedBackTimeNano < SUPPRESSED_BACK_MESSAGE_DELAY_NANO) {
                Toast.makeText(this, R.string.back_suppressed_by_kiosk, Toast.LENGTH_SHORT)
                        .show();
            }
            lastSuppressedBackTimeNano = now;
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && isInForeground) {
            // Start animations.
            batteryStatusIndicator.startAnimations();

            kioskModeHandler.onFocusGained(this);
        }
    }

    @Override
    protected void onDestroy() {
        batteryStatusIndicator.shutdown();
        controller.onActivityDestroy();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // A call with no grantResults means the dialog has been closed without any user decision.
        if (grantResults.length > 0)
            controller.onRequestPermissionResult(requestCode, permissions, grantResults);
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
                    Timber.w(e, "No activity to handle Text-to-Speech data installation.");
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
                Timber.w("Text-to-Speech not available");
                result.setException(e);
                // ttsDeferred stays unset because the result is delivered.
            }
        }
        return result;
    }

    private void handleIntent(Intent intent) {
        if (intent != null && KIOSK_MODE_ENABLE_ACTION.equals(intent.getAction())) {
            if (kioskModeSwitcher.isLockTaskPermitted()) {
                boolean enable = intent.getBooleanExtra(ENABLE_EXTRA, false);
                if (globalSettings.isFullKioskModeEnabled() != enable) {
                    globalSettings.setFullKioskModeEnabledNow(enable);
                    kioskModeSwitcher.onFullKioskModeEnabled(this, enable);

                    // For some reason clearing the preferred Home activity only takes effect if the
                    // application exits (finishing the activity doesn't help).
                    // This issue doesn't happen when disabling the kiosk mode from the settings
                    // screen and I'm out of ideas.
                    if (!enable) {
                        new Handler(getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                System.exit(0);
                            }
                        }, 500);
                    }
                }
            }
        }
    }

    private void setTheme(@NonNull ColorTheme theme) {
        currentTheme = theme;
        setTheme(theme.styleId);
    }
}
