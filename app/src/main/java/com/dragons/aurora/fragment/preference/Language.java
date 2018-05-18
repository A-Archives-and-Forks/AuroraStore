package com.dragons.aurora.fragment.preference;

import android.preference.Preference;

import com.dragons.aurora.OnListPreferenceChangeListener;
import com.dragons.aurora.PlayStoreApiAuthenticator;
import com.dragons.aurora.R;
import com.dragons.aurora.Util;
import com.dragons.aurora.fragment.PreferenceFragment;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class Language extends List {

    public Language(PreferenceFragment activity) {
        super(activity);
    }

    @Override
    protected OnListPreferenceChangeListener getOnListPreferenceChangeListener() {
        OnListPreferenceChangeListener listener = new OnListPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean result = super.onPreferenceChange(preference, newValue);
                try {
                    new PlayStoreApiAuthenticator(activity.getActivity()).getApi().setLocale(new Locale((String) newValue));
                } catch (IOException e) {
                    // Should be impossible to get to preferences with incorrect credentials
                }
                return result;
            }
        };
        listener.setDefaultLabel(activity.getString(R.string.pref_requested_language_default));
        return listener;
    }

    @Override
    protected Map<String, String> getKeyValueMap() {
        Map<String, String> languages = new HashMap<>();
        for (Locale locale : Locale.getAvailableLocales()) {
            String displayName = locale.getDisplayName();
            displayName = displayName.substring(0, 1).toUpperCase(Locale.getDefault()) + displayName.substring(1);
            languages.put(locale.toString(), displayName);
        }
        languages = Util.sort(languages);
        Util.addToStart(
                (LinkedHashMap<String, String>) languages,
                "",
                activity.getString(R.string.pref_requested_language_default)
        );
        return languages;
    }
}
