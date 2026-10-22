package com.ipb.castelobranco.core.data.push

import com.ipb.castelobranco.core.data.app.AppForegroundState
import com.ipb.castelobranco.core.domain.access.AccessRequestType
import com.ipb.castelobranco.core.domain.auth.SessionPresenceProvider
import com.ipb.castelobranco.core.testing.FakeAccessRepository
import com.ipb.castelobranco.core.domain.worship.WorshipAccess
import com.ipb.castelobranco.core.testing.FakeWorshipAccessRepository
import com.ipb.castelobranco.core.testing.WORSHIP_MEMBER
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PushMessageHandlerTest {

    private class FakeNotifier : SetlistNotifier {
        val setlistSaved = mutableListOf<LocalDate>()
        val confirmPlays = mutableListOf<LocalDate>()
        override fun showSetlistSaved(date: LocalDate) {
            setlistSaved += date
        }
        override fun showConfirmPlays(date: LocalDate) {
            confirmPlays += date
        }
    }

    private class FakeAccessNotifier : AccessRequestNotifier {
        val shown = mutableListOf<String>()
        override fun showCreated(type: AccessRequestType) {
            shown += "created $type"
        }
        override fun showApproved(type: AccessRequestType) {
            shown += "approved $type"
        }
        override fun showDenied(type: AccessRequestType, reason: String?) {
            shown += "denied $type $reason"
        }
    }

    private val accessNotifier = FakeAccessNotifier()
    private val profileAccess = FakeAccessRepository()

    private val sunday = LocalDate.of(2026, 10, 4)
    private val setlistSaved = mapOf("type" to "setlist_saved", "date" to "2026-10-04")
    private val confirmPlays = mapOf("type" to "confirm_plays", "date" to "2026-10-04")

    private val notifier = FakeNotifier()
    private var refreshes = 0
    private val foreground = AppForegroundState()

    private fun handler(loggedIn: Boolean = true, access: WorshipAccess = WORSHIP_MEMBER) = PushMessageHandler(
        sessionPresence = SessionPresenceProvider { loggedIn },
        worshipAccess = FakeWorshipAccessRepository(access),
        foreground = foreground,
        notifier = notifier,
        refreshScheduler = SetlistRefreshScheduler { refreshes++ },
        accessNotifier = accessNotifier,
        accessRepository = profileAccess,
    )

    @Test
    fun `setlist saved refreshes and notifies a worship member in background`() = runTest {
        handler().handle(setlistSaved)

        assertEquals(1, refreshes)
        assertEquals(listOf(sunday), notifier.setlistSaved)
    }

    @Test
    fun `setlist saved in foreground refreshes without notifying`() = runTest {
        foreground.isForeground = true

        handler().handle(setlistSaved)

        assertEquals(1, refreshes)
        assertTrue(notifier.setlistSaved.isEmpty())
    }

    @Test
    fun `setlist saved is ignored outside the worship ministry`() = runTest {
        handler(access = WorshipAccess.NONE).handle(setlistSaved)

        assertEquals(0, refreshes)
        assertTrue(notifier.setlistSaved.isEmpty())
    }

    @Test
    fun `confirm plays notifies even in foreground`() = runTest {
        foreground.isForeground = true

        handler().handle(confirmPlays)

        assertEquals(listOf(sunday), notifier.confirmPlays)
    }

    @Test
    fun `everything is ignored while logged out`() = runTest {
        val loggedOut = handler(loggedIn = false)

        loggedOut.handle(setlistSaved)
        loggedOut.handle(confirmPlays)

        assertEquals(0, refreshes)
        assertTrue(notifier.setlistSaved.isEmpty() && notifier.confirmPlays.isEmpty())
    }

    @Test
    fun `an unknown message does nothing`() = runTest {
        handler().handle(mapOf("type" to "other", "date" to "2026-10-04"))

        assertEquals(0, refreshes)
    }

    @Test
    fun `a new request notifies without reading the profile`() = runTest {
        handler().handle(mapOf("type" to "access_request_created", "request_type" to "access"))

        assertEquals(listOf("created ACCESS"), accessNotifier.shown)
        assertEquals(0, profileAccess.refreshCalls)
    }

    @Test
    fun `a decision notifies the requester and reads the profile again`() = runTest {
        handler().handle(mapOf("type" to "access_request_approved", "request_type" to "access"))
        handler().handle(
            mapOf("type" to "access_request_denied", "request_type" to "data_correction", "reason" to "Já está certo.")
        )

        assertEquals(
            listOf("approved ACCESS", "denied DATA_CORRECTION Já está certo."),
            accessNotifier.shown,
        )
        assertEquals(2, profileAccess.refreshCalls)
    }

    @Test
    fun `access request messages are ignored while logged out`() = runTest {
        handler(loggedIn = false).handle(mapOf("type" to "access_request_approved", "request_type" to "access"))

        assertTrue(accessNotifier.shown.isEmpty())
        assertEquals(0, profileAccess.refreshCalls)
    }
}
