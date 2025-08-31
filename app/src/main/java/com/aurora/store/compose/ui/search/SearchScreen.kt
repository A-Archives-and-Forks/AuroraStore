/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.search

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExpandedDockedSearchBar
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.LocalAsyncImagePreviewHandler
import com.aurora.gplayapi.SearchSuggestEntry
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composables.ErrorComposable
import com.aurora.store.compose.composables.ProgressComposable
import com.aurora.store.compose.composables.SearchSuggestionComposable
import com.aurora.store.compose.composables.app.AppListComposable
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.coilPreviewProvider
import com.aurora.store.compose.ui.details.AppDetailsScreen
import com.aurora.store.data.model.SearchFilter
import com.aurora.store.viewmodel.search.SearchViewModel
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun SearchScreen(onNavigateUp: () -> Unit, viewModel: SearchViewModel = hiltViewModel()) {
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val results = viewModel.apps.collectAsLazyPagingItems()

    ScreenContent(
        suggestions = suggestions,
        results = results,
        onNavigateUp = onNavigateUp,
        onSearch = { query -> viewModel.search(query) },
        onFetchSuggestions = { query -> viewModel.fetchSuggestions(query) },
        onFilter = { filter -> viewModel.filterResults(filter) },
        isAnonymous = viewModel.authProvider.isAnonymous
    )
}

@Composable
private fun ScreenContent(
    suggestions: List<SearchSuggestEntry> = emptyList(),
    results: LazyPagingItems<App> = flowOf(PagingData.empty<App>()).collectAsLazyPagingItems(),
    onNavigateUp: () -> Unit = {},
    onFetchSuggestions: (String) -> Unit = {},
    onSearch: (String) -> Unit = {},
    onFilter: (filter: SearchFilter) -> Unit = {},
    isAnonymous: Boolean = true,
) {
    val textFieldState = rememberTextFieldState()
    val searchBarState = rememberSearchBarState()
    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
    var isSearching by rememberSaveable { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<String>()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(key1 = focusRequester) {
        awaitFrame()
        focusRequester.requestFocus()
    }

    LaunchedEffect(key1 = textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .collectLatest { query -> onFetchSuggestions(query) }
    }

    fun showDetailPane(packageName: String) {
        coroutineScope.launch {
            scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail, packageName)
        }
    }

    fun onRequestSearch(query: String) {
        textFieldState.setTextAndPlaceCursorAtEnd(query.trim())
        coroutineScope.launch { searchBarState.animateToCollapsed() }
        onSearch(textFieldState.text.toString())
        isSearching = true
    }

    @Composable
    fun SearchBar() {
        val inputField = @Composable {
            SearchBarDefaults.InputField(
                modifier = Modifier.focusRequester(focusRequester),
                searchBarState = searchBarState,
                textFieldState = textFieldState,
                onSearch = { query -> onRequestSearch(query) },
                placeholder = { Text(text = stringResource(R.string.search_hint)) },
                leadingIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                trailingIcon = {
                    if (textFieldState.text.isNotBlank()) {
                        IconButton(
                            onClick = {
                                textFieldState.clearText()
                                focusRequester.requestFocus()
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_cancel),
                                contentDescription = stringResource(R.string.action_clear)
                            )
                        }
                    }
                }
            )
        }

        AppBarWithSearch(
            scrollBehavior = scrollBehavior,
            state = searchBarState,
            inputField = inputField
        )

        ExpandedDockedSearchBar(state = searchBarState, inputField = inputField) {
            suggestions.forEach { suggestion ->
                SearchSuggestionComposable(
                    searchSuggestEntry = suggestion,
                    onClick = { query -> onRequestSearch(query) },
                    onAction = { query -> textFieldState.setTextAndPlaceCursorAtEnd(query.trim()) }
                )
            }
        }
    }

    @Composable
    fun ListPane() {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = { SearchBar() }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(vertical = dimensionResource(R.dimen.padding_medium))
            ) {
                FilterHeader(
                    isEnabled = isSearching && results.loadState.refresh is LoadState.NotLoading,
                    isAnonymous = isAnonymous,
                    onFilter = onFilter
                )

                when (results.loadState.refresh) {
                    is LoadState.Loading -> ProgressComposable()

                    is LoadState.Error -> {
                        ErrorComposable(
                            modifier = Modifier.padding(paddingValues),
                            icon = painterResource(R.drawable.ic_disclaimer),
                            message = stringResource(R.string.error)
                        )
                    }

                    else -> {
                        if (isSearching && results.itemCount == 0) {
                            ErrorComposable(
                                modifier = Modifier.padding(paddingValues),
                                icon = painterResource(R.drawable.ic_disclaimer),
                                message = stringResource(R.string.no_apps_available)
                            )
                        } else {
                            LazyColumn {
                                items(
                                    count = results.itemCount,
                                    key = results.itemKey { it.id }
                                ) { index ->
                                    results[index]?.let { app ->
                                        AppListComposable(
                                            app = app,
                                            onClick = { showDetailPane(app.packageName) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun DetailPane() {
        with(scaffoldNavigator.currentDestination?.contentKey) {
            when {
                this != null -> {
                    AppDetailsScreen(
                        packageName = this,
                        onNavigateToAppDetails = { packageName -> showDetailPane(packageName) },
                        onNavigateUp = {
                            coroutineScope.launch { scaffoldNavigator.navigateBack() }
                        },
                        forceSinglePane = true
                    )
                }

                else -> {
                    if (isSearching && results.itemCount > 0) {
                        ErrorComposable(
                            icon = painterResource(R.drawable.ic_round_search),
                            message = stringResource(R.string.select_app_for_details)
                        )
                    }
                }
            }
        }
    }

    NavigableListDetailPaneScaffold(
        navigator = scaffoldNavigator,
        listPane = { AnimatedPane { ListPane() } },
        detailPane = { AnimatedPane { DetailPane() } }
    )
}

@Composable
private fun FilterHeader(
    isAnonymous: Boolean = true,
    isEnabled: Boolean = true,
    onFilter: (filter: SearchFilter) -> Unit
) {
    var activeFilter by rememberSaveable { mutableStateOf(SearchFilter()) }

    val filters = listOfNotNull(
        R.string.action_filter_rating,
        R.string.app_info_downloads,
        R.string.details_free,
        R.string.action_filter_no_ads,
        if (!isAnonymous) R.string.action_filter_no_gms else null
    )

    @Composable
    fun NonExpandableFilterChip(@StringRes filter: Int, isSelected: Boolean) {
        FilterChip(
            enabled = isEnabled,
            onClick = {
                activeFilter = when (filter) {
                    R.string.details_free -> activeFilter.copy(isFree = !activeFilter.isFree)
                    R.string.action_filter_no_ads -> activeFilter.copy(noAds = !activeFilter.noAds)
                    else -> activeFilter.copy(noGMS = !activeFilter.noGMS)
                }
                onFilter(activeFilter)
            },
            label = { Text(text = stringResource(filter)) },
            selected = isSelected,
            leadingIcon = {
                if (isSelected) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = stringResource(filter)
                    )
                }
            },
        )
    }

    @Composable
    fun ExpandableFilterChip(@StringRes filter: Int, isSelected: Boolean) {
        var isExpanded by rememberSaveable { mutableStateOf(false) }

        Box {
            FilterChip(
                enabled = isEnabled,
                onClick = { isExpanded = !isExpanded },
                label = { Text(text = stringResource(filter)) },
                selected = isSelected,
                leadingIcon = {
                    if (isSelected) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = stringResource(filter)
                        )
                    }
                },
                trailingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_drop_down),
                        contentDescription = stringResource(filter)
                    )
                },
            )

            DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
                val downloadLabels = stringArrayResource(R.array.filterDownloadsLabels)
                val downloadValues = stringArrayResource(R.array.filterDownloadsValues)
                val ratingLabels = stringArrayResource(R.array.filterRatingLabels)
                val ratingValues = stringArrayResource(R.array.filterRatingValues)

                val options = when (filter) {
                    R.string.action_filter_rating -> ratingLabels.zip(ratingValues).toMap()
                    R.string.app_info_downloads -> downloadLabels.zip(downloadValues).toMap()
                    else -> emptyMap()
                }

                options.forEach { (key, value) ->
                    DropdownMenuItem(
                        text = { Text(text = key) },
                        onClick = {
                            activeFilter = when (filter) {
                                R.string.action_filter_rating -> {
                                    activeFilter.copy(minRating = value.toFloat())
                                }

                                else -> activeFilter.copy(minInstalls = value.toLong())
                            }
                            onFilter(activeFilter)
                            isExpanded = false
                        }
                    )
                }
            }
        }
    }

    LazyRow(
        modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_medium)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_normal))
    ) {
        items(items = filters, key = { item -> item }) { filter ->
            val isSelected = when (filter) {
                R.string.action_filter_rating -> activeFilter.minRating > 0.0
                R.string.app_info_downloads -> activeFilter.minInstalls > 0
                R.string.details_free -> activeFilter.isFree
                R.string.action_filter_no_ads -> activeFilter.noAds
                R.string.action_filter_no_gms -> activeFilter.noGMS
                else -> false
            }

            when (filter) {
                R.string.details_free,
                R.string.action_filter_no_ads,
                R.string.action_filter_no_gms -> {
                    NonExpandableFilterChip(filter = filter, isSelected = isSelected)
                }

                R.string.action_filter_rating,
                R.string.app_info_downloads -> {
                    ExpandableFilterChip(filter = filter, isSelected = isSelected)
                }
            }
        }
    }
}

@PreviewScreenSizes
@Composable
private fun SearchScreenPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    val apps = List(10) { app.copy(id = Random.nextInt()) }
    val results = flowOf(PagingData.from(apps)).collectAsLazyPagingItems()

    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides coilPreviewProvider) {
        ScreenContent(results = results)
    }
}
