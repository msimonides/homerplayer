package com.studio4plus.homerplayer.ui;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;

import com.studio4plus.homerplayer.R;

public class PermissionUtils {

    public static boolean checkAndRequestPermissionForAudiobooksScan(
            Activity activity, String permission, int requestCode) {
        int permissionResult = ContextCompat.checkSelfPermission(activity, permission);
        if (permissionResult != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity, new String[]{ permission }, requestCode);
            return false;
        }
        return true;
    }

    public static AlertDialog.Builder permissionRationaleDialogBuilder(
            Activity activity, @StringRes int rationaleMessage) {
        return new AlertDialog.Builder(activity)
                .setMessage(rationaleMessage)
                .setTitle(R.string.permission_rationale_title)
                .setIcon(R.mipmap.ic_launcher);
    }
}
