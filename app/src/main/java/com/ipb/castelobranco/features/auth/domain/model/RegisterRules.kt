package com.ipb.castelobranco.features.auth.domain.model

/**
 * The register rules the backend enforces (`backend/server/features/accounts/validators.py` and
 * `AUTH_PASSWORD_VALIDATORS`), repeated so the screen can point at a problem before sending. The backend still decides.
 */
object RegisterRules {
    const val PASSWORD_MIN_LENGTH = 6

    // Starting and ending alphanumeric is what rejects "..", ".", ".hidden" and "trailing.".
    private val USERNAME_PATTERN = Regex("^[a-z0-9]([a-z0-9._-]*[a-z0-9])?$")

    /** What the username field keeps of what was typed: lowercase, no whitespace. */
    fun normalizeUsername(input: String): String = input.lowercase().filterNot(Char::isWhitespace)

    fun isValidUsername(username: String): Boolean = USERNAME_PATTERN.matches(username)
}
