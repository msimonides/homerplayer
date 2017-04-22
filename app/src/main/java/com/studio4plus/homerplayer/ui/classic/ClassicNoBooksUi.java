package com.studio4plus.homerplayer.ui.classic;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.studio4plus.homerplayer.ApplicationComponent;
import com.studio4plus.homerplayer.HomerPlayerApplication;
import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.ui.MultitapTouchListener;
import com.studio4plus.homerplayer.ui.UiControllerNoBooks;
import com.studio4plus.homerplayer.ui.NoBooksUi;
import com.studio4plus.homerplayer.ui.SettingsActivity;

import javax.inject.Inject;
import javax.inject.Named;


public class ClassicNoBooksUi extends Fragment implements NoBooksUi {

    private UiControllerNoBooks controller;
    private View view;
    private ProgressUi progressUi;

    public @Inject @Named("AUDIOBOOKS_DIRECTORY") String audioBooksDirectoryName;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_no_books, container, false);
        ApplicationComponent component = HomerPlayerApplication.getComponent(view.getContext());
        component.inject(this);

        TextView noBooksPath = (TextView) view.findViewById(R.id.noBooksPath);
        String directoryMessage =
                getString(R.string.copyBooksInstructionMessage, audioBooksDirectoryName);
        noBooksPath.setText(Html.fromHtml(directoryMessage));

        Button downloadSamplesButton = (Button) view.findViewById(R.id.downloadSamplesButton);
        downloadSamplesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                controller.startSamplesInstallation();
            }
        });

        final Context context = view.getContext();
        view.setOnTouchListener(new MultitapTouchListener(
                context, new MultitapTouchListener.Listener() {
            @Override
            public void onMultiTap() {
                startActivity(new Intent(context, SettingsActivity.class));
            }
        }));

        return view;
    }

    @Override
    public void initWithController(@NonNull UiControllerNoBooks controller) {
        this.controller = controller;
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
    public InstallProgressObserver showInstallProgress() {
        progressUi = new ProgressUi(view.getContext(), controller);
        return progressUi;
    }

    private class ProgressUi implements InstallProgressObserver {
        private final @NonNull Context context;
        private final @NonNull
        UiControllerNoBooks controller;
        private final @NonNull ProgressDialog progressDialog;

        ProgressUi(@NonNull Context context, @NonNull UiControllerNoBooks controller) {
            this.context = context;
            this.controller = controller;
            this.progressDialog = createProgressDialog();
            progressDialog.show();
            progressDialog.setIndeterminate(true);
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
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            controller.abortSamplesInstallation();
                        }
                    });
            return progressDialog;
        }
    }

}
