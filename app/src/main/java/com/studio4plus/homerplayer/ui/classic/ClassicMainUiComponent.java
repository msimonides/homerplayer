package com.studio4plus.homerplayer.ui.classic;

import com.studio4plus.homerplayer.ApplicationComponent;
import com.studio4plus.homerplayer.ui.MainUiComponent;
import com.studio4plus.homerplayer.ui.ActivityScope;

import dagger.Component;

@ActivityScope
@Component(dependencies = ApplicationComponent.class, modules = ClassicMainUiModule.class)
interface ClassicMainUiComponent extends MainUiComponent {
}
