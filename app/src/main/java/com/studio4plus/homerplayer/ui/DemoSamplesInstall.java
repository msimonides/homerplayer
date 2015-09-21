package com.studio4plus.homerplayer.ui;

import com.crashlytics.android.Crashlytics;
import com.studio4plus.homerplayer.model.DemoSamplesInstaller;
import com.studio4plus.homerplayer.util.Downloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class DemoSamplesInstall {

    public static boolean downloadAndInstall(
            Downloader downloader, DemoSamplesInstaller installer) {
        File destinationFile;
        FileOutputStream destination;
        try {
            destinationFile = File.createTempFile("temp", null);
            destination = new FileOutputStream(destinationFile);
        } catch(IOException e) {
            e.printStackTrace();
            Crashlytics.logException(e);
            return false;
        }

        boolean success = downloader.download(destination);

        if (success)
            success = installer.installBooksFromZip(destinationFile);

        //noinspection ResultOfMethodCallIgnored
        destinationFile.delete();

        return success;
    }
}
