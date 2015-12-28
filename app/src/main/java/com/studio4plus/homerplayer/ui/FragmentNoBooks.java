package com.studio4plus.homerplayer.ui;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
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
import com.studio4plus.homerplayer.downloads.DownloadStatus;
import com.studio4plus.homerplayer.downloads.SamplesDownloadController;
import com.studio4plus.homerplayer.events.DemoSamplesInstallationFinished;
import com.studio4plus.homerplayer.model.AudioBookManager;
import com.studio4plus.homerplayer.util.TaskRepeater;

import javax.inject.Inject;
import javax.inject.Named;

import de.greenrobot.event.EventBus;

public class FragmentNoBooks extends Fragment {

    @Inject @Named("AUDIOBOOKS_DIRECTORY") public String audioBooksDirectoryName;
    @Inject public AudioBookManager audioBookManager;
    @Inject public DownloadManager downloadManager;
    @Inject public SamplesDownloadController samplesDownloadController;
    @Inject public EventBus eventBus;

    private ProgressDialog progressDialog;
    private View view;
    private TaskRepeater progressUpdater;
    private Handler mainHandler;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_no_books, container, false);
        ApplicationComponent component = HomerPlayerApplication.getComponent(view.getContext());
        component.inject(this);

        mainHandler = new Handler(getActivity().getMainLooper());

        TextView noBooksPath = (TextView) view.findViewById(R.id.noBooksPath);
        String directoryMessage =
                getString(R.string.copyBooksInstructionMessage, audioBooksDirectoryName);
        noBooksPath.setText(Html.fromHtml(directoryMessage));

        Button downloadSamplesButton = (Button) view.findViewById(R.id.downloadSamplesButton);
        downloadSamplesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                samplesDownloadController.startSamplesDownload();
                showDownloadAndInstallationProgress();
            }
        });

        eventBus.register(this);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        eventBus.unregister(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (samplesDownloadController.isDownloading()) {
            showDownloadAndInstallationProgress();
        }
    }

    @Override
    public void onStop() {
        dismissDialog();
        super.onStop();
    }

    @SuppressWarnings("unused")
    public void onEvent(DemoSamplesInstallationFinished event) {
        if (!event.success) {
            new AlertDialog.Builder(view.getContext())
                    .setTitle(R.string.samplesDownloadErrorTitle)
                    .setMessage(R.string.samplesDownloadErrorMessage)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
        dismissDialog();
    }

    private void showDownloadAndInstallationProgress() {
        if (progressDialog == null) {
            DownloadStatus downloadProgress = samplesDownloadController.getDownloadProgress();
            Preconditions.checkNotNull(downloadProgress);
            progressDialog = createProgressDialog(downloadProgress);
            progressDialog.show();
        }

        progressUpdater = new TaskRepeater(new Runnable() {
            @Override
            public void run() {
                DownloadStatus downloadProgress = samplesDownloadController.getDownloadProgress();
                // The updater should be stopped before the download is removed.
                // Therefore downloadProgress should always be available.
                Preconditions.checkNotNull(downloadProgress);
                updateProgress(downloadProgress);
            }
        }, mainHandler, 500);
        progressUpdater.start();
    }

    private void dismissDialog() {
        if (progressDialog != null) {
            progressUpdater.stop();
            progressDialog.dismiss();
            progressUpdater = null;
            progressDialog = null;
        }
    }

    private void updateProgress(@NonNull DownloadStatus downloadStatus) {
        // TODO: if the download is paused, tell the user to enable data transfer.
        if (downloadStatus.totalBytes == -1
                || downloadStatus.transferredBytes == downloadStatus.totalBytes) {
            progressDialog.setIndeterminate(true);
        } else {
            if (progressDialog.isIndeterminate()) {
                progressDialog.setIndeterminate(false);
                progressDialog.setMax(downloadStatus.totalBytes / 1024);
            }
        }
        progressDialog.setProgress(downloadStatus.transferredBytes / 1024);
    }

    private ProgressDialog createProgressDialog(@NonNull DownloadStatus downloadStatus) {
        final ProgressDialog progressDialog = new ProgressDialog(view.getContext());

        int maxKB = 0;
        int progressKB = 0;
        if (downloadStatus.totalBytes != -1) {
            progressKB = downloadStatus.transferredBytes / 1024;
            maxKB = downloadStatus.totalBytes / 1024;
        }

        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setTitle(R.string.samplesDownloadProgressTitle);
        progressDialog.setProgress(progressKB);
        progressDialog.setMax(maxKB);
        progressDialog.setProgressNumberFormat("%1d/%2d KB");
        progressDialog.setCancelable(false);
        progressDialog.setButton(
                DialogInterface.BUTTON_NEGATIVE,
                getString(android.R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismissDialog();
                        samplesDownloadController.cancelDownload();
                    }
                });
        return progressDialog;
    }
}
