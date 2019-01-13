package com.studio4plus.homerplayer.ui;

import androidx.appcompat.app.AppCompatActivity;

public interface MainUiComponent {
    AppCompatActivity activity();
    void inject(MainActivity mainActivity);
}
