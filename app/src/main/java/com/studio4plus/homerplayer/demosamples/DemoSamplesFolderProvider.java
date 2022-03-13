package com.studio4plus.homerplayer.demosamples;

import android.content.Context;

import java.io.File;

import javax.inject.Inject;

public class DemoSamplesFolderProvider {

    private static final String DEMO_FOLDER = "audiobook_samples";

    private final Context context;

    @Inject
    public DemoSamplesFolderProvider(Context context) {
        this.context = context;
    }

    public File demoFolder() {
        File cacheFolder = context.getCacheDir();
        File demoSamplesFolder = new File(cacheFolder, DEMO_FOLDER);
        if (!demoSamplesFolder.exists()) {
            demoSamplesFolder.mkdirs();
        }
        return demoSamplesFolder;
    }
}
