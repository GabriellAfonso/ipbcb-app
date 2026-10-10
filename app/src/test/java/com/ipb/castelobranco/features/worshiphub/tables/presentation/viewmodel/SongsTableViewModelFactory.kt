package com.ipb.castelobranco.features.worshiphub.tables.presentation.viewmodel

import com.ipb.castelobranco.core.domain.util.DateProvider
import com.ipb.castelobranco.core.domain.worship.ObserveWorshipAccessUseCase
import com.ipb.castelobranco.core.testing.FakeSundaySetlistRepository
import com.ipb.castelobranco.core.testing.FakeWallClock
import com.ipb.castelobranco.core.testing.FakeWorshipAccessRepository
import com.ipb.castelobranco.features.worshiphub.tables.domain.FakeRepertoireDraftRepository
import com.ipb.castelobranco.features.worshiphub.tables.domain.FakeSetlistSaveRepository
import com.ipb.castelobranco.features.worshiphub.tables.domain.repository.SongsRepository
import com.ipb.castelobranco.features.worshiphub.tables.domain.usecase.ClearRepertoireDraftUseCase
import com.ipb.castelobranco.features.worshiphub.tables.domain.usecase.RestoreRepertoireDraftUseCase
import com.ipb.castelobranco.features.worshiphub.tables.domain.usecase.SaveRepertoireDraftUseCase
import com.ipb.castelobranco.features.worshiphub.tables.domain.usecase.SaveSundaySetlistUseCase
import com.ipb.castelobranco.features.worshiphub.tables.domain.usecase.SearchSundaysUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.time.LocalDate

/**
 * Builds the ViewModel with fakes for everything but [repository]. [defaultDispatcher] defaults to Main, which
 * the tests swap for their test dispatcher, so search filtering runs on the test scheduler.
 */
fun songsTableViewModel(
    repository: SongsRepository,
    draftRepository: FakeRepertoireDraftRepository = FakeRepertoireDraftRepository(),
    clock: FakeWallClock = FakeWallClock(),
    worshipAccess: FakeWorshipAccessRepository = FakeWorshipAccessRepository(),
    today: LocalDate = LocalDate.of(2026, 9, 30),
    saveRepository: FakeSetlistSaveRepository = FakeSetlistSaveRepository(),
    sundaySetlist: FakeSundaySetlistRepository = FakeSundaySetlistRepository(),
    defaultDispatcher: CoroutineDispatcher = Dispatchers.Main,
) = SongsTableViewModel(
    repository = repository,
    restoreDraft = RestoreRepertoireDraftUseCase(draftRepository, clock),
    saveDraft = SaveRepertoireDraftUseCase(draftRepository, clock),
    clearDraft = ClearRepertoireDraftUseCase(draftRepository),
    observeWorshipAccess = ObserveWorshipAccessUseCase(worshipAccess),
    dateProvider = DateProvider { today },
    saveSetlist = SaveSundaySetlistUseCase(saveRepository, sundaySetlist),
    searchSundays = SearchSundaysUseCase(),
    defaultDispatcher = defaultDispatcher,
)
