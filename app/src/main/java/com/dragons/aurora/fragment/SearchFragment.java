package com.dragons.aurora.fragment;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dragons.aurora.HistoryItemTouchHelper;
import com.dragons.aurora.PlayStoreApiAuthenticator;
import com.dragons.aurora.R;
import com.dragons.aurora.activities.SearchActivity;
import com.dragons.aurora.adapters.SearchHistoryAdapter;
import com.dragons.aurora.task.playstore.SearchHistoryTask;
import com.dragons.aurora.view.ClusterAppsCard;
import com.github.florent37.shapeofview.shapes.RoundRectView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class SearchFragment extends SearchHistoryTask implements HistoryItemTouchHelper.RecyclerItemTouchHelperListener {
    @BindView(R.id.searchClusterApp)
    ClusterAppsCard clusterAppsCard;
    @BindView(R.id.m_apps_recycler)
    RecyclerView clusterRecycler;
    @BindView(R.id.searchHistory)
    RecyclerView recyclerView;
    @BindView(R.id.search_layout)
    CardView search_layout;
    @BindView(R.id.emptyView)
    TextView emptyView;
    @BindView(R.id.clearAll)
    TextView clearAll;

    private View view;
    private ArrayList<String> currList;
    private SearchHistoryAdapter searchHistoryAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (view != null) {
            if ((ViewGroup) view.getParent() != null)
                ((ViewGroup) view.getParent()).removeView(view);
            return view;
        }

        view = inflater.inflate(R.layout.fragment_search, container, false);
        SearchView searchToolbar = view.findViewById(R.id.search_apps);
        ButterKnife.bind(this, view);

        clearAll.setOnClickListener(v -> clearAll());
        search_layout.setOnClickListener(v -> {
            searchToolbar.setFocusable(true);
            searchToolbar.setIconified(false);
            searchToolbar.requestFocusFromTouch();
            searchToolbar.setQuery("", false);
        });

        addQueryTextListener(searchToolbar);
        setupSearchHistory();
        getHistoryApps(getRecentAppsList());
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSearchHistory();
    }

    protected void addQueryTextListener(SearchView searchView) {
        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        if (null != searchManager) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        }
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.setQuery("", false);
                searchView.setIconified(true);
                searchView.clearFocus();
                setQuery(query);
                return false;
            }
        });

        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                return true;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                Cursor cursor = searchView.getSuggestionsAdapter().getCursor();
                cursor.moveToPosition(position);
                String suggestion = cursor.getString(2);
                searchView.setQuery(suggestion, true);
                return false;
            }
        });
    }

    private void setQuery(String query) {
        if (looksLikeAPackageId(query))
            addRecentApps(query);
        else
            addHistory(query);

        Intent i = new Intent(getContext(), SearchActivity.class);
        i.setAction(Intent.ACTION_SEARCH);
        i.putExtra(SearchManager.QUERY, query);
        startActivity(i);
    }

    private void setupSearchHistory() {
        currList = getHistoryList();
        toggleEmptyRecycle(currList);
        searchHistoryAdapter = new SearchHistoryAdapter(currList, getContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setAdapter(searchHistoryAdapter);
        new ItemTouchHelper(
                new HistoryItemTouchHelper(0, ItemTouchHelper.LEFT, this))
                .attachToRecyclerView(recyclerView);
    }

    private void updateSearchHistory() {
        if (searchHistoryAdapter != null) {
            currList = getHistoryList();
            toggleEmptyRecycle(currList);
            searchHistoryAdapter.queryHistory = currList;
            searchHistoryAdapter.notifyDataSetChanged();
        }
    }

    private void updateHistoryPref() {
        Set<String> updatedSet = new HashSet<>();
        updatedSet.addAll(currList);
        writeToPref("SEARCH_HISTORY", updatedSet);
        if (recyclerView.getAdapter().getItemCount() == 0) {
            toggleEmptyRecycle(currList);
        }
    }

    private void clearAll() {
        if (searchHistoryAdapter != null) {
            currList = new ArrayList<>();
            searchHistoryAdapter.queryHistory = currList;
            searchHistoryAdapter.notifyDataSetChanged();
        }
        writeToPref("SEARCH_HISTORY", new HashSet<>());
        writeToPref("APP_HISTORY", new HashSet<>());
        clusterAppsCard.setVisibility(View.GONE);
        toggleEmptyRecycle(currList);
    }

    private void toggleEmptyRecycle(ArrayList<String> currList) {
        if (currList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    public void getHistoryApps(ArrayList<String> appList) {
        if (!appList.isEmpty()) {
            TextView clusterTitle = clusterAppsCard.findViewById(R.id.m_apps_title);
            clusterTitle.setText(R.string.action_search_history_apps);
            clusterTitle.setTextSize(18);
            Observable.fromCallable(() -> getHistoryApps(new PlayStoreApiAuthenticator(this.getActivity()).getApi(), appList))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((appToAdd) -> {
                        if (view != null) {
                            setupRecyclerView(clusterRecycler, appToAdd);
                        }
                    }, this::processException);
        } else clusterAppsCard.setVisibility(View.GONE);
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if (viewHolder instanceof SearchHistoryAdapter.ViewHolder) {
            searchHistoryAdapter.remove(position);
            currList = searchHistoryAdapter.queryHistory;
            updateHistoryPref();
        }
    }
}
