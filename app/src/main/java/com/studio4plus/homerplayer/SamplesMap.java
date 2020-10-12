package com.studio4plus.homerplayer;

import java.util.HashMap;
import java.util.Map;

import android.net.Uri;

import javax.inject.Inject;

public class SamplesMap {
    private static final String DEFAULT_LOCALE = "en";
    private static final String EN_SAMPLES_URL = "https://homer-player.firebaseapp.com/samples.zip";
    private static final String FR_SAMPLES_URL = "https://homer-player.firebaseapp.com/samples-fr.zip";

    private Map<String, String> map;

    @Inject
    public SamplesMap(){
        this.map = new HashMap<String, String>();
        // Language codes must match those returned by Locale.getLanguage(), which may not be the ones in the newest ISO 639 standard.
        this.map.put("en", EN_SAMPLES_URL);
        this.map.put("fr", FR_SAMPLES_URL);
    }

    public Uri getSamples(String language){
        String url;
        if(this.map.containsKey(language)){
            url = this.map.get(language);
        } else {
            url = this.map.get(DEFAULT_LOCALE);
        }
        return Uri.parse(url);
    }
}
