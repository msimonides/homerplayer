package com.studio4plus.homerplayer.ui.classic;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.common.base.Preconditions;
import com.studio4plus.homerplayer.ApplicationComponent;
import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.HomerPlayerApplication;
import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.crashreporting.CrashReporting;
import com.studio4plus.homerplayer.ui.MultitapTouchListener;
import com.studio4plus.homerplayer.ui.UiControllerNoBooks;
import com.studio4plus.homerplayer.ui.NoBooksUi;
import com.studio4plus.homerplayer.ui.settings.OnFolderSelected;
import com.studio4plus.homerplayer.ui.settings.OpenDocumentTreeUtils;
import com.studio4plus.homerplayer.ui.settings.SettingsActivity;

import javax.inject.Inject;
import javax.inject.Named;

public class ClassicNoBooksUi extends Fragment implements NoBooksUi {

    private UiControllerNoBooks controller;
    private View view;
    private ProgressUi progressUi;

    public @Inject @Named("AUDIOBOOKS_DIRECTORY") String audioBooksDirectoryName;
    public @Inject GlobalSettings globalSettings;
    public @Inject OnFolderSelected onFolderSelected;

    private ActivityResultLauncher<Uri> openDocumentTreeContract;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ApplicationComponent component = HomerPlayerApplication.getComponent(requireContext());
        component.inject(this);
        openDocumentTreeContract = registerForActivityResult(
                new ActivityResultContracts.OpenDocumentTree(), onFolderSelected::onSelected);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_no_books, container, false);

        Button downloadSamplesButton = view.findViewById(R.id.downloadSamplesButton);
        downloadSamplesButton.setOnClickListener(v -> controller.startSamplesInstallation());

        Button selectFolderButton = view.findViewById(R.id.selectAudiobooksFolderButton);
        selectFolderButton.setOnClickListener(
                v -> OpenDocumentTreeUtils.launchWithErrorHandling(requireActivity(), openDocumentTreeContract));

        final Context context = view.getContext();
        view.setOnTouchListener(new MultitapTouchListener(
                context, () -> startActivity(new Intent(context, SettingsActivity.class))));

        return view;
    }

    @Override
    public void initWithController(@NonNull UiControllerNoBooks controller) {
        this.controller = controller;
    }

    @Override
    public void onResume() {
        super.onResume();
        CrashReporting.log("UI: ClassicNoBooks fragment resumed");
    }

    @Override
    public void shutdown() {
        if (progressUi != null) {
            progressUi.shutdown();
            progressUi = null;
        }
    }

    private void onInstallError() {
        Preconditions.checkNotNull(progressUi);
        progressUi.shutdown();
        new AlertDialog.Builder(view.getContext())
                .setTitle(R.string.samplesDownloadErrorTitle)
                .setMessage(R.string.samplesDownloadErrorMessage)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    @Override @NonNull
    public InstallProgressObserver showInstallProgress(boolean isAlreadyInstalling) {
        progressUi = new ProgressUi(view.getContext(), controller, isAlreadyInstalling);
        return progressUi;
    }

    private class ProgressUi implements InstallProgressObserver {
        private final @NonNull Context context;
        private final @NonNull
        UiControllerNoBooks controller;
        private final @NonNull ProgressDialog progressDialog;

        ProgressUi(@NonNull Context context,
                   @NonNull UiControllerNoBooks controller,
                   boolean isAlreadyInstalling) {
            this.context = context;
            this.controller = controller;
            this.progressDialog = createProgressDialog();
            progressDialog.show();
            progressDialog.setIndeterminate(true);
            if (isAlreadyInstalling)
                onInstallStarted();
        }

        @Override
        public void onDownloadProgress(int transferredBytes, int totalBytes) {
            if (totalBytes > -1) {
                int totalKBytes = totalBytes / 1024;
                if (progressDialog.isIndeterminate() || progressDialog.getMax() != totalKBytes) {
                    progressDialog.setIndeterminate(false);
                    progressDialog.setMax(totalKBytes);
                }
                progressDialog.setProgress(transferredBytes / 1024);
            }
        }

        @Override
        public void onInstallStarted() {
            progressDialog.setIndeterminate(true);
            progressDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setVisibility(View.INVISIBLE);
        }

        @Override
        public void onFailure() {
            ClassicNoBooksUi.this.onInstallError();
        }

        void shutdown() {
            progressDialog.dismiss();
        }

        private ProgressDialog createProgressDialog() {
            final ProgressDialog progressDialog = new ProgressDialog(context);

            int maxKB = 0;
            int progressKB = 0;

            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setTitle(R.string.samplesDownloadProgressTitle);
            progressDialog.setProgress(progressKB);
            progressDialog.setMax(maxKB);
            progressDialog.setProgressNumberFormat("%1d/%2d KB");
            progressDialog.setCancelable(false);
            progressDialog.setButton(
                    DialogInterface.BUTTON_NEGATIVE,
                    context.getString(android.R.string.cancel),
                    (dialog, which) -> {
                        // Note: the dialog will dismiss itself even if the controller doesn't
                        // abort the installation.
                        controller.abortSamplesInstallation();
                    });
            return progressDialog;
        }
    }

}
