package com.studio4plus.homerplayer.ui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.studio4plus.homerplayer.HomerPlayerApplication;
import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.events.CurrentBookChangedEvent;
import com.studio4plus.homerplayer.events.PlaybackStoppedEvent;
import com.studio4plus.homerplayer.model.AudioBook;
import com.studio4plus.homerplayer.model.AudioBookManager;
import com.studio4plus.homerplayer.service.PlaybackService;
import com.studio4plus.homerplayer.widget.MultiTapInterceptor;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class MainActivity extends BaseActivity {

    private static final int TTS_CHECK_CODE = 1;

    private Speaker speaker;

    private final PlaybackServiceConnection playbackServiceConnection =
            new PlaybackServiceConnection();
    private PlaybackService playbackService;
    private boolean isPlaybackServiceBound;
    private BroadcastReceiver screenOnReceiver;

    @Inject public AudioBookManager audioBookManager;

    enum Page {
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

        MultiTapInterceptor multiTapInterceptor =
                (MultiTapInterceptor) findViewById(R.id.mainContainer);
        multiTapInterceptor.setOnMultitapListener(new MultiTapInterceptor.Listener() {
            @Override
            public void onMultiTap(View view) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });

        View touchEventEater = findViewById(R.id.touchEventEater);
        touchEventEater.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Tell the other views that the event has been handled.
                return true;
            }
        });

        TextView noBooksPath = (TextView) findViewById(R.id.noBooksPath);
        noBooksPath.setText(audioBookManager.getAudioBooksDirectory().getAbsolutePath());

        Intent serviceIntent = new Intent(this, PlaybackService.class);
        startService(serviceIntent);
        bindService(serviceIntent, playbackServiceConnection, Context.BIND_AUTO_CREATE);
        isPlaybackServiceBound = true;
    }

    @Override
    protected void onStart() {
        if (playbackService != null && playbackService.isInPlaybackMode()) {
            showPage(Page.PLAYBACK, true);
        } else {
            showPage(Page.BOOK_LIST, true);
            registerScreenOnReceiver();
        }

        EventBus.getDefault().register(this);

        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        unregisterScreenOnReceiver();
    }

    @Override
    protected void onDestroy() {
        if (isPlaybackServiceBound) {
            unbindService(playbackServiceConnection);
            isPlaybackServiceBound = false;
        }
        stopTts();
        super.onDestroy();
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEvent(CurrentBookChangedEvent event) {
        speak(event.audioBook.getTitle());
    }

    @SuppressWarnings({"UnusedParameters", "UnusedDeclaration"})
    public void onEvent(PlaybackStoppedEvent ignored) {
        showPage(Page.BOOK_LIST);
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
        if (playbackService != null)
            playbackService.stopPlayback();

        showPage(Page.BOOK_LIST);
    }

    private void showPage(Page page) {
        showPage(page, false);
    }

    private void showPage(Page page, boolean allowStateLoss) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.mainContainer, page.createFragment());
        if (allowStateLoss)
            transaction.commitAllowingStateLoss();
        else
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
                startActivity(installIntent);
            }
        }
    }

    private void startTts() {
        Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, TTS_CHECK_CODE);
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
            if (playbackService.isInPlaybackMode())
                showPage(Page.PLAYBACK);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            playbackService = null;
        }
    }
}
