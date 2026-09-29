package com.ipb.castelobranco.core.presentation.error

import com.ipb.castelobranco.core.domain.error.AppError

private const val HTTP_FORBIDDEN = 403

/**
 * Displayable text for an [AppError]. Uses the app-authored text when there is one, otherwise falls
 * back to a generic message per category. Never exposes [AppError.message], which may carry the
 * server's response body.
 *
 * A 403 is a permission refusal, not a session problem: it never tells the user to log in.
 */
fun AppError.toUserMessage(): String = userMessage ?: when (this) {
    is AppError.Network -> "Sem conexão com a internet. Verifique sua rede e tente novamente."
    is AppError.Auth    -> if (code == HTTP_FORBIDDEN) {
        "Você não tem permissão para esta ação."
    } else {
        "Faça login para continuar."
    }
    is AppError.Server  -> "Não foi possível completar a operação. Tente novamente mais tarde."
    is AppError.Unknown -> "Algo deu errado. Tente novamente."
}
