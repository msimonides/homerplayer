package com.studio4plus.homerplayer;

import java.util.HashMap;
import java.util.Map;
import android.content.Context;
import android.net.Uri;
import androidx.core.os.ConfigurationCompat;

public class SamplesMap {
    private static final String EN_SAMPLES_URL = "https://homer-player.firebaseapp.com/samples.zip";
    private static final String FR_SAMPLES_URL = "https://cdn.glitch.com/5b1a0f47-cb72-4db8-8ea3-9aa2d6fbc0c5%2Fsamples-fr.zip?v=1601809045024";

    private Map<String, String> map;

    public SamplesMap(){
        this.map = new HashMap<String, String>();
        this.map.put("en", EN_SAMPLES_URL);
        this.map.put("fr", FR_SAMPLES_URL);
    }

    public Uri getSamples(String language){
        String url;
        if(this.map.containsKey(language)){
            url = this.map.get(language);
        } else {
            url = this.map.get("en");
        }
        return Uri.parse(url);
    }
}
