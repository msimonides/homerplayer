package com.studio4plus.homerplayer.util;

import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewParent;

public class ViewUtils {

    public static Rect getRelativeRect(@NonNull View ancestor, @NonNull View view) {
        Rect rect = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
        ViewParent parent = view.getParent();
        while (parent instanceof View && parent != ancestor) {
            view = (View) parent;
            rect.left += view.getLeft();
            rect.right += view.getLeft();
            rect.top += view.getTop();
            rect.bottom += view.getTop();
            parent = view.getParent();
        }
        return rect;
    }
}
