package com.studio4plus.homerplayer.ui.settings;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.studio4plus.homerplayer.R;

import timber.log.Timber;

public class OpenDocumentTreeUtils {

    public static void launchWithErrorHandling(@NonNull Activity activity, @NonNull ActivityResultLauncher<Uri> launcher) {
        try {
            launcher.launch(null);
        } catch (ActivityNotFoundException e) {
            Timber.e(e, "Error launching an activity for ACTION_OPEN_DOCUMENT_TREE");
            new AlertDialog.Builder(activity)
                    .setMessage(R.string.settings_folders_no_opendocumenttree_alert)
                    .show();
        }
    }

}
