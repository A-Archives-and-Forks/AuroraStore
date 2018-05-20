package com.dragons.aurora.task.playstore;

import android.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.animation.AnimationUtils;

import com.dragons.aurora.R;
import com.dragons.aurora.adapters.RecyclerAppsAdapter;
import com.dragons.aurora.model.App;
import com.dragons.aurora.model.AppBuilder;
import com.dragons.aurora.playstoreapiv2.DetailsResponse;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class SearchHistoryTask extends ExceptionTask {

    public Set<String> readFromPref(String Key) {
        Set<String> set = PreferenceManager
                .getDefaultSharedPreferences(getContext())
                .getStringSet(Key, null);
        if (set != null)
            return set;
        else
            return new HashSet<>();
    }

    public void writeToPref(String Key, Set<String> newAppSet) {
        PreferenceManager
                .getDefaultSharedPreferences(getContext())
                .edit()
                .putStringSet(Key, newAppSet)
                .apply();
    }

    public void addHistory(String query) {
        String date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        String datedQuery = query + ":" + date;
        ArrayList<String> oldList = getHistoryList();
        oldList.add(datedQuery);
        Set<String> newSet = new HashSet<>();
        newSet.addAll(oldList);
        writeToPref("SEARCH_HISTORY", newSet);
    }

    public void addRecentApps(String packageName) {
        Set<String> set = readFromPref("APP_HISTORY");

        ArrayList<String> currList = new ArrayList<>();
        currList.addAll(set);
        currList.add(packageName);

        set.clear();
        set.addAll(currList);
        writeToPref("APP_HISTORY", set);
    }

    public ArrayList<String> getHistoryList() {
        Set<String> oldSet = readFromPref("SEARCH_HISTORY");
        ArrayList<String> oldList = new ArrayList<>();
        oldList.addAll(oldSet);
        return oldList;
    }

    public ArrayList<String> getRecentAppsList() {
        ArrayList<String> currList = new ArrayList<>();
        Set<String> savedAppSet = readFromPref("APP_HISTORY");
        currList.clear();
        currList.addAll(savedAppSet);
        return currList;
    }

    public List<App> getHistoryApps(GooglePlayAPI api, ArrayList<String> currList) throws IOException {
        List<App> apps = new ArrayList<>();
        for (String packageName : currList) {
            DetailsResponse response = api.details(packageName);
            App app = AppBuilder.build(response);
            apps.add(app);
        }
        return apps;
    }

    public void setupRecyclerView(RecyclerView recyclerView, List<App> appsToAdd) {
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_anim));
        recyclerView.setAdapter(new RecyclerAppsAdapter(getContext(), appsToAdd));
    }

    public boolean looksLikeAPackageId(String query) {
        if (TextUtils.isEmpty(query)) {
            return false;
        }
        String pattern = "([\\p{L}_$][\\p{L}\\p{N}_$]*\\.)+[\\p{L}_$][\\p{L}\\p{N}_$]*";
        Pattern r = Pattern.compile(pattern);
        return r.matcher(query).matches();
    }

}
