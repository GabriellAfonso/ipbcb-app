package com.ipb.castelobranco.core.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Pull to refresh over a scrolling [content]. [enabled] `false` ignores the pull and keeps the content where it is
 * (e.g. while the list is being reordered by drag).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElasticPullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberPullToRefreshState()
    Box(modifier.pullToRefresh(isRefreshing = isRefreshing, state = state, enabled = enabled, onRefresh = onRefresh)) {
        content()
        Indicator(modifier = Modifier.align(Alignment.TopCenter), isRefreshing = isRefreshing, state = state)
    }
}
