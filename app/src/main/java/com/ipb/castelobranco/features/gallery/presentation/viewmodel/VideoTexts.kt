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
    const val TRIM_TITLE = "Cortar vídeo"
    const val USE_WHOLE = "Usar inteiro"
    const val CUT_AND_CONTINUE = "Cortar e continuar"
    const val SEND_ALL_WHOLE = "Enviar todos sem cortar"
    const val CANCEL = "Cancelar"
    const val TOO_BIG_TO_SEND = "Vídeo muito grande (máx. 100 MB). Selecione um trecho menor."
    const val CUT_FAILED = "Não foi possível cortar este vídeo."
    const val DISCARD_QUESTION = "Descartar a seleção?"
    const val DISCARD_DETAIL = "Nenhum item desta seleção será enviado."
    const val DISCARD = "Descartar"
    const val KEEP_EDITING = "Continuar"
    const val PLAY = "Reproduzir"
    const val PAUSE = "Pausar"
    const val START_TIME_TITLE = "Início do corte"
    const val END_TIME_TITLE = "Fim do corte"
    const val TIME_HINT = "Ex.: 1:23 ou 83,5"
    const val APPLY = "Aplicar"
    const val INVALID_TIME = "Digite um tempo como 1:23 ou 83,5."
    const val EDIT_TIME = "Digitar o tempo"

    fun timeOutsideVideo(duration: String) = "O vídeo tem $duration."

    fun startTooLate(end: String) = "O início precisa ficar pelo menos 1 segundo antes do fim ($end)."

    fun endTooEarly(start: String) = "O fim precisa ficar pelo menos 1 segundo depois do início ($start)."

    fun position(position: Int, total: Int) = "Vídeo $position de $total"

    fun cutting(progress: Int) = "Cortando… $progress%"

    fun cutTooLarge(bytes: Long) = "O trecho ficou com ${formatDiskUsage(bytes)}. Selecione um trecho menor."

    fun mobileDataQuestion(count: Int, bytes: Long): String {
        val videos = if (count == 1) "1 vídeo" else "$count vídeos"
        val size = if (bytes > 0) " (${formatDiskUsage(bytes)})" else ""
        return "Você está nos dados móveis. Enviar $videos$size agora?"
    }
}
