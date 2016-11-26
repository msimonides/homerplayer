package com.studio4plus.homerplayer.ui;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
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
import com.studio4plus.homerplayer.downloads.DownloadService;
import com.studio4plus.homerplayer.downloads.SamplesDownloadController;
import com.studio4plus.homerplayer.events.DemoSamplesInstallationFinishedEvent;
import com.studio4plus.homerplayer.events.DemoSamplesInstallationStartedEvent;
import com.studio4plus.homerplayer.model.AudioBookManager;

import javax.inject.Inject;
import javax.inject.Named;

import de.greenrobot.event.EventBus;

public class FragmentNoBooks extends Fragment {

    @Inject @Named("AUDIOBOOKS_DIRECTORY") public String audioBooksDirectoryName;
    @Inject public AudioBookManager audioBookManager;
    @Inject public SamplesDownloadController samplesDownloadController;
    @Inject public EventBus eventBus;

    private ProgressDialog progressDialog;
    private BroadcastReceiver progressReceiver;
    private View view;

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
                eventBus.post(new DemoSamplesInstallationStartedEvent());
                samplesDownloadController.startSamplesDownload();
                showDownloadAndInstallationProgress();
            }
        });

        final Context context = view.getContext();
        view.setOnTouchListener(new MultitapTouchListener(
                context, new MultitapGestureDetectorListener.Listener() {
            @Override
            public void onMultiTap() {
                startActivity(new Intent(context, SettingsActivity.class));
            }
        }));

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
    public void onEvent(DemoSamplesInstallationFinishedEvent event) {
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
            progressDialog = createProgressDialog();
            progressDialog.show();

            Preconditions.checkState(progressReceiver == null);
            IntentFilter filter = new IntentFilter(DownloadService.BROADCAST_DOWNLOAD_PROGRESS_ACTION);
            progressReceiver = new DownloadProgressReceiver();
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                    progressReceiver, filter);
        }
    }

    private void dismissDialog() {
        if (progressDialog != null) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(progressReceiver);
            progressReceiver = null;
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    private void updateProgress(int transferredBytes, int totalBytes) {
        if (totalBytes == -1
                || transferredBytes == totalBytes) {
            progressDialog.setIndeterminate(true);
        } else {
            int totalKBytes = totalBytes / 1024;
            if (progressDialog.isIndeterminate() || progressDialog.getMax() != totalKBytes) {
                progressDialog.setIndeterminate(false);
                progressDialog.setMax(totalKBytes);
            }
        }
        progressDialog.setProgress(transferredBytes / 1024);
    }

    private ProgressDialog createProgressDialog() {
        final ProgressDialog progressDialog = new ProgressDialog(view.getContext());

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

    private class DownloadProgressReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Preconditions.checkArgument(
                    DownloadService.BROADCAST_DOWNLOAD_PROGRESS_ACTION.equals(intent.getAction()));
            int transferredBytes = intent.getIntExtra(DownloadService.PROGRESS_BYTES_EXTRA, 0);
            int totalBytes = intent.getIntExtra(DownloadService.TOTAL_BYTES_EXTRA, -1);
            updateProgress(transferredBytes, totalBytes);
        }
    }
}
