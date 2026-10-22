package com.ipb.castelobranco.core.presentation.modifier

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Navigates to the previous/next page when the user taps within [zoneWidthFraction]
 * of the left or right edge respectively.
 */
fun Modifier.tapToPaginate(
    pagerState: PagerState,
    scope: CoroutineScope,
    zoneWidthFraction: Float = 0.2f,
): Modifier = pointerInput(pagerState.pageCount) {
    detectTapGestures { offset ->
        val zone = size.width * zoneWidthFraction
        scope.launch {
            when {
                offset.x < zone && pagerState.currentPage > 0 ->
                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                offset.x > size.width - zone && pagerState.currentPage < pagerState.pageCount - 1 ->
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }
}
