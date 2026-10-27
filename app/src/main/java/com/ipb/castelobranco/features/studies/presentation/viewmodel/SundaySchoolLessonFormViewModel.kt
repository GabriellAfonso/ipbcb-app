package com.ipb.castelobranco.features.studies.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.core.domain.bible.BibleBookCatalog
import com.ipb.castelobranco.core.domain.bible.BibleBookInfo
import com.ipb.castelobranco.core.domain.error.toAppError
import com.ipb.castelobranco.core.presentation.error.toUserMessage
import com.ipb.castelobranco.features.studies.domain.model.BookFacts
import com.ipb.castelobranco.features.studies.domain.model.EditorLocation
import com.ipb.castelobranco.features.studies.domain.model.LessonBlock
import com.ipb.castelobranco.features.studies.domain.model.LessonDraft
import com.ipb.castelobranco.features.studies.domain.model.LessonIcon
import com.ipb.castelobranco.features.studies.domain.model.LessonSaveResult
import com.ipb.castelobranco.features.studies.domain.model.LessonSection
import com.ipb.castelobranco.features.studies.domain.model.LessonStatus
import com.ipb.castelobranco.features.studies.domain.model.ListItem
import com.ipb.castelobranco.features.studies.domain.model.ListStyle
import com.ipb.castelobranco.features.studies.domain.model.ProofText
import com.ipb.castelobranco.features.studies.domain.usecase.GetSundaySchoolLessonUseCase
import com.ipb.castelobranco.features.studies.domain.usecase.SaveSundaySchoolLessonUseCase
import com.ipb.castelobranco.features.studies.domain.usecase.ValidateLessonDraftUseCase
import com.ipb.castelobranco.features.studies.presentation.navigation.StudiesRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class LessonFormMode { EDIT, PREVIEW }

/** The kinds of block the editor creates in this version. */
enum class NewBlockKind { PARAGRAPH, LIST, KEY_VERSE }

data class SundaySchoolLessonFormUiState(
    val isNew: Boolean = true,
    val draft: LessonDraft? = null,
    /** What was loaded (or the empty new lesson): back with a different [draft] asks to discard. */
    val original: LessonDraft? = null,
    /** "2026 — Tema" for the preview header; the year alone for a new lesson. */
    val yearLabel: String = "",
    val books: Map<String, BookFacts> = emptyMap(),
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val isSaving: Boolean = false,
    val mode: LessonFormMode = LessonFormMode.EDIT,
    /** By form field (`title`, `year`, `date`, `teacher`). */
    val fieldErrors: Map<String, String> = emptyMap(),
    val locationErrors: Map<EditorLocation, List<String>> = emptyMap(),
    val catalog: List<BibleBookInfo> = emptyList(),
    val confirmDiscard: Boolean = false,
) {
    val hasChanges: Boolean get() = draft != null && draft != original
    val formErrors: List<String> get() = locationErrors[EditorLocation.Form].orEmpty()
}

sealed interface SundaySchoolLessonFormEvent {
    data class ShowMessage(val text: String) : SundaySchoolLessonFormEvent
    data object Close : SundaySchoolLessonFormEvent
}

@HiltViewModel
class SundaySchoolLessonFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getLesson: GetSundaySchoolLessonUseCase,
    private val saveLesson: SaveSundaySchoolLessonUseCase,
    private val validate: ValidateLessonDraftUseCase,
    private val catalog: BibleBookCatalog,
) : ViewModel() {

    private val lessonId: Int? = savedStateHandle.get<Int>(StudiesRoutes.ARG_LESSON_ID)
        ?.takeUnless { it == StudiesRoutes.NEW_LESSON }
    private val initialYear: Int = savedStateHandle.get<Int>(StudiesRoutes.ARG_YEAR) ?: LessonDraft.MIN_YEAR

    private val _uiState = MutableStateFlow(SundaySchoolLessonFormUiState(isNew = lessonId == null))
    val uiState: StateFlow<SundaySchoolLessonFormUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SundaySchoolLessonFormEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<SundaySchoolLessonFormEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            catalog.load()
            catalog.books.collect { books -> _uiState.update { it.copy(catalog = books) } }
        }
        load()
    }

    fun load() {
        if (lessonId == null) {
            val draft = LessonDraft.new(initialYear)
            _uiState.update {
                it.copy(draft = draft, original = draft, yearLabel = initialYear.toString(), isLoading = false)
            }
            return
        }
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            getLesson(lessonId).fold(
                onSuccess = { lesson ->
                    val draft = LessonDraft.of(lesson)
                    _uiState.update {
                        it.copy(
                            draft = draft,
                            original = draft,
                            yearLabel = lesson.year.label,
                            books = lesson.books,
                            isLoading = false,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, loadError = error.toAppError().toUserMessage()) }
                },
            )
        }
    }

    fun setMode(mode: LessonFormMode) {
        _uiState.update { it.copy(mode = mode) }
    }

    // ---- metadata ----

    fun setTitle(title: String) {
        editDraft(clearField = FIELD_TITLE) { it.copy(title = title) }
    }

    fun setYear(input: String) {
        val year = input.filter { it.isDigit() }.take(YEAR_DIGITS).toIntOrNull() ?: 0
        editDraft(clearField = FIELD_YEAR) { it.copy(year = year) }
    }

    fun setDate(date: LocalDate?) {
        editDraft(clearField = FIELD_DATE) { it.copy(date = date) }
    }

    fun setTeacher(teacher: String) {
        editDraft(clearField = FIELD_TEACHER) { it.copy(teacher = teacher) }
    }

    fun setStatus(status: LessonStatus) {
        editDraft { it.copy(status = status) }
    }

    // ---- sections ----

    fun addSection() {
        editSections(structural = true) { sections ->
            sections + LessonSection(title = "", iconName = null, blocks = listOf(LessonBlock.Paragraph("")))
        }
    }

    fun setSectionTitle(section: Int, title: String) {
        editSection(section) { it.copy(title = title) }
    }

    fun setSectionIcon(section: Int, icon: LessonIcon?) {
        editSection(section) { it.copy(iconName = icon?.wireName) }
    }

    fun moveSection(section: Int, delta: Int) {
        editSections(structural = true) { it.moved(section, delta) }
    }

    fun deleteSection(section: Int) {
        editSections(structural = true) { sections ->
            sections.filterIndexed { index, _ -> index != section }
        }
    }

    // ---- blocks ----

    fun addBlock(section: Int, kind: NewBlockKind) {
        val block = when (kind) {
            NewBlockKind.PARAGRAPH -> LessonBlock.Paragraph("")
            NewBlockKind.LIST -> LessonBlock.BulletList(ListStyle.BULLET, listOf(ListItem("")))
            NewBlockKind.KEY_VERSE -> LessonBlock.KeyVerse(label = null, text = "", proof = ProofText("", null))
        }
        editSection(section, structural = true) { it.copy(blocks = it.blocks + block) }
    }

    /** Only editable blocks are replaced; the others pass through unchanged (FR-019). */
    fun updateBlock(section: Int, block: Int, value: LessonBlock) {
        editSection(section) { current ->
            val old = current.blocks.getOrNull(block) ?: return@editSection current
            if (!old.isEditable) return@editSection current
            current.copy(blocks = current.blocks.mapIndexed { index, it -> if (index == block) value else it })
        }
    }

    fun moveBlock(section: Int, block: Int, delta: Int) {
        editSection(section, structural = true) {
            it.copy(blocks = it.blocks.moved(block, delta))
        }
    }

    fun deleteBlock(section: Int, block: Int) {
        editSection(section, structural = true) {
            it.copy(blocks = it.blocks.filterIndexed { index, _ -> index != block })
        }
    }

    // ---- save / leave ----

    fun save() {
        val state = _uiState.value
        val draft = state.draft ?: return
        if (state.isSaving) return
        val problems = validate(draft)
        if (!problems.isEmpty) {
            _uiState.update { it.copy(fieldErrors = problems.fields, locationErrors = problems.locations) }
            _events.tryEmit(SundaySchoolLessonFormEvent.ShowMessage(FIX_ERRORS))
            return
        }
        _uiState.update { it.copy(isSaving = true, fieldErrors = emptyMap(), locationErrors = emptyMap()) }
        viewModelScope.launch {
            when (val result = saveLesson(lessonId, draft)) {
                is LessonSaveResult.Saved -> {
                    _uiState.update { it.copy(isSaving = false, original = it.draft) }
                    _events.tryEmit(SundaySchoolLessonFormEvent.ShowMessage("Aula salva"))
                    _events.tryEmit(SundaySchoolLessonFormEvent.Close)
                }
                is LessonSaveResult.Invalid -> {
                    val locations = result.contentErrors
                        .groupBy({ EditorLocation.of(it.path) }, { it.message })
                    val fields = result.fieldErrors
                        .filterKeys { it in FORM_FIELDS }
                        .mapValues { (_, messages) -> messages.joinToString(LINE_BREAK) }
                    val unplaced = result.fieldErrors.filterKeys { it !in FORM_FIELDS }.values.flatten()
                    val extra = unplaced.ifEmpty {
                        if (locations.isEmpty() && fields.isEmpty()) listOfNotNull(result.message) else emptyList()
                    }
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            fieldErrors = fields,
                            locationErrors = locations.mergeForm(extra),
                        )
                    }
                    _events.tryEmit(SundaySchoolLessonFormEvent.ShowMessage(FIX_ERRORS))
                }
                is LessonSaveResult.Failed -> {
                    _uiState.update { it.copy(isSaving = false) }
                    _events.tryEmit(SundaySchoolLessonFormEvent.ShowMessage(result.error.toUserMessage()))
                }
            }
        }
    }

    /** Back: leaves at once without changes, otherwise asks "Descartar alterações?". */
    fun onBack() {
        if (_uiState.value.hasChanges) {
            _uiState.update { it.copy(confirmDiscard = true) }
        } else {
            _events.tryEmit(SundaySchoolLessonFormEvent.Close)
        }
    }

    fun dismissDiscard() {
        _uiState.update { it.copy(confirmDiscard = false) }
    }

    fun discard() {
        _uiState.update { it.copy(confirmDiscard = false) }
        _events.tryEmit(SundaySchoolLessonFormEvent.Close)
    }

    // ---- helpers ----

    private fun editDraft(clearField: String? = null, change: (LessonDraft) -> LessonDraft) {
        _uiState.update { state ->
            val draft = state.draft ?: return@update state
            state.copy(
                draft = change(draft),
                fieldErrors = if (clearField == null) state.fieldErrors else state.fieldErrors - clearField,
            )
        }
    }

    /**
     * A structural change (add, move, delete) shifts the positions server errors point to, so those errors are
     * dropped; a text edit keeps them until the next save.
     */
    private fun editSections(structural: Boolean = false, change: (List<LessonSection>) -> List<LessonSection>) {
        _uiState.update { state ->
            val draft = state.draft ?: return@update state
            val content = draft.content.copy(sections = change(draft.content.sections))
            state.copy(
                draft = draft.copy(content = content),
                locationErrors = if (structural) state.locationErrors.filterKeys { it == EditorLocation.Form }
                else state.locationErrors,
            )
        }
    }

    private fun editSection(
        section: Int,
        structural: Boolean = false,
        change: (LessonSection) -> LessonSection,
    ) {
        editSections(structural) { sections ->
            sections.mapIndexed { index, it -> if (index == section) change(it) else it }
        }
    }

    private fun Map<EditorLocation, List<String>>.mergeForm(extra: List<String>): Map<EditorLocation, List<String>> {
        if (extra.isEmpty()) return this
        return this + (EditorLocation.Form to (this[EditorLocation.Form].orEmpty() + extra))
    }

    companion object {
        const val FIELD_TITLE = "title"
        const val FIELD_YEAR = "year"
        const val FIELD_DATE = "date"
        const val FIELD_TEACHER = "teacher"
        private val FORM_FIELDS = setOf(FIELD_TITLE, FIELD_YEAR, FIELD_DATE, FIELD_TEACHER)
        private const val YEAR_DIGITS = 4
        const val FIX_ERRORS = "Corrija os campos marcados"
        private const val LINE_BREAK = "\n"
    }
}

/** [index] moved by [delta] positions, clamped to the list. */
internal fun <T> List<T>.moved(index: Int, delta: Int): List<T> {
    val target = (index + delta).coerceIn(0, lastIndex)
    if (index !in indices || target == index) return this
    return toMutableList().apply { add(target, removeAt(index)) }
}
