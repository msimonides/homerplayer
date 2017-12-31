package com.studio4plus.homerplayer.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.crashlytics.android.Crashlytics;
import com.google.common.base.Preconditions;
import com.studio4plus.homerplayer.analytics.AnalyticsTracker;
import com.studio4plus.homerplayer.events.AudioBooksChangedEvent;
import com.studio4plus.homerplayer.events.PlaybackStoppedEvent;
import com.studio4plus.homerplayer.model.AudioBook;
import com.studio4plus.homerplayer.model.AudioBookManager;
import com.studio4plus.homerplayer.service.PlaybackService;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class UiControllerMain implements ServiceConnection {

    private final @NonNull Activity activity;
    private final @NonNull MainUi mainUi;
    private final @NonNull AudioBookManager audioBookManager;
    private final @NonNull EventBus eventBus;
    private final @NonNull AnalyticsTracker analyticsTracker;
    private final @NonNull UiControllerNoBooks.Factory noBooksControllerFactory;
    private final @NonNull UiControllerBookList.Factory bookListControllerFactory;
    private final @NonNull UiControllerPlayback.Factory playbackControllerFactory;

    private @Nullable PlaybackService playbackService;

    private @NonNull State currentState = new InitState();

    @Inject
    UiControllerMain(@NonNull Activity activity,
                     @NonNull MainUi mainUi,
                     @NonNull AudioBookManager audioBookManager,
                     @NonNull EventBus eventBus,
                     @NonNull AnalyticsTracker analyticsTracker,
                     @NonNull UiControllerNoBooks.Factory noBooksControllerFactory,
                     @NonNull UiControllerBookList.Factory bookListControllerFactory,
                     @NonNull UiControllerPlayback.Factory playbackControllerFactory) {
        this.activity = activity;
        this.mainUi = mainUi;
        this.audioBookManager = audioBookManager;
        this.eventBus = eventBus;
        this.analyticsTracker = analyticsTracker;
        this.noBooksControllerFactory = noBooksControllerFactory;
        this.bookListControllerFactory = bookListControllerFactory;
        this.playbackControllerFactory = playbackControllerFactory;
    }

    void onActivityCreated() {
        eventBus.register(this);
        Intent serviceIntent = new Intent(activity, PlaybackService.class);
        activity.startService(serviceIntent);
        activity.bindService(serviceIntent, this, Context.BIND_AUTO_CREATE);
    }

    void onActivityStart() {
        Crashlytics.log("activity start");
        if (playbackService != null)
            setInitialState();
    }

    void onActivityPause() {
        currentState.onActivityPause();
    }

    void onActivityStop() {
        Crashlytics.log("UI: leave state " + currentState.debugName() + " (activity stop)");
        currentState.onLeaveState();
        currentState = new InitState();
    }

    void onActivityDestroy() {
        activity.unbindService(this);
        eventBus.unregister(this);
    }

    @SuppressWarnings({"UnusedParameters", "UnusedDeclaration"})
    public void onEvent(AudioBooksChangedEvent event) {
        currentState.onBooksChanged(this);
    }

    @SuppressWarnings({"UnusedParameters", "UnusedDeclaration"})
    public void onEvent(PlaybackStoppedEvent event) {
        currentState.onPlaybackStop(this);
    }

    void playCurrentAudiobook() {
        Preconditions.checkNotNull(currentAudioBook());
        changeState(StateFactory.PLAYBACK);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
        Crashlytics.log("onServiceConnected");
        Preconditions.checkState(playbackService == null);
        playbackService = ((PlaybackService.ServiceBinder) service).getService();
        if (currentState instanceof InitState)
            setInitialState();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        playbackService = null;
    }

    private void setInitialState() {
        Preconditions.checkNotNull(playbackService);
        if (playbackService.getState() != PlaybackService.State.IDLE) {
            Preconditions.checkState(hasAnyBooks());
            changeState(StateFactory.PLAYBACK);
        } else if (hasAnyBooks()) {
            changeState(StateFactory.BOOK_LIST);
        } else {
            changeState(StateFactory.NO_BOOKS);
        }
    }

    private boolean hasAnyBooks() {
        return !audioBookManager.getAudioBooks().isEmpty();
    }

    private AudioBook currentAudioBook() {
        return audioBookManager.getCurrentBook();
    }

    private void changeState(StateFactory newStateFactory) {
        Crashlytics.log("UI: leave state: " + currentState.debugName());
        currentState.onLeaveState();
        Crashlytics.log("UI: create state: " + newStateFactory.name());
        currentState = newStateFactory.create(this, currentState);
    }

    @NonNull
    private UiControllerBookList showBookList(boolean animate) {
        analyticsTracker.onBookListDisplayed();
        BookListUi bookListUi = mainUi.switchToBookList(animate);
        return bookListControllerFactory.create(this, bookListUi);
    }

    @NonNull
    private UiControllerNoBooks showNoBooks(boolean animate) {
        NoBooksUi noBooksUi = mainUi.switchToNoBooks(animate);
        return noBooksControllerFactory.create(noBooksUi);
    }

    @NonNull
    private UiControllerPlayback showPlayback(boolean animate) {
        Preconditions.checkNotNull(playbackService);
        PlaybackUi playbackUi = mainUi.switchToPlayback(animate);
        return playbackControllerFactory.create(playbackService, playbackUi);
    }

    private enum StateFactory {
        NO_BOOKS {
            @Override
            State create(@NonNull UiControllerMain mainController, @NonNull State previousState) {
                return new NoBooksState(mainController, previousState);
            }
        },
        BOOK_LIST {
            @Override
            State create(@NonNull UiControllerMain mainController, @NonNull State previousState) {
                return new BookListState(mainController, previousState);
            }
        },
        PLAYBACK {
            @Override
            State create(@NonNull UiControllerMain mainController, @NonNull State previousState) {
                return new PlaybackState(mainController, previousState);
            }
        };

        abstract State create(
                @NonNull UiControllerMain mainController, @NonNull State previousState);
    }

    private static abstract class State {
        abstract void onLeaveState();

        void onPlaybackStop(@NonNull UiControllerMain mainController) {
            Preconditions.checkState(false);
        }

        void onBooksChanged(@NonNull UiControllerMain mainController) {
            Preconditions.checkState(false);
        }

        void onActivityPause() {}

        abstract @NonNull String debugName();
    }

    private static class InitState extends State {
        @Override
        void onPlaybackStop(@NonNull UiControllerMain mainController) {}

        @Override
        void onBooksChanged(@NonNull UiControllerMain mainController) {}

        @Override
        void onLeaveState() {}

        @Override
        @NonNull String debugName() { return "InitState"; }
    }

    private static class NoBooksState extends State {
        private @NonNull UiControllerNoBooks controller;

        NoBooksState(@NonNull UiControllerMain mainController, @NonNull State previousState) {
            this.controller = mainController.showNoBooks(!(previousState instanceof InitState));
        }

        @Override
        public void onLeaveState() {
            controller.shutdown();
        }

        @Override
        public void onBooksChanged(@NonNull UiControllerMain mainController) {
            if (mainController.hasAnyBooks())
                mainController.changeState(StateFactory.BOOK_LIST);
        }

        @Override
        @NonNull String debugName() { return "NoBooksState"; }
    }

    private static class BookListState extends State {
        private @NonNull UiControllerBookList controller;

        BookListState(@NonNull UiControllerMain mainController, @NonNull State previousState) {
            controller = mainController.showBookList(!(previousState instanceof InitState));
        }

        @Override
        void onLeaveState() {
            controller.shutdown();
        }

        @Override
        void onBooksChanged(@NonNull UiControllerMain mainController) {
            if (!mainController.hasAnyBooks())
                mainController.changeState(StateFactory.NO_BOOKS);
        }

        @Override
        @NonNull String debugName() { return "BookListState"; }

    }

    private static class PlaybackState extends State {
        private @NonNull UiControllerPlayback controller;
        private @Nullable AudioBook playingAudioBook;

        PlaybackState(@NonNull UiControllerMain mainController, @NonNull State previousState) {
            controller = mainController.showPlayback(!(previousState instanceof InitState));
            playingAudioBook = mainController.currentAudioBook();
            if (!(previousState instanceof InitState)) {
                Preconditions.checkNotNull(playingAudioBook);
                controller.startPlayback(playingAudioBook);
            }
        }

        @Override
        void onActivityPause() {
            Preconditions.checkNotNull(controller);
            controller.stopRewindIfActive();
        }

        @Override
        void onLeaveState() {
            Preconditions.checkNotNull(controller);
            controller.shutdown();
        }

        @Override
        void onPlaybackStop(@NonNull UiControllerMain mainController) {
            mainController.changeState(mainController.hasAnyBooks()
                    ? StateFactory.BOOK_LIST
                    : StateFactory.NO_BOOKS);
        }

        @Override
        void onBooksChanged(@NonNull UiControllerMain mainController) {
            if (playingAudioBook != null &&
                    playingAudioBook != mainController.currentAudioBook()) {
                controller.stopPlayback();
                playingAudioBook = null;
            }
        }

        @Override
        @NonNull String debugName() { return "PlaybackState"; }
    }

}
