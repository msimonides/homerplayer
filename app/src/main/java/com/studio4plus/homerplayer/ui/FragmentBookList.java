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

import de.greenrobot.event.EventBus;

public class FragmentBookList extends Fragment {

    private ViewPager bookPager;
    private BookListPagerAdapter bookAdapter;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_book_list, container, false);

        final AudioBookManager audioBookManager = HomerPlayerApplication.getAudioBookManager();
        bookAdapter = new BookListPagerAdapter(getFragmentManager(), audioBookManager);
        bookPager = (ViewPager) view.findViewById(R.id.bookListPager);
        bookPager.setAdapter(bookAdapter);
        bookPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int i) {
                FragmentBookItem itemFragment = (FragmentBookItem) bookAdapter.getItem(i);
                audioBookManager.setCurrentBook(itemFragment.getAudioBook());
            }
        });

        bookPager.setCurrentItem(
                HomerPlayerApplication.getAudioBookManager().getCurrentBookIndex());
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
        bookAdapter = new BookListPagerAdapter(
                getFragmentManager(), HomerPlayerApplication.getAudioBookManager());
        bookPager.setAdapter(bookAdapter);
        bookPager.setCurrentItem(
                HomerPlayerApplication.getAudioBookManager().getCurrentBookIndex());
    }

    @SuppressWarnings("UnusedDeclaration")
    public void OnEvent(CurrentBookChangedEvent event) {
        bookPager.setCurrentItem(
                HomerPlayerApplication.getAudioBookManager().getCurrentBookIndex());
    }

    private class BookListPagerAdapter extends FragmentStatePagerAdapter {

        private final List<AudioBook> audioBooks;

        public BookListPagerAdapter(FragmentManager fm, AudioBookManager audioBookManager) {
            super(fm);
            this.audioBooks = audioBookManager.getAudioBooks();
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
