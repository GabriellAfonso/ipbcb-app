package com.ipb.castelobranco.core.presentation.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/** The maps app, searching for [place]; nothing happens without one. */
fun Context.openMap(place: String) = openUri("geo:0,0?q=" + Uri.encode(place))

/** The app that handles [url], or the browser; nothing happens without one. */
fun Context.openLink(url: String) = openUri(url)

private fun Context.openUri(uri: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
    } catch (_: ActivityNotFoundException) {
        // No app on the phone can open it.
    }
}
