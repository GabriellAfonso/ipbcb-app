package com.ipb.castelobranco.core.domain.download

/**
 * Modelo compartilhado de progresso de download usado por features que
 * baixam dados em segundo plano (galeria, bíblia, etc).
 */
data class DownloadProgress(
    val downloaded: Int,
    val total: Int,
) {
    val percentage: Int
        get() = if (total == 0) 0 else (downloaded * 100) / total
}
