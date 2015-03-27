package com.studio4plus.homerplayer.model;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class Storage implements AudioBook.PositionObserver, AudioBookManager.Listener {

    private static final String PREFERENCES_NAME = Storage.class.getSimpleName();
    private static final String AUDIOBOOK_KEY_PREFIX = "audiobook_";
    private static final String LAST_AUDIOBOOK_KEY = "lastPlayedId";

    private static final String FIELD_POSITION = "position";
    private static final String FIELD_COLOUR_SCHEME = "colourScheme";
    private static final String FIELD_POSITION_FILEPATH = "filePath";
    private static final String FIELD_POSITION_SEEK = "seek";


    private final SharedPreferences preferences;

    public Storage(Context context) {
        this.preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public void readAudioBookState(AudioBook audioBook) {
        String bookData = preferences.getString(getAudioBookPreferenceKey(audioBook.getId()), null);
        if (bookData != null) {
            try {
                JSONObject jsonObject = (JSONObject) new JSONTokener(bookData).nextValue();
                JSONObject jsonPosition = jsonObject.getJSONObject(FIELD_POSITION);
                String fileName = jsonPosition.getString(FIELD_POSITION_FILEPATH);
                int seek = jsonPosition.getInt(FIELD_POSITION_SEEK);
                audioBook.setLastPosition(new Position(fileName, seek));

                String colourSchemeName = jsonObject.optString(FIELD_COLOUR_SCHEME, null);
                if (colourSchemeName != null) {
                    audioBook.setColourScheme(ColourScheme.valueOf(colourSchemeName));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void writeAudioBookState(AudioBook audioBook) {
        JSONObject jsonAudioBook = new JSONObject();
        JSONObject jsonPosition = new JSONObject();
        Position position = audioBook.getLastPosition();
        try {
            jsonPosition.put(FIELD_POSITION_FILEPATH, position.filePath);
            jsonPosition.put(FIELD_POSITION_SEEK, position.seekPosition);
            jsonAudioBook.put(FIELD_POSITION, jsonPosition);
            jsonAudioBook.putOpt(FIELD_COLOUR_SCHEME, audioBook.getColourScheme());

            SharedPreferences.Editor editor = preferences.edit();
            String key = getAudioBookPreferenceKey(audioBook.getId());
            editor.putString(key, jsonAudioBook.toString());
            editor.apply();
        } catch (JSONException e) {
            // Should never happen, none of the values is null, NaN nor Infinity.
            e.printStackTrace();
        }
    }

    public String getCurrentAudioBook() {
        return preferences.getString(LAST_AUDIOBOOK_KEY, null);
    }

    public void writeCurrentAudioBook(String id) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(LAST_AUDIOBOOK_KEY, id);
        editor.apply();
    }

    @Override
    public void onAudioBookPositionChanged(AudioBook audioBook) {
        writeAudioBookState(audioBook);
    }

    private String getAudioBookPreferenceKey(String id) {
        return AUDIOBOOK_KEY_PREFIX + id;
    }

    @Override
    public void onCurrentBookChanged(AudioBook book) {
        writeCurrentAudioBook(book.getId());
    }
}
