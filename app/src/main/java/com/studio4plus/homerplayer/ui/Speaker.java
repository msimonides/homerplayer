package com.studio4plus.homerplayer.ui;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.HashMap;
import java.util.Locale;

class Speaker implements TextToSpeech.OnInitListener {

    // TTS is usually much louder than a regular audio book recording.
    private static final String TTS_VOLUME_ADJUSTMENT = "0.5";

    private final Locale locale;
    private final TextToSpeech tts;
    private final HashMap<String, String> speechParams = new HashMap<>();

    private boolean ttsReady;
    private String pendingSpeech;

    Speaker(Context context) {
        this.locale = context.getResources().getConfiguration().locale;
        this.tts = new TextToSpeech(context, this);
        speechParams.put(TextToSpeech.Engine.KEY_PARAM_VOLUME, TTS_VOLUME_ADJUSTMENT);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            ttsReady = true;
            tts.setLanguage(locale);
            tts.speak(pendingSpeech, TextToSpeech.QUEUE_FLUSH, speechParams);
            pendingSpeech = null;
        }
    }

    public void speak(String text) {
        if (ttsReady) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, speechParams);
        } else {
            pendingSpeech = text;
        }
    }

    public void stop() {
        tts.stop();
        pendingSpeech = null;
    }

    public void shutdown() {
        ttsReady = false;
        tts.shutdown();
    }
}
