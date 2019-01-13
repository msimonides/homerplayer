package com.studio4plus.homerplayer.ui.classic;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Toast;

import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.ui.BookListUi;
import com.studio4plus.homerplayer.ui.MainUi;
import com.studio4plus.homerplayer.ui.NoBooksUi;

import java.io.File;

import javax.inject.Inject;

// TODO: ideally this would be a View.
class ClassicMainUi implements MainUi {

    private final @NonNull AppCompatActivity activity;

    @Inject
    ClassicMainUi(@NonNull AppCompatActivity activity) {
        this.activity = activity;
    }

    @NonNull @Override
    public BookListUi switchToBookList(boolean animate) {
        ClassicBookList bookList = new ClassicBookList();
        showPage(bookList, animate);
        return bookList;
    }

    @NonNull @Override
    public NoBooksUi switchToNoBooks(boolean animate) {
        ClassicNoBooksUi noBooks = new ClassicNoBooksUi();
        showPage(noBooks, animate);
        return noBooks;
    }

    @NonNull @Override
    public ClassicPlaybackUi switchToPlayback(boolean animate) {
        return new ClassicPlaybackUi(activity, this, animate);
    }

    @Override
    public void onPlaybackError(File path) {
        String message = activity.getString(R.string.playbackErrorToast, path.toString());
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
    }

    void showPlayback(@NonNull FragmentPlayback playbackUi, boolean animate) {
        showPage(playbackUi, animate);
    }

    private void showPage(@NonNull Fragment pageFragment, boolean animate) {
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (animate)
            transaction.setCustomAnimations(R.animator.flip_right_in, R.animator.flip_right_out);
        transaction.replace(R.id.mainContainer, pageFragment);
        transaction.commitNow();
    }
}
