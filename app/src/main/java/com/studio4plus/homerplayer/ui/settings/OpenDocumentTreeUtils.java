package com.studio4plus.homerplayer.ui.settings;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.storage.StorageManager;
import android.provider.DocumentsContract;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;

import com.studio4plus.homerplayer.R;

import org.jetbrains.annotations.Nullable;

import timber.log.Timber;

public class OpenDocumentTreeUtils {

    private final static String EXTRA_SHOW_ADVANCED_1 = "android.provider.extra.SHOW_ADVANCED";
    private final static String EXTRA_SHOW_ADVANCED_2 = "android.content.extra.SHOW_ADVANCED";

    public static class Contract extends ActivityResultContracts.OpenDocumentTree {
        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, @Nullable Uri input) {
            Intent intent = super.createIntent(context, input);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            intent.putExtra(EXTRA_SHOW_ADVANCED_1, true);
            intent.putExtra(EXTRA_SHOW_ADVANCED_2, true);
            return intent;
        }
    }

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
