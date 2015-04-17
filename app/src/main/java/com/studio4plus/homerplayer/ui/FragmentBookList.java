package com.studio4plus.homerplayer.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.studio4plus.homerplayer.HomerPlayerApplication;
import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.events.AudioBooksChangedEvent;
import com.studio4plus.homerplayer.events.CurrentBookChangedEvent;
import com.studio4plus.homerplayer.model.AudioBook;
import com.studio4plus.homerplayer.model.AudioBookManager;

import java.util.List;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class FragmentBookList extends Fragment {

    private ViewPager bookPager;
    private BookListPagerAdapter bookAdapter;

    @Inject public AudioBookManager audioBookManager;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_book_list, container, false);
        HomerPlayerApplication.getComponent(view.getContext()).inject(this);

        bookAdapter = new BookListPagerAdapter(getFragmentManager(), audioBookManager);
        bookPager = (ViewPager) view.findViewById(R.id.bookListPager);
        bookPager.setAdapter(bookAdapter);
        bookPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            int currentViewIndex;

            @Override
            public void onPageSelected(int i) {
                FragmentBookItem itemFragment = (FragmentBookItem) bookAdapter.getItem(i);
                audioBookManager.setCurrentBook(itemFragment.getAudioBookId());
                currentViewIndex = i;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager.SCROLL_STATE_IDLE) {
                    int adjustedIndex = bookAdapter.wrapViewIndex(currentViewIndex);
                    if (adjustedIndex != currentViewIndex)
                        bookPager.setCurrentItem(adjustedIndex, false);
                }
            }
        });

        updateViewPosition();
        EventBus.getDefault().register(this);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEvent(AudioBooksChangedEvent event) {
        bookAdapter = new BookListPagerAdapter(getFragmentManager(), audioBookManager);
        bookPager.setAdapter(bookAdapter);
        updateViewPosition();
    }

    @SuppressWarnings("UnusedDeclaration")
    public void OnEvent(CurrentBookChangedEvent event) {
        updateViewPosition();
    }

    private void updateViewPosition() {
        int currentBookIndex = audioBookManager.getCurrentBookIndex();
        bookPager.setCurrentItem(bookAdapter.bookIndexToViewIndex(currentBookIndex), false);
    }

    private class BookListPagerAdapter extends FragmentStatePagerAdapter {

        private static final int OFFSET = 1;

        private final List<AudioBook> audioBooks;

        public BookListPagerAdapter(FragmentManager fm, AudioBookManager audioBookManager) {
            super(fm);
            this.audioBooks = audioBookManager.getAudioBooks();
        }

        @Override
        public Fragment getItem(int viewIndex) {
            int bookIndex = viewIndex - OFFSET;
            if (bookIndex < 0)
                bookIndex = audioBooks.size() + bookIndex;
            else
                bookIndex = bookIndex % audioBooks.size();

            return FragmentBookItem.newInstance(audioBooks.get(bookIndex));
        }

        @Override
        public int getCount() {
            return audioBooks.size() > 0 ? audioBooks.size() + 2*OFFSET : 0;
        }

        public int bookIndexToViewIndex(int bookIndex) {
            return bookIndex + OFFSET;
        }

        public int wrapViewIndex(int viewIndex) {
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
