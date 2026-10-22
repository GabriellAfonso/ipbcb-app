package com.ipb.castelobranco.core.domain.snapshot

import com.ipb.castelobranco.core.domain.error.AppError

sealed class SnapshotState<out T> {

    data object Loading : SnapshotState<Nothing>()

    data class Data<T>(val value: T) : SnapshotState<T>()

    data class Error(
        val error: AppError
    ) : SnapshotState<Nothing>()
}
