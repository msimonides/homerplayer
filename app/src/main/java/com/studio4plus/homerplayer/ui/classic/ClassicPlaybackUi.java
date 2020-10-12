package com.studio4plus.homerplayer.ui.classic;

import android.app.Activity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.HomerPlayerApplication;
import com.studio4plus.homerplayer.ui.PlaybackUi;
import com.studio4plus.homerplayer.ui.SoundBank;
import com.studio4plus.homerplayer.ui.UiControllerPlayback;

import java.util.EnumMap;

import javax.inject.Inject;

public class ClassicPlaybackUi implements PlaybackUi {

    @Inject public SoundBank soundBank;
    @Inject public GlobalSettings globalSettings;

    private final @NonNull FragmentPlayback fragment;
    private final @NonNull ClassicMainUi mainUi;
    private final boolean animateOnInit;

    private @Nullable SoundBank.Sound ffRewindSound;

    ClassicPlaybackUi(
            @NonNull Activity activity, @NonNull ClassicMainUi mainUi, boolean animateOnInit) {
        this.fragment = new FragmentPlayback();
        this.mainUi = mainUi;
        this.animateOnInit = animateOnInit;
        HomerPlayerApplication.getComponent(activity).inject(this);

        if (globalSettings.isFFRewindSoundEnabled())
            ffRewindSound = soundBank.getSound(SoundBank.SoundId.FF_REWIND);
    }

    @Override
    public void initWithController(@NonNull UiControllerPlayback controller) {
        fragment.setController(controller);
        mainUi.showPlayback(fragment, animateOnInit);
    }

    @Override
    public void onPlaybackProgressed(long playbackPositionMs) {
        fragment.onPlaybackProgressed(playbackPositionMs);
    }

    @Override
    public void onPlaybackStopping() {
        fragment.onPlaybackStopping();
    }

    @Override
    public void onFFRewindSpeed(SpeedLevel speedLevel) {
        if (ffRewindSound != null) {
            if (speedLevel == SpeedLevel.STOP) {
                SoundBank.stopTrack(ffRewindSound.track);
            } else {
                int soundPlaybackFactor = SPEED_LEVEL_SOUND_RATE.get(speedLevel);
                ffRewindSound.track.setPlaybackRate(ffRewindSound.sampleRate * soundPlaybackFactor);
                ffRewindSound.track.play();
            }

        }
    }

    @Override
    public void onVolumeChanged(int min, int max, int current) {
        fragment.onVolumeChanged(min, max, current);
    }

    private static final EnumMap<SpeedLevel, Integer> SPEED_LEVEL_SOUND_RATE =
            new EnumMap<>(SpeedLevel.class);

    static {
        // No value for STOP.
        SPEED_LEVEL_SOUND_RATE.put(SpeedLevel.REGULAR, 1);
        SPEED_LEVEL_SOUND_RATE.put(SpeedLevel.FAST, 2);
        SPEED_LEVEL_SOUND_RATE.put(SpeedLevel.FASTEST, 4);
    }
}
