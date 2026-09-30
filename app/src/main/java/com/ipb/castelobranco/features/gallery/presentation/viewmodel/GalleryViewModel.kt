package com.ipb.castelobranco.features.gallery.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import coil.ImageLoader
import com.ipb.castelobranco.core.data.NetworkConnectivityObserver
import com.ipb.castelobranco.core.di.DefaultDispatcher
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.core.domain.error.toAppError
import com.ipb.castelobranco.features.gallery.data.work.GalleryDownloadWorker
import com.ipb.castelobranco.features.gallery.data.work.GalleryUploadWorker
import com.ipb.castelobranco.features.gallery.di.GalleryThumbnailLoader
import com.ipb.castelobranco.features.gallery.domain.manage.AlbumDraft
import com.ipb.castelobranco.features.gallery.domain.manage.GalleryManageUseCases
import com.ipb.castelobranco.features.gallery.domain.manage.GalleryNames
import com.ipb.castelobranco.features.gallery.domain.manage.GalleryWriteError
import com.ipb.castelobranco.features.gallery.domain.manage.NameCheck
import com.ipb.castelobranco.features.gallery.domain.manage.ReorderResult
import com.ipb.castelobranco.features.gallery.domain.model.GalleryLocalState
import com.ipb.castelobranco.features.gallery.domain.model.GalleryTree
import com.ipb.castelobranco.features.gallery.domain.model.TreeTarget
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import com.ipb.castelobranco.features.gallery.domain.trash.RestoreResult
import com.ipb.castelobranco.features.gallery.domain.trash.TrashKey
import com.ipb.castelobranco.features.gallery.domain.trash.TrashKind
import com.ipb.castelobranco.features.gallery.domain.upload.UploadItem
import com.ipb.castelobranco.features.gallery.domain.upload.UploadState
import com.ipb.castelobranco.features.gallery.domain.usecase.GalleryAutoDownloadUseCase
import com.ipb.castelobranco.features.gallery.domain.usecase.SyncGalleryUseCase
import com.ipb.castelobranco.features.gallery.presentation.state.AlbumTile
import com.ipb.castelobranco.features.gallery.presentation.state.AlbumUiState
import com.ipb.castelobranco.features.gallery.presentation.state.AlbumUploadsUiState
import com.ipb.castelobranco.features.gallery.presentation.state.ConfirmAction
import com.ipb.castelobranco.features.gallery.presentation.state.ConfirmState
import com.ipb.castelobranco.features.gallery.presentation.state.FailedUpload
import com.ipb.castelobranco.features.gallery.presentation.state.FormTarget
import com.ipb.castelobranco.features.gallery.presentation.state.GalleryDialogState
import com.ipb.castelobranco.features.gallery.presentation.state.GalleryDownloadState
import com.ipb.castelobranco.features.gallery.presentation.state.GalleryMessage
import com.ipb.castelobranco.features.gallery.presentation.state.GalleryPermissions
import com.ipb.castelobranco.features.gallery.presentation.state.GalleryRootUiState
import com.ipb.castelobranco.features.gallery.presentation.state.ItemFormState
import com.ipb.castelobranco.features.gallery.presentation.state.MessageAction
import com.ipb.castelobranco.features.gallery.presentation.state.MovePickerState
import com.ipb.castelobranco.features.gallery.presentation.state.MoveSubject
import com.ipb.castelobranco.features.gallery.presentation.state.PhotoTile
import com.ipb.castelobranco.features.gallery.presentation.state.PhotoViewerUiState
import com.ipb.castelobranco.features.gallery.presentation.state.toGalleryPermissions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Graph-scoped: one instance serves the root, every album entry on the back stack and the viewer.
 * Per-album and per-viewer state are memoized by key.
 *
 * Management controls follow the user's level on `gallery` ([permissions]). A write the server
 * refuses with 403 needs no refresh from here: the network layer already re-reads the profile on
 * `PERMISSION_DENIED`, and the controls follow the new access when it arrives.
 */
@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val repository: GalleryRepository,
    private val syncGallery: SyncGalleryUseCase,
    private val autoDownload: GalleryAutoDownloadUseCase,
    private val manage: GalleryManageUseCases,
    observeAccess: ObserveAccessUseCase,
    connectivityObserver: NetworkConnectivityObserver,
    workManager: WorkManager,
    @param:GalleryThumbnailLoader val previewLoader: ImageLoader,
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private data class LocalView(val local: GalleryLocalState, val tree: GalleryTree?)

    /** Photos picked for a batch in one album. */
    private data class Selection(val albumId: Long, val ids: Set<Long>)

    /** Everything management adds on top of the local copy, combined once for every screen. */
    private data class ManageView(
        val permissions: GalleryPermissions,
        val organize: OrganizeSession?,
        val selection: Selection?,
        val uploads: List<UploadItem>,
        val copying: Set<Long>,
        val uploadProgress: Pair<Int, Int>,
    )

    /** The local copy with its tree, built once per change off the main thread. */
    private val view: StateFlow<LocalView?> = repository.localState
        .map { local -> LocalView(local, local.index?.let(::GalleryTree)) }
        .flowOn(defaultDispatcher)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _message = MutableStateFlow<GalleryMessage?>(null)

    /** Held until a screen shows it, so it survives the back-stack unwind of a removed album. */
    val message: StateFlow<GalleryMessage?> = _message.asStateFlow()

    private val _dialog = MutableStateFlow<GalleryDialogState?>(null)

    /** The form, picker or confirmation open over the top screen, if any. */
    val dialog: StateFlow<GalleryDialogState?> = _dialog.asStateFlow()

    val permissions: StateFlow<GalleryPermissions> = observeAccess()
        .map { it.toGalleryPermissions() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, GalleryPermissions.NONE)

    private val organize = MutableStateFlow<OrganizeSession?>(null)
    private val selection = MutableStateFlow<Selection?>(null)
    private val copying = MutableStateFlow<Set<Long>>(emptySet())

    /** Albums and photos this ViewModel itself removed: their screens say why, not "foi removido". */
    private val selfRemovedAlbums = mutableSetOf<Long>()
    private val selfRemovedPhotos = mutableSetOf<Long>()

    private val isOnWifi: StateFlow<Boolean> = connectivityObserver.isOnWifi
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    private val downloadState: StateFlow<GalleryDownloadState> = workManager
        .getWorkInfosForUniqueWorkFlow(GalleryDownloadWorker.WORK_NAME)
        .map { infos -> infos.firstOrNull().toDownloadState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), GalleryDownloadState())

    private val uploadProgress: StateFlow<Pair<Int, Int>> = workManager
        .getWorkInfosForUniqueWorkFlow(GalleryUploadWorker.WORK_NAME)
        .map { infos -> infos.firstOrNull { it.state == WorkInfo.State.RUNNING }.toUploadProgress() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), 0 to 0)

    private val manageView: StateFlow<ManageView> = combine(
        combine(permissions, organize, selection) { p, o, s -> Triple(p, o, s) },
        manage.observeUploads(),
        copying,
        uploadProgress,
    ) { (permissions, organize, selection), uploads, copying, progress ->
        ManageView(permissions, organize, selection, uploads, copying, progress)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        ManageView(GalleryPermissions.NONE, null, null, emptyList(), emptySet(), 0 to 0),
    )

    val rootState: StateFlow<GalleryRootUiState> = combine(
        view,
        repository.syncStatus,
        downloadState,
        isOnWifi,
        manageView,
    ) { view, status, download, wifi, manageView ->
        val index = view?.local?.index
        val isLoading = view == null || (index == null && (!status.hasAnswered || status.isRunning))
        val error = status.lastError.takeIf { index == null && !isLoading }
        val session = manageView.organize?.takeIf { it.albumId == null }
        val albums = view?.tree?.roots()?.map { GalleryUiMapper.albumTile(it, view.local) }.orEmpty()
        GalleryRootUiState(
            isLoading = isLoading,
            hasIndex = index != null,
            albums = session?.let { albums.inOrder(it.albumIds) { tile -> tile.id } } ?: albums,
            download = download,
            isOnWifi = wifi,
            syncError = error?.let(GalleryUiMapper::syncErrorMessage),
            syncErrorCode = error?.let(GalleryUiMapper::errorCode),
            showMembersOnlyNotice = index != null && GalleryUiMapper.isForbidden(status.lastError),
            permissions = manageView.permissions,
            isOrganizing = session != null,
            isSavingOrder = session?.isSaving == true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), GalleryRootUiState())

    private val albumStates = mutableMapOf<Long, StateFlow<AlbumUiState>>()
    private val viewers = mutableMapOf<Pair<Long, Long>, Viewer>()

    init {
        // Abrir a galeria é um dos gatilhos do sync.
        sync()
        // "Organizar" recusado: a lista é refeita quando o sync trouxer a ordem do servidor.
        viewModelScope.launch {
            view.filterNotNull().collect { view ->
                val tree = view.tree ?: return@collect
                organize.update { session ->
                    if (session?.awaitingRefresh != true) return@update session
                    if (session.isUntouched) {
                        OrganizeSession.from(session.albumId, tree)
                    } else {
                        session.copy(awaitingRefresh = false)
                    }
                }
            }
        }
    }

    fun albumState(albumId: Long): StateFlow<AlbumUiState> = albumStates.getOrPut(albumId) {
        var wasPresent = false
        combine(view.filterNotNull(), manageView) { view, manageView ->
            val base = GalleryUiMapper.albumState(albumId, view.local, view.tree)
            if (base.isRemoved && wasPresent && albumId !in selfRemovedAlbums) post(GalleryMessage.AlbumRemoved)
            if (!base.isLoading && !base.isRemoved) wasPresent = true
            decorate(albumId, base, manageView)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AlbumUiState())
    }

    fun viewerState(albumId: Long, photoId: Long): StateFlow<PhotoViewerUiState> =
        viewers.getOrPut(albumId to photoId) { Viewer(albumId, photoId) }.state

    /** The pager settled on [currentPhotoId]; used to pick the next photo if this one leaves. */
    fun onPageChanged(albumId: Long, openedPhotoId: Long, currentPhotoId: Long) {
        viewers[albumId to openedPhotoId]?.current?.value = currentPhotoId
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun retrySync() = sync()

    fun downloadWithMobileData() {
        autoDownload.enqueueAnyNetwork()
    }

    /** Nova tentativa após falha — REPLACE para descartar o WorkInfo com erro. */
    fun retryDownload() {
        autoDownload.enqueueWifiOnly(replaceExisting = true)
    }

    // region forms

    fun openCreateAlbum(parentId: Long?) {
        _dialog.value = GalleryDialogState.Form(ItemFormState(FormTarget.NewAlbum(parentId)))
    }

    fun openEditAlbum(albumId: Long) {
        val album = tree()?.album(albumId) ?: return
        _dialog.value = GalleryDialogState.Form(
            ItemFormState(FormTarget.Album(albumId), album.name, album.description, album.eventDate)
        )
    }

    /** The name is edited without its extension, which is put back on save. */
    fun openEditPhoto(photoId: Long) {
        val photo = view.value?.local?.index?.photos?.get(photoId) ?: return
        _dialog.value = GalleryDialogState.Form(
            ItemFormState(
                FormTarget.Photo(photoId),
                name = photo.name.substringBeforeLast('.'),
                description = photo.description,
                date = photo.dateTaken,
            )
        )
    }

    fun onFormNameChange(name: String) = updateForm { it.copy(name = name, nameError = null) }

    fun onFormDescriptionChange(description: String) = updateForm { it.copy(description = description) }

    fun onFormDateChange(date: String?) = updateForm { it.copy(date = date) }

    fun saveForm() {
        val form = (_dialog.value as? GalleryDialogState.Form)?.form ?: return
        if (form.isSaving) return
        val name = when (val check = GalleryNames.validate(fullName(form))) {
            NameCheck.Empty -> return updateForm { it.copy(nameError = GalleryManageTexts.NAME_EMPTY) }
            NameCheck.TooLong -> return updateForm { it.copy(nameError = GalleryManageTexts.NAME_TOO_LONG) }
            is NameCheck.Valid -> check.name
        }
        updateForm { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = when (val target = form.target) {
                is FormTarget.NewAlbum ->
                    manage.createAlbum(AlbumDraft(name, form.description, form.date, target.parentId))
                is FormTarget.Album -> manage.editAlbum(target.albumId, name, form.description, form.date)
                is FormTarget.Photo -> manage.editPhoto(target.photoId, name, form.description, form.date)
            }
            result.fold(
                onSuccess = {
                    _dialog.value = null
                    post(if (form.target is FormTarget.NewAlbum) GalleryMessage.AlbumCreated else GalleryMessage.Saved)
                },
                onFailure = { error ->
                    when (val refusal = error.toAppError().toGalleryWriteError()) {
                        is GalleryWriteError.DuplicateName ->
                            updateForm { it.copy(isSaving = false, nameError = refusal.message) }
                        is GalleryWriteError.NotFound, is GalleryWriteError.Forbidden -> {
                            _dialog.value = null
                            post(GalleryMessage.Text(refusal.message))
                        }
                        else -> {
                            updateForm { it.copy(isSaving = false) }
                            post(GalleryMessage.Text(refusal.message))
                        }
                    }
                },
            )
        }
    }

    // endregion

    // region move

    fun openMoveAlbum(albumId: Long) {
        val tree = tree() ?: return
        _dialog.value = GalleryDialogState.MovePicker(
            MovePickerState(MoveSubject.Album(albumId), tree.moveTargetsForAlbum(albumId))
        )
    }

    /** The photos selected in [albumId], or the single [photoId] shown in the viewer. */
    fun openMovePhotos(albumId: Long, photoId: Long? = null) {
        val tree = tree() ?: return
        val ids = photoId?.let(::listOf) ?: selectedIds(albumId)
        if (ids.isEmpty()) return
        _dialog.value = GalleryDialogState.MovePicker(
            MovePickerState(
                MoveSubject.Photos(albumId, ids, fromViewer = photoId != null),
                tree.moveTargetsForPhotos(albumId),
            )
        )
    }

    fun chooseMoveTarget(target: TreeTarget) {
        val picker = (_dialog.value as? GalleryDialogState.MovePicker)?.picker ?: return
        if (picker.isSaving || !target.selectable) return
        _dialog.value = GalleryDialogState.MovePicker(picker.copy(isSaving = true))
        viewModelScope.launch {
            when (val subject = picker.subject) {
                is MoveSubject.Album -> {
                    val result = manage.moveAlbum(subject.albumId, target.albumId)
                    _dialog.value = null
                    post(result.fold({ GalleryMessage.Saved }, { failure(it) }))
                }
                is MoveSubject.Photos -> {
                    val destination = target.albumId ?: return@launch
                    if (subject.fromViewer) selfRemovedPhotos += subject.photoIds
                    val result = manage.movePhotos(subject.photoIds, destination)
                    _dialog.value = null
                    selection.value = null
                    post(
                        if (subject.fromViewer && result.failures.isEmpty()) {
                            GalleryMessage.PhotoMovedTo(target.name)
                        } else {
                            GalleryMessage.Text(GalleryManageTexts.moved(result, target.name))
                        }
                    )
                }
            }
        }
    }

    // endregion

    // region confirmations: delete and remove cover

    fun askDeleteAlbum(albumId: Long) {
        val tree = tree() ?: return
        val album = tree.album(albumId) ?: return
        confirm(
            ConfirmAction.DeleteAlbum(albumId),
            GalleryManageTexts.deleteAlbum(album.name, tree.subtreeCounts(albumId)),
            GalleryManageTexts.DELETE_LABEL,
        )
    }

    /** The photos selected in [albumId], or the single [photoId] shown in the viewer. */
    fun askDeletePhotos(albumId: Long, photoId: Long? = null) {
        val ids = photoId?.let(::listOf) ?: selectedIds(albumId)
        if (ids.isEmpty()) return
        confirm(
            ConfirmAction.DeletePhotos(albumId, ids, fromViewer = photoId != null),
            GalleryManageTexts.deletePhotos(ids.size),
            GalleryManageTexts.DELETE_LABEL,
        )
    }

    fun askRemoveCover(albumId: Long) {
        val album = tree()?.album(albumId) ?: return
        confirm(
            ConfirmAction.RemoveCover(albumId),
            GalleryManageTexts.removeCover(album.name),
            GalleryManageTexts.REMOVE_LABEL,
        )
    }

    fun confirm() {
        val confirm = (_dialog.value as? GalleryDialogState.Confirm)?.confirm ?: return
        if (confirm.isRunning) return
        _dialog.value = GalleryDialogState.Confirm(confirm.copy(isRunning = true))
        viewModelScope.launch {
            val message = when (val action = confirm.action) {
                is ConfirmAction.DeleteAlbum -> {
                    selfRemovedAlbums += action.albumId
                    manage.deleteAlbum(action.albumId).fold(
                        onSuccess = { GalleryMessage.AlbumTrashed(undoFor(TrashKey(TrashKind.ALBUM, action.albumId))) },
                        onFailure = {
                            selfRemovedAlbums -= action.albumId
                            failure(it)
                        },
                    )
                }
                is ConfirmAction.DeletePhotos -> {
                    selfRemovedPhotos += action.photoIds
                    val result = manage.deletePhotos(action.photoIds)
                    selection.value = null
                    // One photo = one trash entry, so it can be undone; a batch goes through the trash.
                    val single = action.photoIds.singleOrNull()
                    if (single != null && result.failures.isEmpty()) {
                        GalleryMessage.PhotoTrashed(undoFor(TrashKey(TrashKind.PHOTO, single)))
                    } else {
                        GalleryMessage.Text(GalleryManageTexts.deleted(result))
                    }
                }
                is ConfirmAction.RemoveCover ->
                    manage.removeCover(action.albumId).fold({ GalleryMessage.CoverRemoved }, { failure(it) })
            }
            _dialog.value = null
            post(message)
        }
    }

    fun dismissDialog() {
        val current = _dialog.value
        val busy = when (current) {
            is GalleryDialogState.Form -> current.form.isSaving
            is GalleryDialogState.MovePicker -> current.picker.isSaving
            is GalleryDialogState.Confirm -> current.confirm.isRunning
            null -> false
        }
        if (!busy) _dialog.value = null
    }

    // endregion

    // region undo

    /**
     * "Desfazer" on a "sent to the trash" message: restores it, with the trash's texts. Ignored when the
     * user lost `owner` meanwhile.
     */
    fun undo(action: MessageAction.Undo) {
        if (!permissions.value.canDelete) return
        viewModelScope.launch {
            val result = manage.restore(action.key)
            if (result == RestoreResult.Restored) {
                when (action.key.kind) {
                    TrashKind.ALBUM -> selfRemovedAlbums -= action.key.id
                    TrashKind.PHOTO -> selfRemovedPhotos -= action.key.id
                }
            }
            val open = (result as? RestoreResult.NameConflict)
                ?.takeIf { it.isOnDevice }
                ?.let { MessageAction.OpenAlbum(it.conflictingAlbumId) }
            post(GalleryMessage.Text(TrashTexts.result(result, action.key.kind), open))
        }
    }

    private fun undoFor(key: TrashKey): MessageAction.Undo? =
        MessageAction.Undo(key).takeIf { permissions.value.canDelete }

    // endregion

    // region covers

    /** "Trocar capa": [source] is the picked image's URI. */
    fun setCoverFromPicked(albumId: Long, source: String) {
        viewModelScope.launch {
            post(manage.setCover.fromPicked(albumId, source).fold({ GalleryMessage.CoverUpdated }, { failure(it) }))
        }
    }

    /** "Usar como capa" on the photo shown in the viewer. */
    fun useAsCover(albumId: Long, photoId: Long) {
        viewModelScope.launch {
            post(manage.setCover.fromPhoto(albumId, photoId).fold({ GalleryMessage.CoverUpdated }, { failure(it) }))
        }
    }

    // endregion

    // region selection

    fun onPhotoLongPress(albumId: Long, photoId: Long) {
        if (!permissions.value.canManage || organize.value != null) return
        selection.value = Selection(albumId, setOf(photoId))
    }

    fun togglePhotoSelection(albumId: Long, photoId: Long) {
        selection.update { current ->
            val ids = current?.takeIf { it.albumId == albumId }?.ids.orEmpty()
            val next = if (photoId in ids) ids - photoId else ids + photoId
            if (next.isEmpty()) null else Selection(albumId, next)
        }
    }

    fun clearSelection() {
        selection.value = null
    }

    // endregion

    // region organize

    /** [albumId] `null` = the root. Closes a selection in progress. */
    fun startOrganize(albumId: Long?) {
        val tree = tree() ?: return
        selection.value = null
        organize.value = OrganizeSession.from(albumId, tree)
    }

    fun moveOrganizeItem(fromKey: Any?, toKey: Any?) {
        organize.update { it?.takeUnless { session -> session.isSaving }?.move(fromKey, toKey) ?: it }
    }

    fun cancelOrganize() {
        if (organize.value?.isSaving != true) organize.value = null
    }

    fun saveOrganize() {
        val session = organize.value ?: return
        if (session.isSaving) return
        organize.value = session.copy(isSaving = true)
        viewModelScope.launch {
            when (val result = manage.reorder(session.toDraft())) {
                ReorderResult.Saved -> {
                    organize.value = null
                    post(GalleryMessage.Saved)
                }
                ReorderResult.Unchanged -> organize.value = null
                ReorderResult.OrderChanged -> {
                    val tree = tree()
                    organize.value = (tree?.let { OrganizeSession.from(session.albumId, it) } ?: session)
                        .copy(isSaving = false, awaitingRefresh = true)
                    post(GalleryMessage.OrderChanged)
                }
                is ReorderResult.Failed -> {
                    organize.update { it?.copy(isSaving = false) }
                    post(failure(result.error))
                }
            }
        }
    }

    // endregion

    // region uploads

    /** [sources] are the picked images' URIs; they are copied into the app before this returns. */
    fun addPhotos(albumId: Long, sources: List<String>) {
        if (sources.isEmpty()) return
        copying.update { it + albumId }
        viewModelScope.launch {
            try {
                manage.enqueueUploads(albumId, sources)
            } finally {
                copying.update { it - albumId }
            }
        }
    }

    fun dismissUpload(uploadId: String) {
        viewModelScope.launch { manage.dismissUpload(uploadId) }
    }

    // endregion

    private fun decorate(albumId: Long, base: AlbumUiState, manageView: ManageView): AlbumUiState {
        if (base.isLoading || base.isRemoved) return base.copy(permissions = manageView.permissions)
        val session = manageView.organize?.takeIf { it.albumId == albumId }
        val photoIds = base.photos.map { it.id }.toSet()
        val items = manageView.uploads.filter { it.albumId == albumId }
        val pending = items.count { it.state != UploadState.Failed }
        return base.copy(
            subAlbums = session?.let { base.subAlbums.inOrder(it.albumIds, AlbumTile::id) } ?: base.subAlbums,
            photos = session?.let { base.photos.inOrder(it.photoIds, PhotoTile::id) } ?: base.photos,
            permissions = manageView.permissions,
            isOrganizing = session != null,
            isSavingOrder = session?.isSaving == true,
            selection = manageView.selection?.takeIf { it.albumId == albumId }?.ids.orEmpty().intersect(photoIds),
            uploads = AlbumUploadsUiState(
                isCopying = albumId in manageView.copying,
                pending = pending,
                current = if (pending > 0) manageView.uploadProgress.first else 0,
                total = if (pending > 0) manageView.uploadProgress.second else 0,
                failed = items.filter { it.state == UploadState.Failed }.map {
                    FailedUpload(it.uploadId, it.displayName, it.failure.orEmpty())
                },
            ),
        )
    }

    /** The photos selected in [albumId], in grid order, without those gone meanwhile. */
    private fun selectedIds(albumId: Long): List<Long> {
        val ids = selection.value?.takeIf { it.albumId == albumId }?.ids ?: return emptyList()
        return tree()?.photosOf(albumId)?.map { it.id }?.filter { it in ids }.orEmpty()
    }

    private fun tree(): GalleryTree? = view.value?.tree

    private fun confirm(action: ConfirmAction, text: String, label: String) {
        _dialog.value = GalleryDialogState.Confirm(ConfirmState(action, text, label))
    }

    private fun updateForm(transform: (ItemFormState) -> ItemFormState) {
        _dialog.update { current ->
            if (current is GalleryDialogState.Form) GalleryDialogState.Form(transform(current.form)) else current
        }
    }

    /** A photo keeps its extension: it is its file name when saved to the device. */
    private fun fullName(form: ItemFormState): String {
        val target = form.target as? FormTarget.Photo ?: return form.name
        val original = view.value?.local?.index?.photos?.get(target.photoId)?.name.orEmpty()
        val extension = original.substringAfterLast('.', "")
        return if (extension.isEmpty() || form.name.isBlank()) form.name else "${form.name.trim()}.$extension"
    }

    private fun failure(error: Throwable) = GalleryMessage.Text(error.toAppError().toGalleryWriteError().message)

    private fun sync() {
        viewModelScope.launch { syncGallery() }
    }

    private fun post(message: GalleryMessage) {
        _message.value = message
    }

    /**
     * One open viewer. It follows the photo on screen; when that photo leaves the album it moves to
     * the next one (the previous one when it was the last) and posts why — unless this ViewModel
     * moved or deleted it, which posts its own message.
     */
    private inner class Viewer(private val albumId: Long, openedPhotoId: Long) {
        val current = MutableStateFlow(openedPhotoId)
        private var lastIds: List<Long> = emptyList()

        val state: StateFlow<PhotoViewerUiState> = combine(
            view.filterNotNull(),
            current,
            permissions,
        ) { view, currentId, permissions ->
            val tree = view.tree ?: return@combine PhotoViewerUiState(isLoading = true)
            val photos = tree.photosOf(albumId)
            val ids = photos.map { it.id }
            var index = ids.indexOf(currentId)

            if (index < 0) {
                val left = lastIds.isNotEmpty() && currentId in lastIds
                if (left && currentId !in selfRemovedPhotos) {
                    val stillThere = view.local.index?.photos?.containsKey(currentId) == true
                    post(if (stillThere) GalleryMessage.PhotoMoved else GalleryMessage.PhotoRemoved)
                }
                if (ids.isEmpty()) {
                    lastIds = ids
                    return@combine PhotoViewerUiState(isLoading = false, isClosed = true, permissions = permissions)
                }
                val position = lastIds.indexOf(currentId)
                val next = lastIds.drop(position + 1).firstOrNull { it in ids }
                    ?: lastIds.take(position.coerceAtLeast(0)).lastOrNull { it in ids }
                    ?: ids.first()
                index = ids.indexOf(next)
                current.value = next
            }
            lastIds = ids
            PhotoViewerUiState(
                isLoading = false,
                photos = photos.map { GalleryUiMapper.viewerPhoto(it, view.local) },
                currentIndex = index,
                permissions = permissions,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PhotoViewerUiState())
    }

    private fun WorkInfo?.toDownloadState(): GalleryDownloadState = when (this?.state) {
        WorkInfo.State.RUNNING -> GalleryDownloadState(
            isDownloading = true,
            downloaded = progress.getInt(GalleryDownloadWorker.KEY_DOWNLOADED, 0),
            total = progress.getInt(GalleryDownloadWorker.KEY_TOTAL, 0),
            isResolved = true,
        )
        WorkInfo.State.ENQUEUED ->
            if (runAttemptCount > 0) GalleryDownloadState(isResuming = true, isResolved = true)
            else GalleryDownloadState(isPending = true, isResolved = true)
        WorkInfo.State.FAILED -> GalleryDownloadState(
            error = outputData.getString(GalleryDownloadWorker.KEY_ERROR) ?: DOWNLOAD_FAILED_MESSAGE,
            errorCode = outputData.getInt(GalleryDownloadWorker.KEY_ERROR_CODE, 0).takeIf { it != 0 },
            isResolved = true,
        )
        else -> GalleryDownloadState(isResolved = true)
    }

    private fun WorkInfo?.toUploadProgress(): Pair<Int, Int> =
        if (this == null) 0 to 0
        else progress.getInt(GalleryUploadWorker.KEY_CURRENT, 0) to progress.getInt(GalleryUploadWorker.KEY_TOTAL, 0)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val DOWNLOAD_FAILED_MESSAGE = "Falha ao baixar galeria"
    }
}

/** [items] in the order of [ids]; items not listed (added meanwhile) keep their place at the end. */
private fun <T> List<T>.inOrder(ids: List<Long>, idOf: (T) -> Long): List<T> {
    val byId = associateBy(idOf)
    val listed = ids.mapNotNull { byId[it] }
    return listed + filterNot { idOf(it) in ids }
}
