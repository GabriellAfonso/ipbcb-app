package com.ipb.castelobranco.features.auth.domain.model

data class RegisterErrors(
    val username: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val password: String? = null,
    val passwordConfirm: String? = null,
    val general: String? = null
)
