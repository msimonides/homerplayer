package com.studio4plus.homerplayer.ui.classic;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;

import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.HomerPlayerApplication;
import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.analytics.AnalyticsTracker;
import com.studio4plus.homerplayer.crashreporting.CrashReporting;
import com.studio4plus.homerplayer.model.AudioBook;
import com.studio4plus.homerplayer.ui.UiControllerBookList;
import com.studio4plus.homerplayer.ui.BookListUi;
import com.studio4plus.homerplayer.ui.HintOverlay;
import com.studio4plus.homerplayer.ui.MultitapTouchListener;
import com.studio4plus.homerplayer.ui.settings.SettingsActivity;

import java.util.List;

import javax.inject.Inject;

public class ClassicBookList extends Fragment implements BookListUi {

    private View view;
    private ViewPager bookPager;
    private BookListPagerAdapter bookAdapter;
    private UiControllerBookList uiControllerBookList;

    @Inject public AnalyticsTracker analyticsTracker;
    @Inject public GlobalSettings globalSettings;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_book_list, container, false);
        HomerPlayerApplication.getComponent(view.getContext()).inject(this);

        bookPager = view.findViewById(R.id.bookListPager);
        bookPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            int currentViewIndex;
            String currentBookId;

            @Override
            public void onPageSelected(int index) {
                FragmentBookItem itemFragment = (FragmentBookItem) bookAdapter.getItem(index);
                if (!itemFragment.getAudioBookId().equals(currentBookId)) {
                    currentBookId = itemFragment.getAudioBookId();
                    uiControllerBookList.changeBook(currentBookId);
                }
                currentViewIndex = index;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager.SCROLL_STATE_IDLE) {
                    int adjustedIndex = bookAdapter.wrapViewIndex(currentViewIndex);
                    if (adjustedIndex != currentViewIndex)
                        bookPager.setCurrentItem(adjustedIndex, false);
                    analyticsTracker.onBookSwiped();
                }
            }
        });

        final Context context = view.getContext();
        bookPager.setOnTouchListener(new MultitapTouchListener(
                context, () -> startActivity(new Intent(context, SettingsActivity.class))));

        return view;
    }

    @Override
    public void updateBookList(List<AudioBook> audioBooks, int currentBookIndex) {
        bookAdapter = new BookListPagerAdapter(getChildFragmentManager(), audioBooks);
        bookPager.setAdapter(bookAdapter);
        bookPager.setCurrentItem(
                bookAdapter.bookIndexToViewIndex(currentBookIndex), false);
    }

    @Override
    public void updateCurrentBook(int currentBookId) {

    }

    @Override
    public void initWithController(UiControllerBookList uiControllerBookList) {
        this.uiControllerBookList = uiControllerBookList;
    }

    @Override
    public void onResume() {
        super.onResume();
        CrashReporting.log("UI: ClassicBookList fragment resumed");
        showHintsIfNecessary();
    }

    private void showHintsIfNecessary() {
        if (isResumed() && isVisible() && !isAnyHintVisible()) {
            if (!globalSettings.browsingHintShown()) {
                HintOverlay overlay = new HintOverlay(
                        view, R.id.browseHintOverlayStub, R.string.hint_browsing, R.drawable.hint_horizontal_swipe);
                overlay.show();
                globalSettings.setBrowsingHintShown();
            } else if (!globalSettings.settingsHintShown()) {
                HintOverlay overlay = new HintOverlay(
                        view, R.id.settingsHintOverlayStub, R.string.hint_settings, R.drawable.hint_tap);
                overlay.show();
                globalSettings.setSettingsHintShown();
            }
        }
    }

    private boolean isAnyHintVisible() {
        ViewStub browseHintStub = view.findViewById(R.id.browseHintOverlayStub);
        ViewStub settingsHintStub = view.findViewById(R.id.settingsHintOverlayStub);
        return  browseHintStub == null || settingsHintStub == null;
    }

    private class BookListPagerAdapter extends FragmentStatePagerAdapter {

        private static final int OFFSET = 1;

        private final @NonNull List<AudioBook> audioBooks;

        BookListPagerAdapter(@NonNull FragmentManager fm, @NonNull List<AudioBook> audioBooks) {
            super(fm);
            this.audioBooks = audioBooks;
        }

        int getBookIndex(int viewIndex) {
            int bookIndex = viewIndex - OFFSET;
            if (bookIndex < 0)
                return audioBooks.size() + bookIndex;
            else
                return bookIndex % audioBooks.size();
        }

        @Override
        @NonNull
        public Fragment getItem(int viewIndex) {
            int bookIndex = getBookIndex(viewIndex);
            FragmentBookItem item = FragmentBookItem.newInstance(audioBooks.get(bookIndex).getId());
            item.setController(uiControllerBookList);
            return item;
        }

        @Override
        public int getCount() {
            return audioBooks.size() > 0 ? audioBooks.size() + 2*OFFSET : 0;
        }

        int bookIndexToViewIndex(int bookIndex) {
            return bookIndex + OFFSET;
        }

        int wrapViewIndex(int viewIndex) {
            if (viewIndex < OFFSET) {
                viewIndex += audioBooks.size();
            } else {
                final int audioBookCount = audioBooks.size();
                final int lastBookIndex = audioBookCount - 1;
                if (viewIndex - OFFSET > lastBookIndex)
                    viewIndex -= audioBookCount;
            }
            return viewIndex;
        }
    }
}
