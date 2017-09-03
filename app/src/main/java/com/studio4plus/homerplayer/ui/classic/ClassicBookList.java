package com.studio4plus.homerplayer.ui.classic;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;

import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.HomerPlayerApplication;
import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.analytics.AnalyticsTracker;
import com.studio4plus.homerplayer.model.AudioBook;
import com.studio4plus.homerplayer.ui.UiControllerBookList;
import com.studio4plus.homerplayer.ui.BookListUi;
import com.studio4plus.homerplayer.ui.HintOverlay;
import com.studio4plus.homerplayer.ui.MultitapTouchListener;
import com.studio4plus.homerplayer.ui.SettingsActivity;

import java.util.List;

import javax.inject.Inject;

public class ClassicBookList extends Fragment implements BookListUi {

    private View view;
    private ViewPager bookPager;
    private BookListPagerAdapter bookAdapter;
    private UiControllerBookList uiControllerBookList;

    @Inject public AnalyticsTracker analyticsTracker;
    @Inject public GlobalSettings globalSettings;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_book_list, container, false);
        HomerPlayerApplication.getComponent(view.getContext()).inject(this);

        bookPager = (ViewPager) view.findViewById(R.id.bookListPager);
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
                context, new MultitapTouchListener.Listener() {
                    @Override
                    public void onMultiTap() {
                        startActivity(new Intent(context, SettingsActivity.class));
                    }
                }));

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
        ViewStub browseHintStub = (ViewStub)view.findViewById(R.id.browseHintOverlayStub);
        ViewStub settingsHintStub = (ViewStub) view.findViewById(R.id.settingsHintOverlayStub);
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
