package com.studio4plus.homerplayer.ui;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
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
import com.studio4plus.homerplayer.model.AudioBookManager;
import com.studio4plus.homerplayer.model.DemoSamplesInstaller;
import com.studio4plus.homerplayer.util.Downloader;

import java.net.URL;

import javax.inject.Inject;
import javax.inject.Named;

public class FragmentNoBooks extends Fragment {

    @Inject @Named("AUDIOBOOKS_DIRECTORY") public String audioBooksDirectoryName;
    @Inject @Named("SAMPLES_DOWNLOAD_URL") public URL samplesDownloadUrl;
    @Inject public AudioBookManager audioBookManager;

    private ApplicationComponent component;
    private Downloader downloader;
    private View view;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_no_books, container, false);
        component = HomerPlayerApplication.getComponent(view.getContext());
        component.inject(this);

        TextView noBooksPath = (TextView) view.findViewById(R.id.noBooksPath);
        String directoryMessage =
                getString(R.string.copyBooksInstructionMessage, audioBooksDirectoryName);
        noBooksPath.setText(Html.fromHtml(directoryMessage));

        Button downloadSamplesButton = (Button) view.findViewById(R.id.downloadSamplesButton);
        downloadSamplesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSamplesDownload();
            }
        });

        return view;
    }

    private void startSamplesDownload() {
        downloader = new Downloader(samplesDownloadUrl);
        DemoSamplesInstaller installer = component.createDemoSamplesInstaller();
        new DownloadAndInstallTask(installer).execute();
    }

    private static class DownloadProgress {
        public final int transferredBytes;
        public final int totalBytes;

        private DownloadProgress(int transferredBytes, int totalBytes) {
            this.transferredBytes = transferredBytes;
            this.totalBytes = totalBytes;
        }
    }

    private class DownloadAndInstallTask
            extends AsyncTask<Void, DownloadProgress, Boolean> implements Downloader.ProgressUpdater {

        private final DemoSamplesInstaller installer;
        private final ProgressDialog progressDialog;

        private DownloadAndInstallTask(DemoSamplesInstaller installer) {
            this.installer = installer;
            progressDialog = new ProgressDialog(view.getContext());

            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setTitle(R.string.samplesDownloadProgressTitle);
            progressDialog.setMax(0);
            progressDialog.setProgressNumberFormat("%1d/%2d KB");
            progressDialog.setCancelable(false);
            progressDialog.setButton(
                    DialogInterface.BUTTON_NEGATIVE,
                    getString(android.R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            cancel(false);
                            downloader.cancel();
                        }
                    });
        }

        @Override
        protected void onPreExecute() {
            progressDialog.show();
            progressDialog.setIndeterminate(true);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (result) {
                audioBookManager.scanFiles();
            } else {
                new AlertDialog.Builder(view.getContext())
                        .setTitle(R.string.samplesDownloadErrorTitle)
                        .setMessage(R.string.samplesDownloadErrorMessage)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
            progressDialog.dismiss();
        }

        @Override
        protected void onCancelled(Boolean aBoolean) {
            downloader = null;
        }

        @Override
        protected void onProgressUpdate(DownloadProgress... values) {
            super.onProgressUpdate(values);
            DownloadProgress progress = values[0];
            if (progress.transferredBytes == progress.totalBytes) {
                progressDialog.setIndeterminate(true);
            } else {
                if (progressDialog.isIndeterminate()) {
                    progressDialog.setIndeterminate(false);
                    progressDialog.setMax(progress.totalBytes / 1024);
                }
                progressDialog.setProgress(progress.transferredBytes / 1024);
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            Preconditions.checkNotNull(downloader);
            downloader.setProgressUpdater(this);
            return DemoSamplesInstall.downloadAndInstall(downloader, installer);
        }

        @Override
        public void onDownloadProgress(int transferredBytes, int totalBytes) {
            if (totalBytes > 0)
                publishProgress(new DownloadProgress(transferredBytes, totalBytes));
        }

    }
}
