package com.studio4plus.homerplayer.demosamples;

import androidx.annotation.NonNull;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import timber.log.Timber;

public class Unzipper {

    public static boolean unzip(@NonNull InputStream zipStream, @NonNull File destinationFolder) {
        try(ZipInputStream zip = new ZipInputStream(zipStream)) {
            String canonicalDstPath = destinationFolder.getCanonicalFile().toString();
            ZipEntry entry = zip.getNextEntry();
            while (entry != null) {
                File file = (new File(canonicalDstPath, entry.getName())).getCanonicalFile();
                if (!file.toString().startsWith(canonicalDstPath)) {
                    // This should never happen with the samples ZIP file.
                    throw new IllegalArgumentException("ZIP entry points outside target directory: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    if (!file.mkdirs())
                        throw new IOException("Unable to create directory: " + file.getAbsolutePath());
                } else {
                    copyData(zip, file);
                }
                zip.closeEntry();
                entry = zip.getNextEntry();
            }
            return true;
        } catch (IOException e) {
            Timber.e(e, "Error unzipping file");
            return false;
        }
    }

    private static void copyData(@NonNull ZipInputStream zip, @NonNull File destinationFile) throws IOException {
        byte[] buffer = new byte[16384];
        int readCount;
        try (OutputStream output = new BufferedOutputStream(new FileOutputStream(destinationFile))) {
            while ((readCount = zip.read(buffer, 0, buffer.length)) != -1) {
                output.write(buffer, 0, readCount);
            }
        }
    }
}
