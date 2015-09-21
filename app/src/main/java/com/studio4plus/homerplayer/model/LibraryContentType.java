package com.studio4plus.homerplayer.model;

public enum LibraryContentType {
    EMPTY(0),
    SAMPLES_ONLY(1),
    USER_CONTENT(2);

    private final int priority;

    LibraryContentType(int priority) {
        this.priority = priority;
    }

    public boolean supersedes(LibraryContentType other) {
        return priority > other.priority;
    }
}
