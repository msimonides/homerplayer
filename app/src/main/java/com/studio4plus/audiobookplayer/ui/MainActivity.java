package com.studio4plus.audiobookplayer.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;

import com.studio4plus.audiobookplayer.AudioBookPlayerApplication;
import com.studio4plus.audiobookplayer.R;
import com.studio4plus.audiobookplayer.model.AudioBook;
import com.studio4plus.audiobookplayer.model.AudioBookManager;
import com.studio4plus.audiobookplayer.service.PlaybackService;

import java.util.Arrays;

import fr.castorflex.android.verticalviewpager.VerticalViewPager;

public class MainActivity
        extends FragmentActivity implements TextToSpeech.OnInitListener, AudioBookManager.Listener {

    private static final int TTS_CHECK_CODE = 1;

    private VerticalViewPager actionViewPager;
    private TextToSpeech tts;
    private boolean ttsReady;

    private final PlaybackServiceConnection playbackServiceConnection =
            new PlaybackServiceConnection();
    private PlaybackService playbackService;
    private boolean isPlaybackServiceBound;

    enum Page {
        BOOK_LIST(new FragmentBookList()),
        PLAYBACK(new FragmentPlayback());

        public final Fragment fragment;

        Page(Fragment fragment) {
            this.fragment = fragment;
        }

        public static Fragment[] getAllFragments() {
            Page[] values = values();
            Fragment[] fragments = new Fragment[values.length];
            for (int i = 0; i < values.length; ++i)
                fragments[i] = getByPosition(i).fragment;
            return fragments;
        }

        public static Page getByPosition(int position) {
            return values()[position];
        }

        public static int getPosition(Page page) {
            return Arrays.asList(values()).indexOf(page);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        ttsReady = false;

        final AudioBookManager audioBookManager = AudioBookPlayerApplication.getAudioBookManager();
        actionViewPager = (VerticalViewPager) findViewById(R.id.actionPager);
        actionViewPager.setAdapter(
                new ActionPagerAdapter(getSupportFragmentManager(), Page.getAllFragments()));
        actionViewPager.setOnPageChangeListener(new PlayStopTrigger(audioBookManager));

        audioBookManager.addWeakListener(this);
        Intent serviceIntent = new Intent(this, PlaybackService.class);
        startService(serviceIntent);
        bindService(serviceIntent, playbackServiceConnection, Context.BIND_AUTO_CREATE);
        isPlaybackServiceBound = true;
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (playbackService != null && playbackService.isPlaying())
            actionViewPager.setCurrentItem(Page.getPosition(Page.PLAYBACK));

        Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, TTS_CHECK_CODE);
    }

    @Override
    protected void onStop() {
        if (tts != null) {
            ttsReady = false;
            tts.shutdown();
            tts = null;
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (isPlaybackServiceBound) {
            unbindService(playbackServiceConnection);
            isPlaybackServiceBound = false;
        }
        super.onDestroy();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            ttsReady = true;
            tts.setLanguage(getResources().getConfiguration().locale);
        }
    }

    @Override
    public void onCurrentBookChanged(AudioBook book) {
        speak(book.getTitle());
    }

    private void speak(String text) {
        if (ttsReady) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        if (requestCode == TTS_CHECK_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                // success, create the TTS instance
                tts = new TextToSpeech(this, this);
            } else {
                // missing data, install it
                Intent installIntent = new Intent();
                installIntent.setAction(
                        TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installIntent);
            }
        }
    }

    private class PlayStopTrigger extends ViewPager.SimpleOnPageChangeListener {

        private final AudioBookManager audioBookManager;

        private PlayStopTrigger(AudioBookManager audioBookManager) {
            this.audioBookManager = audioBookManager;
        }

        @Override
        public void onPageSelected(int position) {
            if (Page.getByPosition(position) == Page.PLAYBACK) {
                if (playbackService != null) {
                    playbackService.startPlayback(audioBookManager.getCurrentBook());
                    if (tts != null)
                        tts.stop();
                }
            } else {
                if (playbackService != null) {
                    playbackService.stopPlayback();
                }
            }
        }
    }

    private class PlaybackServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            playbackService = ((PlaybackService.ServiceBinder) service).getService();
            if (playbackService.isPlaying())
                actionViewPager.setCurrentItem(Page.getPosition(Page.PLAYBACK));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // TODO: handle this case
            playbackService = null;
        }
    }
}
