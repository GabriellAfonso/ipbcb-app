package com.ipb.castelobranco.features.profile.data.snapshot


import com.ipb.castelobranco.core.domain.snapshot.BaseSnapshotRepository
import com.ipb.castelobranco.core.domain.snapshot.Logger
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.features.profile.data.access.toAccess
import com.ipb.castelobranco.features.profile.data.dto.MeProfileDto
import com.ipb.castelobranco.features.profile.domain.model.MeProfile
import java.time.LocalDate
import java.time.format.DateTimeParseException
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
            isWorshipMember = dto.isWorshipMember,
            canSaveSetlist = dto.canSaveSetlist,
            username = dto.username?.takeIf { it.isNotBlank() },
            hasPassword = dto.hasPassword,
            dateJoined = dto.dateJoined.toLocalDateOrNull(),
        )
    },
    logger = logger,
    tag = "ProfileSnapshot"
)

/** An unparseable date is "unknown", never a failure: a throw here would lose the whole cached profile. */
internal fun String?.toLocalDateOrNull(): LocalDate? =
    this?.let {
        try {
            LocalDate.parse(it)
        } catch (_: DateTimeParseException) {
            null
        }
    }
