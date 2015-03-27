package com.studio4plus.homerplayer.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.studio4plus.homerplayer.HomerPlayerApplication;
import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.model.AudioBook;
import com.studio4plus.homerplayer.model.AudioBookManager;

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

        final AudioBookManager audioBookManager = HomerPlayerApplication.getAudioBookManager();
        final BookListPagerAdapter adapter =
                new BookListPagerAdapter(getFragmentManager());
        bookPager = (ViewPager) view.findViewById(R.id.bookListPager);
        bookPager.setAdapter(adapter);
        bookPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int i) {
                FragmentBookItem itemFragment = (FragmentBookItem) adapter.getItem(i);
                audioBookManager.setCurrentBook(itemFragment.getAudioBook());
            }
        });

        bookPager.setCurrentItem(audioBookManager.getCurrentBookIndex());

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
            this.audioBooks = HomerPlayerApplication.getAudioBookManager().getAudioBooks();
        }

        @Override
        public Fragment getItem(int i) {
            return FragmentBookItem.newInstance(audioBooks.get(i).getId());
        }

        @Override
        public int getCount() {
            return audioBooks.size();
        }
    }
}
