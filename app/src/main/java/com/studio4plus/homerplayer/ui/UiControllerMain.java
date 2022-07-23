package com.studio4plus.homerplayer.ui;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;

import com.google.common.base.Preconditions;
import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.analytics.AnalyticsTracker;
import com.studio4plus.homerplayer.crashreporting.CrashReporting;
import com.studio4plus.homerplayer.events.AudioBooksChangedEvent;
import com.studio4plus.homerplayer.events.PlaybackFatalErrorEvent;
import com.studio4plus.homerplayer.events.PlaybackStoppedEvent;
import com.studio4plus.homerplayer.model.AudioBook;
import com.studio4plus.homerplayer.model.AudioBookManager;
import com.studio4plus.homerplayer.service.PlaybackService;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

public class UiControllerMain implements ServiceConnection {

    private final @NonNull AppCompatActivity activity;
    private final @NonNull MainUi mainUi;
    private final @NonNull AudioBookManager audioBookManager;
    private final @NonNull EventBus eventBus;
    private final @NonNull AnalyticsTracker analyticsTracker;
    private final @NonNull GlobalSettings globalSettings;
    private final @NonNull UiControllerNoBooks.Factory noBooksControllerFactory;
    private final @NonNull UiControllerBookList.Factory bookListControllerFactory;
    private final @NonNull UiControllerPlayback.Factory playbackControllerFactory;

    private static final int PERMISSION_REQUEST_FOR_BOOK_SCAN = 1;

    private @Nullable PlaybackService playbackService;

    private @NonNull State currentState = new InitState();
    private boolean isRunning = false;

    @Inject
    UiControllerMain(@NonNull AppCompatActivity activity,
                     @NonNull MainUi mainUi,
                     @NonNull AudioBookManager audioBookManager,
                     @NonNull EventBus eventBus,
                     @NonNull AnalyticsTracker analyticsTracker,
                     @NonNull GlobalSettings globalSettings,
                     @NonNull UiControllerNoBooks.Factory noBooksControllerFactory,
                     @NonNull UiControllerBookList.Factory bookListControllerFactory,
                     @NonNull UiControllerPlayback.Factory playbackControllerFactory) {
        this.activity = activity;
        this.mainUi = mainUi;
        this.audioBookManager = audioBookManager;
        this.eventBus = eventBus;
        this.analyticsTracker = analyticsTracker;
        this.globalSettings = globalSettings;
        this.noBooksControllerFactory = noBooksControllerFactory;
        this.bookListControllerFactory = bookListControllerFactory;
        this.playbackControllerFactory = playbackControllerFactory;
    }

    void onActivityCreated() {
        eventBus.register(this);
        Intent serviceIntent = new Intent(activity, PlaybackService.class);
        activity.bindService(serviceIntent, this, Context.BIND_AUTO_CREATE);
    }

    void onActivityStart() {
        Timber.i("UI: onActivityStart");
        scanAudioBookFiles();
    }

    void onActivityResumeFragments() {
        isRunning = true;
        maybeSetInitialState();
    }

    void onActivityPause() {
        Timber.i("UI: onActivityPause, state: %s", currentState.debugName());
        currentState.onActivityPause();
        isRunning = false;
    }

    void onActivityStop() {
        Timber.i("UI: leave state %s (activity stop)", currentState.debugName());
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
        maybeSetInitialState();
    }

    @SuppressWarnings({"UnusedParameters", "UnusedDeclaration"})
    public void onEvent(PlaybackStoppedEvent event) {
        currentState.onPlaybackStop(this);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void onEvent(PlaybackFatalErrorEvent event) {
        mainUi.onPlaybackError(event.uri);
    }

    void playCurrentAudiobook() {
        Preconditions.checkNotNull(currentAudioBook());
        changeState(StateFactory.PLAYBACK);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
        Timber.i("onServiceConnected");
        Preconditions.checkState(playbackService == null);
        playbackService = ((PlaybackService.ServiceBinder) service).getService();
        maybeSetInitialState();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Timber.i("onServiceDisconnected");
        playbackService = null;
    }

    public void onRequestPermissionResult(
            int code, final @NonNull String[] permissions, final @NonNull int[] grantResults) {
        switch(code) {
            case PERMISSION_REQUEST_FOR_BOOK_SCAN:
                if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    scanAudioBookFiles();
                } else {
                    boolean canRetry =
                            ActivityCompat.shouldShowRequestPermissionRationale(
                                    activity, Manifest.permission.READ_EXTERNAL_STORAGE) ||
                            ActivityCompat.shouldShowRequestPermissionRationale(
                                    activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    AlertDialog.Builder dialogBuilder = PermissionUtils.permissionRationaleDialogBuilder(
                            activity, R.string.permission_rationale_scan_audiobooks);
                    if (canRetry) {
                        dialogBuilder.setPositiveButton(
                                R.string.permission_rationale_try_again,
                                (dialogInterface, i) -> PermissionUtils.checkAndRequestPermission(
                                        activity, permissions, PERMISSION_REQUEST_FOR_BOOK_SCAN));
                    } else {
                        analyticsTracker.onPermissionRationaleShown("audiobooksScan");
                        dialogBuilder.setPositiveButton(
                                R.string.permission_rationale_settings,
                                (dialogInterface, i) -> PermissionUtils.openAppSettings(activity));
                    }
                    dialogBuilder.setNegativeButton(
                            R.string.permission_rationale_exit, (dialogInterface, i) -> activity.finish())
                    .create().show();
                }
                break;
            case UiControllerNoBooks.PERMISSION_REQUEST_DOWNLOADS:
                currentState.onRequestPermissionResult(code, grantResults);
                break;
        }
    }

    private void scanAudioBookFiles() {
        boolean needsPermissions = globalSettings.legacyFileAccessMode();
        if (!needsPermissions || PermissionUtils.checkAndRequestPermission(
                activity,
                new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE },
                PERMISSION_REQUEST_FOR_BOOK_SCAN)) {
            audioBookManager.scanFiles();
        }
    }

    private void  maybeSetInitialState() {
        if (currentState instanceof InitState && playbackService != null &&
                audioBookManager.isInitialized() && isRunning) {

            if (playbackService.getState() != PlaybackService.State.IDLE) {
                Preconditions.checkState(hasAnyBooks());
                changeState(StateFactory.PLAYBACK);
            } else if (hasAnyBooks()) {
                changeState(StateFactory.BOOK_LIST);
            } else {
                changeState(StateFactory.NO_BOOKS);
            }
        }
    }

    private boolean hasAnyBooks() {
        return !audioBookManager.getAudioBooks().isEmpty();
    }

    private AudioBook currentAudioBook() {
        return audioBookManager.getCurrentBook();
    }

    private void changeState(StateFactory newStateFactory) {
        if (!isRunning)
            Timber.i("UI(!): changing state while activity is paused");

        Timber.i("UI: leave state: %s", currentState.debugName());
        currentState.onLeaveState();
        Timber.i("UI: create state: %s", newStateFactory.name());
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

        void onRequestPermissionResult(int code, @NonNull int[] grantResults) {
            Preconditions.checkState(false);
        }

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
