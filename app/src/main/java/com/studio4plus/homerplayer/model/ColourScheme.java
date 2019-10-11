package com.studio4plus.homerplayer.model;

import androidx.annotation.AttrRes;

import com.studio4plus.homerplayer.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Use the first batch from Kenneth Kelly's max contrast color palette.
// See https://eleanormaclure.files.wordpress.com/2011/03/colour-coding.pdf
// More documents on color summarised here: http://stackoverflow.com/a/4382138/3892517
public enum ColourScheme {
    VIVID_YELLOW(R.attr.bookVividYellowBackground, R.attr.bookVividYellowTextColor),
    STRONG_PURPLE(R.attr.bookStrongPurpleBackground, R.attr.bookStrongPurpleTextColor),
    VIVID_ORANGE(R.attr.bookVividOrangeBackground, R.attr.bookVividOrangeTextColor),
    VERY_LIGHT_BLUE(R.attr.bookVeryLightBlueBackground, R.attr.bookVeryLightBlueTextColor),
    VIVID_RED(R.attr.bookVividRedBackground, R.attr.bookVividRedTextColor),
    GREYISH_YELLOW(R.attr.bookGreyishYellowBackground, R.attr.bookGreyishYellowTextColor),
    MEDIUM_GREY(R.attr.bookMediumGreyBackground, R.attr.bookMediumGreyTextColor);

    @AttrRes
    public final int backgroundColorAttrId;
    @AttrRes
    public final int textColourAttrId;

    ColourScheme(@AttrRes int backgroundColourAttrId, @AttrRes int textColourAttrId) {
        this.backgroundColorAttrId = backgroundColourAttrId;
        this.textColourAttrId = textColourAttrId;
    }

    private static Random random;

    public static ColourScheme getRandom(List<ColourScheme> avoidColours) {
        int totalColours = ColourScheme.values().length;
        List<ColourScheme> availableColourSchemes = new ArrayList<>(totalColours);
        for (ColourScheme colour : ColourScheme.values()) {
            if (!avoidColours.contains(colour))
                availableColourSchemes.add(colour);
        }

        return availableColourSchemes.get(getRandom().nextInt(availableColourSchemes.size()));
    }

    private static Random getRandom() {
        if (random == null)
            random = new Random();
        return random;
    }
}