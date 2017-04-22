package com.studio4plus.homerplayer.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.studio4plus.homerplayer.events.AudioBooksChangedEvent;
import com.studio4plus.homerplayer.events.CurrentBookChangedEvent;
import com.studio4plus.homerplayer.model.AudioBookManager;
import com.studio4plus.homerplayer.util.SimpleFuture;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class UiControllerBookList {

    public static class Factory {
        private final @NonNull Context context;
        private final @NonNull AudioBookManager audioBookManager;
        private final @NonNull EventBus eventBus;
        private final @NonNull SpeakerProvider speakerProvider;

        @Inject
        public Factory(@NonNull Context context,
                       @NonNull AudioBookManager audioBookManager,
                       @NonNull EventBus eventBus,
                       @NonNull SpeakerProvider speakerProvider) {
            this.context = context;
            this.audioBookManager = audioBookManager;
            this.eventBus = eventBus;
            this.speakerProvider = speakerProvider;
        }

        public UiControllerBookList create(
                @NonNull UiControllerMain uiControllerMain, @NonNull BookListUi ui) {
            return new UiControllerBookList(
                    context, audioBookManager, speakerProvider, eventBus, uiControllerMain, ui);
        }
    }

    private final @NonNull Context context;
    private final @NonNull AudioBookManager audioBookManager;
    private final @NonNull EventBus eventBus;
    private final @NonNull UiControllerMain uiControllerMain;
    private final @NonNull BookListUi ui;

    private final @NonNull BroadcastReceiver screenOnReceiver;

    private @Nullable SimpleFuture<Speaker> speakerFuture;
    private @Nullable SimpleFuture.Listener<Speaker> speakerListener;
    private @Nullable Speaker speaker;

    private UiControllerBookList(@NonNull Context context,
                                 @NonNull AudioBookManager audioBookManager,
                                 @NonNull SpeakerProvider speakerProvider,
                                 @NonNull EventBus eventBus,
                                 @NonNull UiControllerMain uiControllerMain,
                                 @NonNull BookListUi ui) {
        this.context = context;
        this.audioBookManager = audioBookManager;
        this.eventBus = eventBus;
        this.uiControllerMain = uiControllerMain;
        this.ui = ui;
        this.screenOnReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                speakCurrentTitle();
            }
        };

        speakerFuture = speakerProvider.obtainTts();
        speakerListener = new SimpleFuture.Listener<Speaker>() {
            @Override
            public void onResult(@NonNull Speaker result) {
                onSpeakerObtained(result);
            }
            @Override
            public void onException(@NonNull Throwable t) {
                onSpeakerObtained(null);
            }
        };
        speakerFuture.addListener(speakerListener);

        ui.initWithController(this);

        updateAudioBooks();

        context.registerReceiver(screenOnReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
        eventBus.register(this);
    }

    public void shutdown() {
        stopSpeaking();
        if (speaker != null) {
            speaker.shutdown();
            speaker = null;
        }
        if (speakerFuture != null) {
            Preconditions.checkNotNull(speakerListener);
            speakerFuture.removeListener(speakerListener);
            speakerFuture = null;
        }

        eventBus.unregister(this);
        context.unregisterReceiver(screenOnReceiver);
    }

    public void playCurrentAudiobook() {
        uiControllerMain.playCurrentAudiobook();
    }

    public void changeBook(@NonNull String bookId) {
        audioBookManager.setCurrentBook(bookId);
        speak(audioBookManager.getById(bookId).getTitle());
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEvent(AudioBooksChangedEvent event) {
        updateAudioBooks();
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEvent(CurrentBookChangedEvent event) {
        ui.updateCurrentBook(audioBookManager.getCurrentBookIndex());
    }

    private void onSpeakerObtained(@Nullable Speaker speaker) {
        this.speaker = speaker;
        speakerFuture = null;
        speakerListener = null;
    }

    private void speak(@NonNull String text) {
        if (speaker != null)
            speaker.speak(text);
    }

    private void stopSpeaking() {
        if (speaker != null)
            speaker.stop();
    }

    private void updateAudioBooks() {
        ui.updateBookList(
                audioBookManager.getAudioBooks(),
                audioBookManager.getCurrentBookIndex());
    }

    private void speakCurrentTitle() {
        speak(audioBookManager.getCurrentBook().getTitle());
    }
}
