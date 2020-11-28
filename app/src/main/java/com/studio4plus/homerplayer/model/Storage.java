package com.studio4plus.homerplayer.model;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.studio4plus.homerplayer.events.CurrentBookChangedEvent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

public class Storage implements AudioBook.UpdateObserver {

    private static final String PREFERENCES_NAME = Storage.class.getSimpleName();
    private static final String AUDIOBOOK_KEY_PREFIX = "audiobook_";
    private static final String LAST_AUDIOBOOK_KEY = "lastPlayedId";

    private static final String FIELD_POSITION = "position";
    private static final String FIELD_COLOUR_SCHEME = "colourScheme";
    private static final String FIELD_POSITION_FILEPATH_DEPRECATED = "filePath";
    private static final String FIELD_POSITION_FILE_INDEX = "fileIndex";
    private static final String FIELD_POSITION_SEEK = "seek";
    private static final String FIELD_FILE_DURATIONS = "fileDurations";

    @NonNull
    private final SharedPreferences preferences;

    public Storage(@NonNull Context context, @NonNull EventBus eventBus) {
        this.preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        eventBus.register(this);
    }

    public void readAudioBookState(@NonNull AudioBook audioBook) {
        String bookData = preferences.getString(getAudioBookPreferenceKey(audioBook.getId()), null);
        if (bookData != null) {
            try {
                ColourScheme colourScheme = null;
                List<Long> durations = null;

                JSONObject jsonObject = (JSONObject) new JSONTokener(bookData).nextValue();
                JSONObject jsonPosition = jsonObject.getJSONObject(FIELD_POSITION);
                String fileName = jsonPosition.optString(FIELD_POSITION_FILEPATH_DEPRECATED, null);
                int fileIndex = jsonPosition.optInt(FIELD_POSITION_FILE_INDEX, -1);
                long seek = jsonPosition.getLong(FIELD_POSITION_SEEK);

                String colourSchemeName = jsonObject.optString(FIELD_COLOUR_SCHEME, null);
                if (colourSchemeName != null) {
                    colourScheme = ColourScheme.valueOf(colourSchemeName);
                }

                JSONArray jsonDurations = jsonObject.optJSONArray(FIELD_FILE_DURATIONS);
                if (jsonDurations != null) {
                    final int count = jsonDurations.length();
                    durations = new ArrayList<>(count);
                    for (int i = 0; i < count; ++i)
                        durations.add(jsonDurations.getLong(i));
                }

                if (fileIndex >= 0)
                    audioBook.restore(colourScheme, fileIndex, seek, durations);
                else
                    audioBook.restoreOldFormat(colourScheme, fileName, seek, durations);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void writeAudioBookState(AudioBook audioBook) {
        JSONObject jsonAudioBook = new JSONObject();
        JSONObject jsonPosition = new JSONObject();
        AudioBook.Position position = audioBook.getLastPosition();
        try {
            jsonPosition.put(FIELD_POSITION_FILE_INDEX, position.fileIndex);
            jsonPosition.put(FIELD_POSITION_SEEK, position.seekPosition);
            JSONArray jsonDurations = new JSONArray(audioBook.getFileDurations());
            jsonAudioBook.put(FIELD_POSITION, jsonPosition);
            jsonAudioBook.putOpt(FIELD_COLOUR_SCHEME, audioBook.getColourScheme());
            jsonAudioBook.put(FIELD_FILE_DURATIONS, jsonDurations);

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
    public void onAudioBookStateUpdated(AudioBook audioBook) {
        writeAudioBookState(audioBook);
    }

    private String getAudioBookPreferenceKey(String id) {
        return AUDIOBOOK_KEY_PREFIX + id;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEvent(CurrentBookChangedEvent event) {
        writeCurrentAudioBook(event.audioBook.getId());
    }
}
