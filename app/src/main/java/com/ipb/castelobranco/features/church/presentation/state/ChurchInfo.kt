package com.ipb.castelobranco.features.church.presentation.state

import java.time.DayOfWeek
import java.time.LocalTime

data class ChurchService(val name: String, val day: DayOfWeek, val time: LocalTime)

/** The church's details, fixed in the app (`specs/church/spec.md`): changing one takes a new version. */
object ChurchInfo {
    const val NAME = "IPB Castelo Branco"
    const val DENOMINATION = "Igreja Presbiteriana do Brasil"
    const val ADDRESS = "R. Celestino de Almeida, 308 - Castelo Branco, Cariacica - ES"
    const val INSTAGRAM_URL = "https://www.instagram.com/ipb.castelobranco/"

    /** Static PIX code of the offerings, as the church's printed QR carries it. `R.drawable.church_pix_qr` encodes it. */
    const val PIX_CODE = "00020126330014br.gov.bcb.pix0111527528957045204000053039865802BR5921Jose Antonio De Souza" +
        "6009Sao Paulo62290525REC6886990A7D37171913914063049114"

    /** In week order starting on Monday, so Sunday comes last. */
    val services = listOf(
        ChurchService(name = "Culto de oração", day = DayOfWeek.TUESDAY, time = LocalTime.of(19, 30)),
        ChurchService(name = "Culto de oração", day = DayOfWeek.THURSDAY, time = LocalTime.of(19, 30)),
        ChurchService(name = "Escola bíblica dominical", day = DayOfWeek.SUNDAY, time = LocalTime.of(9, 0)),
        ChurchService(name = "Culto de adoração", day = DayOfWeek.SUNDAY, time = LocalTime.of(19, 30)),
    )
}
