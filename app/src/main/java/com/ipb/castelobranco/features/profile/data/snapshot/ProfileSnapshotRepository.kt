package com.ipb.castelobranco.features.profile.data.snapshot


import com.ipb.castelobranco.core.domain.snapshot.BaseSnapshotRepository
import com.ipb.castelobranco.core.domain.snapshot.Logger
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.features.profile.data.access.toAccess
import com.ipb.castelobranco.features.profile.data.dto.MeProfileDto
import com.ipb.castelobranco.features.profile.domain.model.MeProfile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileSnapshotRepository @Inject constructor(
    cache: SnapshotCache<MeProfileDto>,
    fetcher: SnapshotFetcher<MeProfileDto>,
    logger: Logger
) : BaseSnapshotRepository<MeProfileDto, MeProfile>(
    cache = cache,
    fetcher = fetcher,
    mapper = { dto ->
        MeProfile(
            name = dto.name,
            isMember = dto.isMember,
            photoUrl = dto.photoUrl,
            access = dto.toAccess(),
            memberId = dto.memberId,
        )
    },
    logger = logger,
    tag = "ProfileSnapshot"
)
