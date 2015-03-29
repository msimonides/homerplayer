package com.studio4plus.homerplayer.ui;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

class Speaker implements TextToSpeech.OnInitListener {

    private final Locale locale;
    private final TextToSpeech tts;
    private boolean ttsReady;
    private String pendingSpeech;

    Speaker(Context context) {
        this.locale = context.getResources().getConfiguration().locale;
        this.tts = new TextToSpeech(context, this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            ttsReady = true;
            tts.setLanguage(locale);
            tts.speak(pendingSpeech, TextToSpeech.QUEUE_FLUSH, null);
            pendingSpeech = null;
        }
    }

    public void speak(String text) {
        if (ttsReady) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
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
