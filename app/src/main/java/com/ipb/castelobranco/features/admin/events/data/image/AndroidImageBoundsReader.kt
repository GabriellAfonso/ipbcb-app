package com.ipb.castelobranco.features.admin.events.data.image

import android.graphics.BitmapFactory
import com.ipb.castelobranco.features.admin.events.domain.repository.ImageBounds
import com.ipb.castelobranco.features.admin.events.domain.repository.ImageBoundsReader
import javax.inject.Inject

class AndroidImageBoundsReader @Inject constructor() : ImageBoundsReader {
    override fun read(bytes: ByteArray): ImageBounds? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        return if (options.outWidth > 0 && options.outHeight > 0) {
            ImageBounds(options.outWidth, options.outHeight)
        } else {
            null
        }
    }
}
