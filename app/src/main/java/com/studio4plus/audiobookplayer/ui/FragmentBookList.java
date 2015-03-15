package com.studio4plus.audiobookplayer.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.studio4plus.audiobookplayer.R;
import com.studio4plus.audiobookplayer.model.AudioBook;
import com.studio4plus.audiobookplayer.model.AudioBookManager;

import java.util.List;

public class FragmentBookList extends Fragment {

    private static final String CLASS_ID = FragmentBookList.class.getSimpleName();
    private static final String STATE_KEY_ITEM_INDEX = CLASS_ID + "_item_index";

    private ViewPager bookPager;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_book_list, container, false);

        final BookListPagerAdapter adapter =
                new BookListPagerAdapter(getFragmentManager());
        bookPager = (ViewPager) view.findViewById(R.id.bookListPager);
        bookPager.setAdapter(adapter);
        bookPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int i) {
                FragmentBookItem itemFragment = (FragmentBookItem) adapter.getItem(i);
                AudioBookManager.getInstance().setCurrentBook(itemFragment.getAudioBook());
            }
        });

        if (savedInstanceState != null) {
            int itemIndex = savedInstanceState.getInt(STATE_KEY_ITEM_INDEX, 0);
            if (itemIndex < adapter.getCount())
                bookPager.setCurrentItem(itemIndex);
        }

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_KEY_ITEM_INDEX, bookPager.getCurrentItem());
    }

    private class BookListPagerAdapter extends FragmentPagerAdapter {

        private final List<AudioBook> audioBooks;

        public BookListPagerAdapter(FragmentManager fm) {
            super(fm);
            this.audioBooks = AudioBookManager.getInstance().getAudioBooks();
        }

        @Override
        public Fragment getItem(int i) {
            return FragmentBookItem.newInstance(audioBooks.get(i).getDirectoryName());
        }

        @Override
        public int getCount() {
            return audioBooks.size();
        }
    }
}
