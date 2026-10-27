package com.ipb.castelobranco.features.studies.presentation.components.sundayschool

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.R
import com.ipb.castelobranco.features.studies.domain.model.LessonIcon

/** Phosphor duotone drawing of each icon of the closed list (research R-06). */
@DrawableRes
fun LessonIcon.drawable(): Int = when (this) {
    LessonIcon.HEART -> R.drawable.ic_ph_heart
    LessonIcon.SCALES -> R.drawable.ic_ph_scales
    LessonIcon.SHIELD -> R.drawable.ic_ph_shield
    LessonIcon.PLANT -> R.drawable.ic_ph_plant
    LessonIcon.HANDS_PRAYING -> R.drawable.ic_ph_hands_praying
    LessonIcon.CROWN -> R.drawable.ic_ph_crown
    LessonIcon.CROSS -> R.drawable.ic_ph_cross
    LessonIcon.BOOK_OPEN -> R.drawable.ic_ph_book_open
    LessonIcon.SCROLL -> R.drawable.ic_ph_scroll
    LessonIcon.FLAME -> R.drawable.ic_ph_flame
    LessonIcon.BIRD -> R.drawable.ic_ph_bird
    LessonIcon.CHURCH -> R.drawable.ic_ph_church
    LessonIcon.HOURGLASS -> R.drawable.ic_ph_hourglass
    LessonIcon.SWORD -> R.drawable.ic_ph_sword
    LessonIcon.MOUNTAINS -> R.drawable.ic_ph_mountains
    LessonIcon.LIGHTNING -> R.drawable.ic_ph_lightning
}

/** Portuguese name of each icon, for the editor's picker. */
val LessonIcon.label: String
    get() = when (this) {
        LessonIcon.HEART -> "Coração"
        LessonIcon.SCALES -> "Balança"
        LessonIcon.SHIELD -> "Escudo"
        LessonIcon.PLANT -> "Planta"
        LessonIcon.HANDS_PRAYING -> "Mãos em oração"
        LessonIcon.CROWN -> "Coroa"
        LessonIcon.CROSS -> "Cruz"
        LessonIcon.BOOK_OPEN -> "Livro aberto"
        LessonIcon.SCROLL -> "Pergaminho"
        LessonIcon.FLAME -> "Chama"
        LessonIcon.BIRD -> "Pomba"
        LessonIcon.CHURCH -> "Igreja"
        LessonIcon.HOURGLASS -> "Ampulheta"
        LessonIcon.SWORD -> "Espada"
        LessonIcon.MOUNTAINS -> "Montanhas"
        LessonIcon.LIGHTNING -> "Raio"
    }

/** The icon in one colour: the duotone background is the same colour at 20 %. */
@Composable
fun LessonIconImage(
    icon: LessonIcon,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    Icon(
        painter = painterResource(icon.drawable()),
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(size),
    )
}
