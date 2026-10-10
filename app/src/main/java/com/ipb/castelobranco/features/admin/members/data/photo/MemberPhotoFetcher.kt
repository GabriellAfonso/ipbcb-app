package com.ipb.castelobranco.features.admin.members.data.photo

import android.net.Uri
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import okio.Buffer
import java.io.IOException

/**
 * Hands member photos to Coil from [MemberPhotoSource] instead of Coil's own HTTP fetcher, so they
 * come from the encrypted cache when there is a copy. No photo → an error, and the avatar shows
 * the initials.
 */
class MemberPhotoFetcher(
    private val url: String,
    private val options: Options,
    private val source: MemberPhotoSource,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val photo = source.load(url) ?: throw IOException(NO_PHOTO)
        return SourceResult(
            source = ImageSource(Buffer().write(photo.bytes), options.context),
            mimeType = photo.mimeType,
            dataSource = DataSource.DISK,
        )
    }

    class Factory(private val source: MemberPhotoSource) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.scheme != SCHEME_HTTP && data.scheme != SCHEME_HTTPS) return null
            return MemberPhotoFetcher(data.toString(), options, source)
        }
    }

    private companion object {
        const val NO_PHOTO = "Member photo unavailable"
        const val SCHEME_HTTP = "http"
        const val SCHEME_HTTPS = "https"
    }
}
