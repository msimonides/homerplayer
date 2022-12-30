package com.studio4plus.homerplayer.deviceadmin;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

@RequiresApi(29)
public class GetProvisioningModeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        finishWithDeviceOwnerIntent();
    }

    private void finishWithDeviceOwnerIntent() {
        Intent intent = new Intent();
        intent.putExtra(
                android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_MODE,
                android.app.admin.DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE
        );
        intent.putExtra(
                android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_SKIP_EDUCATION_SCREENS,
                true
        );
        setResult(RESULT_OK, intent);
        finish();
    }
}
