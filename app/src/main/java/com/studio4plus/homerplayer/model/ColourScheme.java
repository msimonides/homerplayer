package com.studio4plus.homerplayer.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Use the first batch from Kenneth Kelly's max contrast color palette.
// See https://eleanormaclure.files.wordpress.com/2011/03/colour-coding.pdf
// More documents on color summarised here: http://stackoverflow.com/a/4382138/3892517
public enum ColourScheme {
    VIVID_YELLOW(0xFFFFB300, 0xFF000000),
    STRONG_PURPLE(0xFF803E75, 0xFFFFFFFF),
    VIVID_ORANGE(0xFFFF6800, 0xFF000000),
    VERY_LIGHT_BLUE(0xFFA6BDD7, 0xFF000000),
    VIVID_RED(0xFFC10020, 0xFFFFFFFF),
    GREYISH_YELLOW(0xFFCEA262, 0xFF000000),
    MEDIUM_GREY(0xFF817066, 0xFFFFFFFF);

    public final int backgroundColour;
    public final int textColour;

    ColourScheme(int backgroundColour, int textColour) {
        this.backgroundColour = backgroundColour;
        this.textColour = textColour;
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