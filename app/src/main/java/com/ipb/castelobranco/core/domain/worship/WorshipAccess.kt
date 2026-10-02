package com.ipb.castelobranco.core.domain.worship

/**
 * What the worship ministry side of the app lets the user do, as the profile reports it. Not a scope
 * level: membership comes from the user's ministries, which `Access` does not model.
 *
 * @param isWorshipMember the linked member belongs to the "Louvor" ministry — receives the Sunday setlist.
 * @param canSaveSetlist a worship member with `manage` on `songs` — may save the Sunday setlist.
 */
data class WorshipAccess(
    val isWorshipMember: Boolean,
    val canSaveSetlist: Boolean,
) {
    companion object {
        /** No profile data: loading, failed, or signed out. */
        val NONE = WorshipAccess(isWorshipMember = false, canSaveSetlist = false)
    }
}
