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
import java.time.LocalDate

/** Builds the ViewModel with fakes for everything but [repository]. */
fun songsTableViewModel(
    repository: SongsRepository,
    draftRepository: FakeRepertoireDraftRepository = FakeRepertoireDraftRepository(),
    clock: FakeWallClock = FakeWallClock(),
    worshipAccess: FakeWorshipAccessRepository = FakeWorshipAccessRepository(),
    today: LocalDate = LocalDate.of(2026, 9, 30),
    saveRepository: FakeSetlistSaveRepository = FakeSetlistSaveRepository(),
    sundaySetlist: FakeSundaySetlistRepository = FakeSundaySetlistRepository(),
) = SongsTableViewModel(
    repository = repository,
    restoreDraft = RestoreRepertoireDraftUseCase(draftRepository, clock),
    saveDraft = SaveRepertoireDraftUseCase(draftRepository, clock),
    clearDraft = ClearRepertoireDraftUseCase(draftRepository),
    observeWorshipAccess = ObserveWorshipAccessUseCase(worshipAccess),
    dateProvider = DateProvider { today },
    saveSetlist = SaveSundaySetlistUseCase(saveRepository, sundaySetlist),
)
