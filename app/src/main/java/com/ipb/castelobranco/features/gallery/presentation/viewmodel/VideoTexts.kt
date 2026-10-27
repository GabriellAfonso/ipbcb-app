package com.ipb.castelobranco.features.gallery.presentation.viewmodel

import com.ipb.castelobranco.core.presentation.components.formatDiskUsage

/** The gallery's texts about videos. */
internal object VideoTexts {
    const val CARD_DISMISSED = "Você pode baixar os vídeos depois em Configurações."
    const val WAITING_WIFI = "Os vídeos vão baixar quando você estiver no WiFi"
    const val LOW_SPACE = "Pouco espaço no aparelho. Os vídeos pararam de baixar."
    const val PLAYBACK_FAILED = "Não foi possível reproduzir este vídeo neste aparelho"
    const val WAIT_WIFI_LABEL = "Esperar WiFi"
    const val SEND_LABEL = "Enviar"
    const val NOT_ON_DEVICE = "Não está no aparelho"

    fun mobileDataQuestion(count: Int, bytes: Long): String {
        val videos = if (count == 1) "1 vídeo" else "$count vídeos"
        val size = if (bytes > 0) " (${formatDiskUsage(bytes)})" else ""
        return "Você está nos dados móveis. Enviar $videos$size agora?"
    }
}
