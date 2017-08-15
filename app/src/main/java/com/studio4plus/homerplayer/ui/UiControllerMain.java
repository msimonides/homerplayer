package com.studio4plus.homerplayer.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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

    private @NonNull State currentState = State.INIT;

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
        if (playbackService != null)
            setInitialState();
    }

    void onActivityPause() {
        currentState.onActivityPause();
    }

    void onActivityStop() {
        currentState.onLeaveState();
        currentState = State.INIT;
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
        changeState(State.PLAYBACK);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
        playbackService = ((PlaybackService.ServiceBinder) service).getService();
        if (currentState == State.INIT)
            setInitialState();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        playbackService = null;
    }

    private void setInitialState() {
        Preconditions.checkNotNull(playbackService);
        if (playbackService.getState() != PlaybackService.State.IDLE) {
            changeState(State.PLAYBACK);
        } else if (hasAnyBooks()) {
            changeState(State.BOOK_LIST);
        } else {
            changeState(State.NO_BOOKS);
        }
    }

    private boolean hasAnyBooks() {
        return !audioBookManager.getAudioBooks().isEmpty();
    }

    private AudioBook currentAudioBook() {
        return audioBookManager.getCurrentBook();
    }

    private void changeState(State newState) {
        currentState.onLeaveState();
        State previousState = currentState;
        currentState = newState;
        currentState.onEnterState(this, previousState);
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

    private enum State {
        INIT {
            @Override
            void onEnterState(@NonNull UiControllerMain mainController,
                              @NonNull State previousState) {
                Preconditions.checkState(false);  // Never called.
            }

            @Override
            void onLeaveState() {
            }
        },
        NO_BOOKS {
            private @Nullable UiControllerNoBooks controller;

            @Override
            public void onEnterState(@NonNull UiControllerMain mainController,
                                     @NonNull State previousState) {
                controller = mainController.showNoBooks(previousState != INIT);
            }

            @Override
            public void onLeaveState() {
                Preconditions.checkNotNull(controller);
                controller.shutdown();
                controller = null;
            }

            @Override
            public void onBooksChanged(@NonNull UiControllerMain mainController) {
                if (mainController.hasAnyBooks())
                    mainController.changeState(BOOK_LIST);
            }
        },
        BOOK_LIST {
            private @Nullable UiControllerBookList controller;

            @Override
            void onEnterState(@NonNull UiControllerMain mainController,
                              @NonNull State previousState) {
                controller = mainController.showBookList(previousState != INIT);
            }

            @Override
            void onLeaveState() {
                Preconditions.checkNotNull(controller);
                controller.shutdown();
                controller = null;
            }

            @Override
            void onBooksChanged(@NonNull UiControllerMain mainController) {
                if (!mainController.hasAnyBooks())
                    mainController.changeState(NO_BOOKS);
            }
        },
        PLAYBACK {
            private @Nullable UiControllerPlayback controller;
            private @Nullable AudioBook playingAudioBook;

            @Override
            void onEnterState(@NonNull UiControllerMain mainController,
                              @NonNull State previousState) {
                controller = mainController.showPlayback(previousState != INIT);
                playingAudioBook = mainController.currentAudioBook();
                if (previousState != INIT) {
                    Preconditions.checkNotNull(mainController.currentAudioBook());
                    controller.startPlayback(mainController.currentAudioBook());
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
                controller = null;
                playingAudioBook = null;
            }

            @Override
            void onPlaybackStop(@NonNull UiControllerMain mainController) {
                mainController.changeState(mainController.hasAnyBooks() ? BOOK_LIST : NO_BOOKS);
            }

            @Override
            void onBooksChanged(@NonNull UiControllerMain mainController) {
                Preconditions.checkNotNull(controller);
                if (playingAudioBook != null &&
                        playingAudioBook != mainController.currentAudioBook()) {
                    controller.stopPlayback();
                    playingAudioBook = null;
                }
            }
        };

        abstract void onEnterState(@NonNull UiControllerMain mainController,
                                   @NonNull State previousState);
        abstract void onLeaveState();

        void onPlaybackStop(@NonNull UiControllerMain mainController) {
            Preconditions.checkState(false);
        }

        void onBooksChanged(@NonNull UiControllerMain mainController) {
            Preconditions.checkState(false);
        }

        void onActivityPause() {}
    }

}
