package com.studio4plus.homerplayer.ui.settings;

import static com.studio4plus.homerplayer.util.CollectionUtils.map;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.studio4plus.homerplayer.HomerPlayerApplication;
import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.databinding.FoldersActivityBinding;
import com.studio4plus.homerplayer.databinding.ItemAudiobooksFolderAddBinding;
import com.studio4plus.homerplayer.databinding.ItemAudiobooksFolderBinding;
import com.studio4plus.homerplayer.util.Callback;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

public class SettingsFoldersActivity extends AppCompatActivity {

    @Inject public AudiobooksFolderManager folderManager;
    @Inject public OnFolderSelected onFolderSelected;

    private ActivityResultLauncher<Uri> openDocumentTreeContract;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applySettingsTheme();
        FoldersActivityBinding views = FoldersActivityBinding.inflate(LayoutInflater.from(this));
        setContentView(views.getRoot());

        HomerPlayerApplication.getComponent(this).inject(this);
        openDocumentTreeContract = registerForActivityResult(
                new ActivityResultContracts.OpenDocumentTree(), onFolderSelected::onSelected);

        setSupportActionBar(views.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FoldersAdapter foldersAdapter =
                new FoldersAdapter(folderManager::removeAudiobooksFolder);
        views.listFolders.setAdapter(
                new ConcatAdapter(new AddAdapter(this::addFolder), foldersAdapter));
        views.listFolders.setLayoutManager(new LinearLayoutManager(this));

        folderManager.observeFolders().observe(this, folders -> {
            List<FolderEntry> folderNames =
                    map(folders, folder -> new FolderEntry(this, Objects.requireNonNull(folder.getUri())));
            Collections.sort(folderNames);
            foldersAdapter.submitList(folderNames);
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void applySettingsTheme() {
        // Taken from PreferenceFragmentCompat:
        final TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(R.attr.preferenceTheme, tv, true);
        int theme = tv.resourceId;
        if (theme == 0) {
            // Fallback to default theme.
            theme = R.style.PreferenceThemeOverlay;
        }
        getTheme().applyStyle(theme, false);
    }

    private void addFolder() {
        OpenDocumentTreeUtils.launchWithErrorHandling(this, openDocumentTreeContract);
    }

    private static class FolderEntry implements Comparable<FolderEntry> {

        @NonNull
        public final String name;
        @NonNull
        public final Uri uri;

        private FolderEntry(@NonNull Context context, @NonNull Uri uri) {
            this.uri = uri;
            DocumentFile documentFile =
                    Objects.requireNonNull(DocumentFile.fromTreeUri(context, uri));
            name = (documentFile.getName() != null)
                    ? documentFile.getName()
                    : documentFile.getUri().toString();
        }

        @Override
        public int compareTo(@NonNull FolderEntry folderEntry) {
            return name.compareTo(folderEntry.name);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FolderEntry that = (FolderEntry) o;
            return name.equals(that.name) && uri.equals(that.uri);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, uri);
        }
    }

    private static class FolderVH extends RecyclerView.ViewHolder {

        public final ItemAudiobooksFolderBinding views;

        public FolderVH(@NonNull ItemAudiobooksFolderBinding views) {
            super(views.getRoot());
            this.views = views;
        }
    }

    private static class AddFolderVH extends RecyclerView.ViewHolder {
        public AddFolderVH(@NonNull ItemAudiobooksFolderAddBinding views) {
            super(views.getRoot());
        }
    }

    private static class AddAdapter extends RecyclerView.Adapter<AddFolderVH> {

        private final Runnable onClick;

        private AddAdapter(Runnable onClick) {
            this.onClick = onClick;
        }

        @NonNull
        @Override
        public AddFolderVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new AddFolderVH(ItemAudiobooksFolderAddBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull AddFolderVH holder, int position) {
            holder.itemView.setOnClickListener(v -> onClick.run());
        }

        @Override
        public int getItemCount() {
            return 1;
        }
    }

    private static class FoldersAdapter extends ListAdapter<FolderEntry, FolderVH> {

        private final Callback<String> onDelete;

        public FoldersAdapter(@NonNull Callback<String> onDelete) {
            super(new Differ());
            this.onDelete = onDelete;
        }

        @NonNull
        @Override
        public FolderVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new FolderVH(ItemAudiobooksFolderBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull FolderVH holder, int position) {
            FolderEntry folder = getItem(position);
            holder.views.name.setText(folder.name);
            holder.views.buttonRemove.setOnClickListener(view -> onDelete.onFinished(folder.uri.toString()));
        }
    }

    private static class Differ extends DiffUtil.ItemCallback<FolderEntry> {
        @Override
        public boolean areItemsTheSame(@NonNull FolderEntry oldItem, @NonNull FolderEntry newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areContentsTheSame(@NonNull FolderEntry oldItem, @NonNull FolderEntry newItem) {
            return oldItem.uri.equals(newItem.uri);
        }
    }
}
