package com.ipb.castelobranco.features.church.presentation.state

data class ChurchService(val name: String, val day: String, val time: String)

/** The church's details, fixed in the app (`specs/church/spec.md`): changing one takes a new version. */
object ChurchInfo {
    const val NAME = "IPB Castelo Branco"
    const val ADDRESS = "R. Celestino de Almeida, 308 - Castelo Branco, Cariacica - ES"
    const val INSTAGRAM_HANDLE = "@ipb.castelobranco"
    const val INSTAGRAM_URL = "https://www.instagram.com/ipb.castelobranco/"

    /** Static PIX code of the offerings, as the church's printed QR carries it. `R.drawable.church_pix_qr` encodes it. */
    const val PIX_CODE = "00020126330014br.gov.bcb.pix0111527528957045204000053039865802BR5921Jose Antonio De Souza" +
        "6009Sao Paulo62290525REC6886990A7D37171913914063049114"

    val services = listOf(
        ChurchService(name = "EBD", day = "Domingo", time = "09:00"),
        ChurchService(name = "Culto", day = "Domingo", time = "19:30"),
        ChurchService(name = "Culto de oração", day = "Terça", time = "19:30"),
        ChurchService(name = "Culto de oração", day = "Quinta", time = "19:30"),
    )
}
