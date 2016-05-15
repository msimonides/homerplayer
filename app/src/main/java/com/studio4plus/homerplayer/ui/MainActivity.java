package com.studio4plus.homerplayer.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.crashlytics.android.Crashlytics;
import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.HomerPlayerApplication;
import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.battery.BatteryStatusProvider;
import com.studio4plus.homerplayer.events.AudioBooksChangedEvent;
import com.studio4plus.homerplayer.events.CurrentBookChangedEvent;
import com.studio4plus.homerplayer.events.PlaybackStoppedEvent;
import com.studio4plus.homerplayer.model.AudioBook;
import com.studio4plus.homerplayer.model.AudioBookManager;
import com.studio4plus.homerplayer.service.PlaybackService;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class MainActivity extends BaseActivity {

    private static final int TTS_CHECK_CODE = 1;
    private static final String TAG = "MainActivity";

    private Speaker speaker;

    private final PlaybackServiceConnection playbackServiceConnection =
            new PlaybackServiceConnection();
    private final EventListener eventListener = new EventListener();
    private PlaybackService playbackService;
    private boolean isPlaybackServiceBound;
    private BroadcastReceiver screenOnReceiver;
    private BatteryStatusIndicator batteryStatusIndicator;
    private boolean isRunning;

    @Inject public AudioBookManager audioBookManager;
    @Inject public BatteryStatusProvider batteryStatusProvider;
    @Inject public GlobalSettings globalSettings;

    enum Page {
        NO_BOOKS() {
            @Override
            public Fragment createFragment() {
                return new FragmentNoBooks();
            }
        },

        BOOK_LIST() {
            @Override
            public Fragment createFragment() {
                return new FragmentBookList();
            }
        },
        PLAYBACK() {
            @Override
            public Fragment createFragment() {
                return new FragmentPlayback();
            }
        };

        public abstract Fragment createFragment();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        HomerPlayerApplication.getComponent(this).inject(this);

        startTts();
        ApplicationLocker.onActivityCreated(this);

        batteryStatusIndicator = new BatteryStatusIndicator(
                (ImageView) findViewById(R.id.batteryStatusIndicator), EventBus.getDefault());

        View touchEventEater = findViewById(R.id.touchEventEater);
        touchEventEater.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Tell the other views that the event has been handled.
                return true;
            }
        });

        Intent serviceIntent = new Intent(this, PlaybackService.class);
        startService(serviceIntent);
        bindService(serviceIntent, playbackServiceConnection, Context.BIND_AUTO_CREATE);
        isPlaybackServiceBound = true;
    }

    @Override
    protected void onStart() {
        Page page;
        if (audioBookManager.getAudioBooks().isEmpty()) {
            page = Page.NO_BOOKS;
        } else if (playbackService != null && playbackService.isInPlaybackMode()) {
            page = Page.PLAYBACK;
        } else {
            page = Page.BOOK_LIST;
            registerScreenOnReceiver();
        }

        Crashlytics.log(Log.INFO, TAG, "onStart showing page: " + page);
        showPage(page, true);

        EventBus.getDefault().register(eventListener);
        batteryStatusProvider.start();
        super.onStart();
        isRunning = true;
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
    protected void onStop() {
        isRunning = false;
        super.onStop();
        batteryStatusProvider.stop();
        EventBus.getDefault().unregister(eventListener);
        unregisterScreenOnReceiver();
    }

    @Override
    protected String getScreenName() {
        return "Main";
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
            if (globalSettings.isSimpleKioskModeEnabled() &&
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
        if (isPlaybackServiceBound) {
            unbindService(playbackServiceConnection);
            isPlaybackServiceBound = false;
        }
        batteryStatusIndicator.shutdown();
        stopTts();
        super.onDestroy();
    }

    public void startPlayback(String bookId) {
        AudioBook book = audioBookManager.getById(bookId);
        if (playbackService != null && book != null) {
            playbackService.startPlayback(book);
            stopSpeaking();

            showPage(Page.PLAYBACK);
        }
    }

    public void stopPlayback() {
        if (playbackService != null) {
            playbackService.stopPlayback();
            // Page is changed in response to PlaybackStoppedEvent.
        } else {
            showPage(Page.BOOK_LIST);
        }
    }

    public void pauseForRewind() {
        if (playbackService != null)
            playbackService.pauseForRewind();
    }

    public void resumeFromRewind(long newTotalPositionMs) {
        if (playbackService != null)
            playbackService.resumeFromRewind(newTotalPositionMs);
    }

    public void requestElapsedTimeSyncEvent() {
        if (playbackService != null)
            playbackService.requestElapsedTimeSyncEvent();
    }

    private void showPage(Page page) {
        showPage(page, false);
    }

    private void showPage(Page page, boolean suppressAnimation) {
        Crashlytics.log(Log.INFO, TAG, "Switching to page: " + page);
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (!suppressAnimation)
            transaction.setCustomAnimations(R.animator.flip_right_in, R.animator.flip_right_out);
        transaction.replace(R.id.mainContainer, page.createFragment());
        transaction.commit();
    }

    private void speakCurrentTitle() {
        if (audioBookManager.getCurrentBook() != null) {
            speak(audioBookManager.getCurrentBook().getTitle());
        } else {
            speak(getResources().getString(R.string.noBooksMessage));
        }
    }

    private void speak(String text) {
        if (speaker != null)
            speaker.speak(text);
    }

    private void stopSpeaking() {
        if (speaker != null)
            speaker.stop();
    }

    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TTS_CHECK_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                // success, create the TTS instance
                speaker = new Speaker(this);
            } else {
                // missing data, install it
                Intent installIntent = new Intent();
                installIntent.setAction(
                        TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                try {
                    startActivity(installIntent);
                } catch (ActivityNotFoundException e) {
                    Log.w("MainActivity", "No activity to handle Text-to-Speech data installation.");
                }
            }
        }
    }

    private void startTts() {
        Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        try {
            startActivityForResult(checkIntent, TTS_CHECK_CODE);
        } catch (ActivityNotFoundException e) {
            Log.w("MainActivity", "Text-to-Speech not available");
        }
    }

    private void stopTts() {
        if (speaker != null) {
            speaker.shutdown();
            speaker = null;
        }
    }

    private void registerScreenOnReceiver() {
        screenOnReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                speakCurrentTitle();
            }
        };
        registerReceiver(screenOnReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
    }

    private void unregisterScreenOnReceiver() {
        if (screenOnReceiver != null) {
            unregisterReceiver(screenOnReceiver);
            screenOnReceiver = null;
        }
    }

    private class PlaybackServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            playbackService = ((PlaybackService.ServiceBinder) service).getService();
            Crashlytics.log(Log.INFO, TAG, "PlaybackService conected, is in playback mode: " +
                    playbackService.isInPlaybackMode());
            if (playbackService.isInPlaybackMode())
                showPage(Page.PLAYBACK, true);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            playbackService = null;
            Crashlytics.log(Log.INFO, TAG, "PlaybackService disconected.");
        }
    }

    /**
     * A subclass for handling EventBus events.
     *
     * The MainActivity object cannot be registered with EventBus because it declares
     * methods that are not available on some of the Android versions supported by the
     * application (namely onSaveInstanceState with its ParcelableBundle argument that
     * causes EventBus's reflection code to throw exceptions).
     */
    private class EventListener {

        @SuppressWarnings("UnusedDeclaration")
        public void onEvent(CurrentBookChangedEvent event) {
            speak(event.audioBook.getTitle());
        }

        @SuppressWarnings({"UnusedParameters", "UnusedDeclaration"})
        public void onEvent(PlaybackStoppedEvent ignored) {
            Crashlytics.log(Log.INFO, TAG, "Playback stopped");
            if (isRunning)
                showPage(Page.BOOK_LIST);
        }

        @SuppressWarnings({"UnusedParameters", "UnusedDeclaration"})
        public void onEvent(AudioBooksChangedEvent event) {
            Crashlytics.log(Log.INFO, TAG, "AudioBooks changed, new count: " +
                    audioBookManager.getAudioBooks().size());
            if (isRunning) {
                if (audioBookManager.getAudioBooks().isEmpty()) {
                    showPage(Page.NO_BOOKS);
                } else {
                    showPage(Page.BOOK_LIST);
                }
            }
        }
    }
}
