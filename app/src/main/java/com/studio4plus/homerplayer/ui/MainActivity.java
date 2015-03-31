package com.studio4plus.homerplayer.ui;

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
import android.view.View;
import android.widget.TextView;

import com.studio4plus.homerplayer.HomerPlayerApplication;
import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.model.AudioBook;
import com.studio4plus.homerplayer.model.AudioBookManager;
import com.studio4plus.homerplayer.service.PlaybackService;
import com.studio4plus.homerplayer.widget.MultiTapInterceptor;

import java.util.Arrays;

import fr.castorflex.android.verticalviewpager.VerticalViewPager;

public class MainActivity
        extends FragmentActivity
        implements AudioBookManager.Listener, PlaybackService.StopListener {

    private static final int TTS_CHECK_CODE = 1;

    private VerticalViewPager actionViewPager;
    private Speaker speaker;

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

        startTts();
        TaskLocker.onActivityCreated(this);

        MultiTapInterceptor multiTapInterceptor =
                (MultiTapInterceptor) findViewById(R.id.mainContainer);
        multiTapInterceptor.setOnMultitapListener(new MultiTapInterceptor.Listener() {
            @Override
            public void onMultiTap(View view) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });

        final AudioBookManager audioBookManager = HomerPlayerApplication.getAudioBookManager();
        actionViewPager = (VerticalViewPager) findViewById(R.id.actionPager);
        actionViewPager.setAdapter(
                new ActionPagerAdapter(getSupportFragmentManager(), Page.getAllFragments()));
        actionViewPager.setOnPageChangeListener(new PlayStopTrigger(audioBookManager));

        TextView noBooksPath = (TextView) findViewById(R.id.noBooksPath);
        noBooksPath.setText(audioBookManager.getAudioBooksDirectory().getAbsolutePath());

        audioBookManager.addWeakListener(this);
        Intent serviceIntent = new Intent(this, PlaybackService.class);
        startService(serviceIntent);
        bindService(serviceIntent, playbackServiceConnection, Context.BIND_AUTO_CREATE);
        isPlaybackServiceBound = true;
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (playbackService != null && playbackService.isInPlaybackMode())
            actionViewPager.setCurrentItem(Page.getPosition(Page.PLAYBACK));

    }

    @Override
    protected void onResume() {
        super.onResume();
        final AudioBookManager audioBookManager = HomerPlayerApplication.getAudioBookManager();
        if (audioBookManager.getCurrentBook() != null) {
            speak(audioBookManager.getCurrentBook().getTitle());
        } else {
            speak(getResources().getString(R.string.noBooksMessage));
        }
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

    @Override
    public void onCurrentBookChanged(AudioBook book) {
        speak(book.getTitle());
    }

    @Override
    public void onPlaybackStopped() {
        actionViewPager.setCurrentItem(Page.getPosition(Page.BOOK_LIST), true);
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

    private class PlayStopTrigger extends ViewPager.SimpleOnPageChangeListener {

        private final AudioBookManager audioBookManager;

        private PlayStopTrigger(AudioBookManager audioBookManager) {
            this.audioBookManager = audioBookManager;
        }

        @Override
        public void onPageSelected(int position) {
            if (Page.getByPosition(position) == Page.PLAYBACK) {
                if (playbackService != null && audioBookManager.getCurrentBook() != null) {
                    playbackService.startPlayback(audioBookManager.getCurrentBook());
                    stopSpeaking();

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
            if (playbackService.isInPlaybackMode())
                actionViewPager.setCurrentItem(Page.getPosition(Page.PLAYBACK), false);
            playbackService.registerStopListener(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // TODO: handle this case
            playbackService.unregisterStopListener(MainActivity.this);
            playbackService = null;
        }
    }
}
