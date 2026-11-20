package telefam.company.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.telefam.company.app.R
import telefam.company.core.PostBackgroundTheme
import telefam.company.core.PostFontOption
import telefam.company.core.PostMode
import telefam.company.core.PostUploadStatus
import telefam.company.core.TelefamPostRules
import telefam.company.media.PreparedPostVideo
import telefam.company.media.ProcessedPostPhoto
import telefam.company.ui.components.DashedMediaUploadCard
import telefam.company.ui.components.PostMetadataRowsCard
import telefam.company.ui.components.PostThemeCanvasBox
import telefam.company.ui.components.postCardTextStyle
import telefam.company.ui.components.postFontFamily
import telefam.company.ui.theme.LocalTelefamDarkTheme
import telefam.company.ui.theme.TelefamBlushWave
import telefam.company.ui.theme.TelefamCrimson
import telefam.company.ui.theme.TelefamDarkBlush
import telefam.company.ui.theme.TelefamPureWhite
import telefam.company.ui.viewmodel.ContactUiItem
import telefam.company.ui.viewmodel.PostComposerUiState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TelefamPostScreen(
  state: PostComposerUiState,
  onClose: () -> Unit,
  onSelectMode: (PostMode) -> Unit,
  onTextChanged: (String) -> Unit,
  onCaptionChanged: (String) -> Unit,
  onSelectTheme: (String) -> Unit,
  onSelectFont: (PostFontOption) -> Unit,
  onPhotosPicked: (List<Uri>) -> Unit,
  onRemovePhoto: (String) -> Unit,
  onVideoPicked: (Uri?) -> Unit,
  onClearVideo: () -> Unit,
  onTrimChanged: (Long, Long) -> Unit,
  onSelectThumbnailFrame: (Int) -> Unit,
  onToggleMuteOriginalAudio: () -> Unit,
  onOpenSongSheet: () -> Unit,
  onDismissSongSheet: () -> Unit,
  onSongQueryChanged: (String) -> Unit,
  onSelectSong: (telefam.company.core.PostSongTrack) -> Unit,
  onToggleSongPreview: (telefam.company.core.PostSongTrack) -> Unit,
  onClearSong: () -> Unit,
  onOpenTagPeopleSheet: () -> Unit,
  onDismissTagPeopleSheet: () -> Unit,
  onTagPeopleQueryChanged: (String) -> Unit,
  onTogglePendingTagPerson: (String) -> Unit,
  onConfirmTaggedPeople: () -> Unit,
  onOpenHashtagsSheet: () -> Unit,
  onDismissHashtagsSheet: () -> Unit,
  onHashtagQueryChanged: (String) -> Unit,
  onToggleHashtag: (String) -> Unit,
  onShare: () -> Unit,
) {
  BackHandler(enabled = true) {
    when {
      state.showTagPeopleSheet -> onDismissTagPeopleSheet()
      state.showSongSheet -> onDismissSongSheet()
      state.showHashtagsSheet -> onDismissHashtagsSheet()
      else -> onClose()
    }
  }

  // Zero-permission Photo Picker launchers (Play Policy compliant)
  val photosPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia(TelefamPostRules.MAX_PHOTOS_COUNT),
    onResult = onPhotosPicked,
  )
  val videoPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
    onResult = onVideoPicked,
  )

  val launchPhotoPicker = {
    photosPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
  }
  val launchVideoPicker = {
    videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
  }

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background,
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Top Crimson Header + Mode Switcher matching Reference Images 1 & 3
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(TelefamCrimson)
          .statusBarsPadding()
          .padding(horizontal = 14.dp, vertical = 10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(
            onClick = onClose,
            modifier = Modifier.testTag("post_close_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Close,
              contentDescription = stringResource(R.string.cd_close_post),
              tint = TelefamPureWhite,
            )
          }

          Text(
            text = if (state.editingPostId != null) {
              stringResource(R.string.post_edit_title)
            } else {
              stringResource(R.string.post_new_title)
            },
            style = MaterialTheme.typography.titleMedium.copy(
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
            ),
            color = TelefamPureWhite,
          )

          Spacer(modifier = Modifier.weight(1f))

          val shareLabel = when (state.uploadStatus) {
            PostUploadStatus.PREPARING -> "..."
            PostUploadStatus.UPLOADING -> "${state.uploadProgressPct}%"
            PostUploadStatus.UPLOADED -> stringResource(R.string.post_uploaded)
            else -> if (state.editingPostId != null) {
              stringResource(R.string.post_save_changes)
            } else {
              stringResource(R.string.post_share)
            }
          }

          Surface(
            onClick = { if (state.canShare) onShare() },
            enabled = state.canShare,
            shape = RoundedCornerShape(20.dp),
            color = if (state.canShare) TelefamPureWhite else TelefamPureWhite.copy(alpha = 0.72f),
            modifier = Modifier.testTag("post_share_button"),
          ) {
            Text(
              text = shareLabel,
              style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
              ),
              color = TelefamCrimson.copy(alpha = if (state.canShare) 1f else 0.55f),
              modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          PostMode.entries.forEach { mode ->
            val selected = state.mode == mode
            val icon = when (mode) {
              PostMode.TEXT -> Icons.Filled.Title
              PostMode.PHOTOS -> Icons.Outlined.PhotoLibrary
              PostMode.VIDEO -> Icons.Outlined.VideoLibrary
            }
            val label = when (mode) {
              PostMode.TEXT -> stringResource(R.string.post_mode_text)
              PostMode.PHOTOS -> stringResource(R.string.post_mode_photos)
              PostMode.VIDEO -> stringResource(R.string.post_mode_video)
            }
            Surface(
              onClick = { if (state.editingPostId == null) onSelectMode(mode) },
              shape = RoundedCornerShape(20.dp),
              color = if (selected) TelefamPureWhite else Color.Transparent,
              modifier = Modifier.testTag("post_mode_${mode.apiValue}"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = icon,
                  contentDescription = null,
                  tint = if (selected) TelefamCrimson else TelefamPureWhite,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 13.5.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                  ),
                  color = if (selected) TelefamCrimson else TelefamPureWhite,
                )
              }
            }
          }
        }
      }

      if (state.uploadStatus == PostUploadStatus.UPLOADING || state.uploadStatus == PostUploadStatus.PREPARING) {
        LinearProgressIndicator(
          progress = {
            if (state.uploadStatus == PostUploadStatus.PREPARING) 0.2f
            else (state.uploadProgressPct / 100f).coerceIn(0.02f, 1f)
          },
          color = TelefamCrimson,
          trackColor = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.fillMaxWidth().height(3.dp),
        )
      }

      // Main adaptive body: Single-column scrollable on phones, balanced 2-pane on Tablets/Foldables/Desktops
      BoxWithConstraints(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .navigationBarsPadding(),
      ) {
        val isWide = maxWidth >= 680.dp
        val activeTheme = TelefamPostRules.findTheme(state.selectedThemeId)
        val taggedSummary = when (state.taggedPeople.size) {
          0 -> ""
          1 -> "@${state.taggedPeople.first().username}"
          else -> "@${state.taggedPeople.first().username} +${state.taggedPeople.size - 1}"
        }
        val activePreviewTrack = state.selectedSong?.let { s ->
          s.trackId == state.playingPreviewTrackId
        } ?: false

        if (isWide) {
          Row(
            modifier = Modifier
              .fillMaxSize()
              .widthIn(max = 1040.dp)
              .align(Alignment.TopCenter)
              .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
          ) {
            Column(
              modifier = Modifier
                .weight(1.05f)
                .verticalScroll(rememberScrollState()),
            ) {
              when (state.mode) {
                PostMode.TEXT -> TextComposerCard(
                  text = state.textContent,
                  theme = activeTheme,
                  fontOption = state.selectedFont,
                  onTextChanged = onTextChanged,
                )
                PostMode.PHOTOS -> PhotosComposerCanvas(
                  photos = state.photos,
                  onLaunchPicker = launchPhotoPicker,
                  onRemovePhoto = onRemovePhoto,
                )
                PostMode.VIDEO -> VideoComposerCanvas(
                  video = state.video,
                  isEditing = state.editingPostId != null,
                  trimStartMs = state.videoTrimStartMs,
                  trimEndMs = state.videoTrimEndMs,
                  muteOriginalAudio = state.muteOriginalAudio,
                  onLaunchPicker = launchVideoPicker,
                  onClearVideo = onClearVideo,
                  onTrimChanged = onTrimChanged,
                  onSelectThumbnailFrame = onSelectThumbnailFrame,
                  onToggleMuteOriginalAudio = onToggleMuteOriginalAudio,
                )
              }
            }

            Column(
              modifier = Modifier
                .weight(0.95f)
                .verticalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
              if (!state.errorMessage.isNullOrBlank()) {
                ErrorBannerChip(state.errorMessage)
              }
              if (state.mode == PostMode.TEXT) {
                ThemeSwatchesRow(
                  selectedThemeId = state.selectedThemeId,
                  onSelectTheme = onSelectTheme,
                )
                FontPillsFlow(
                  selectedFont = state.selectedFont,
                  onSelectFont = onSelectFont,
                )
              } else {
                CaptionInputCard(
                  caption = state.caption,
                  onCaptionChanged = onCaptionChanged,
                )
              }
              PostMetadataRowsCard(
                selectedSong = state.selectedSong,
                isPlayingSongPreview = activePreviewTrack,
                taggedCount = state.taggedPeople.size,
                taggedPreviewNames = taggedSummary,
                selectedHashtags = state.selectedHashtags,
                onSongClick = onOpenSongSheet,
                onToggleSongPreview = { state.selectedSong?.let(onToggleSongPreview) },
                onClearSong = onClearSong,
                onTagPeopleClick = onOpenTagPeopleSheet,
                onHashtagsClick = onOpenHashtagsSheet,
              )
            }
          }
        } else {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            if (!state.errorMessage.isNullOrBlank()) {
              ErrorBannerChip(state.errorMessage)
            }

            when (state.mode) {
              PostMode.TEXT -> {
                TextComposerCard(
                  text = state.textContent,
                  theme = activeTheme,
                  fontOption = state.selectedFont,
                  onTextChanged = onTextChanged,
                )
                ThemeSwatchesRow(
                  selectedThemeId = state.selectedThemeId,
                  onSelectTheme = onSelectTheme,
                )
                FontPillsFlow(
                  selectedFont = state.selectedFont,
                  onSelectFont = onSelectFont,
                )
              }
              PostMode.PHOTOS -> {
                PhotosComposerCanvas(
                  photos = state.photos,
                  onLaunchPicker = launchPhotoPicker,
                  onRemovePhoto = onRemovePhoto,
                )
                CaptionInputCard(
                  caption = state.caption,
                  onCaptionChanged = onCaptionChanged,
                )
              }
              PostMode.VIDEO -> {
                VideoComposerCanvas(
                  video = state.video,
                  isEditing = state.editingPostId != null,
                  trimStartMs = state.videoTrimStartMs,
                  trimEndMs = state.videoTrimEndMs,
                  muteOriginalAudio = state.muteOriginalAudio,
                  onLaunchPicker = launchVideoPicker,
                  onClearVideo = onClearVideo,
                  onTrimChanged = onTrimChanged,
                  onSelectThumbnailFrame = onSelectThumbnailFrame,
                  onToggleMuteOriginalAudio = onToggleMuteOriginalAudio,
                )
                CaptionInputCard(
                  caption = state.caption,
                  onCaptionChanged = onCaptionChanged,
                )
              }
            }

            PostMetadataRowsCard(
              selectedSong = state.selectedSong,
              isPlayingSongPreview = activePreviewTrack,
              taggedCount = state.taggedPeople.size,
              taggedPreviewNames = taggedSummary,
              selectedHashtags = state.selectedHashtags,
              onSongClick = onOpenSongSheet,
              onToggleSongPreview = { state.selectedSong?.let(onToggleSongPreview) },
              onClearSong = onClearSong,
              onTagPeopleClick = onOpenTagPeopleSheet,
              onHashtagsClick = onOpenHashtagsSheet,
            )

            Spacer(modifier = Modifier.height(12.dp))
          }
        }
      }
    }
  }

  // Modal Sheets
  if (state.showTagPeopleSheet) {
    TagPeopleModalSheet(
      query = state.tagPeopleQuery,
      people = state.allDirectoryPeople,
      selectedIds = state.pendingTaggedIds,
      onQueryChanged = onTagPeopleQueryChanged,
      onTogglePerson = onTogglePendingTagPerson,
      onDone = onConfirmTaggedPeople,
      onDismiss = onDismissTagPeopleSheet,
    )
  }

  if (state.showSongSheet) {
    SongSearchModalSheet(
      query = state.songQuery,
      tracks = state.songResults,
      isLoading = state.isSearchingSongs,
      playingTrackId = state.playingPreviewTrackId,
      onQueryChanged = onSongQueryChanged,
      onTogglePreview = onToggleSongPreview,
      onSelectTrack = onSelectSong,
      onDismiss = onDismissSongSheet,
    )
  }

  if (state.showHashtagsSheet) {
    HashtagsModalSheet(
      query = state.hashtagQuery,
      items = state.hashtagResults,
      selectedTags = state.selectedHashtags,
      isLoading = state.isSearchingHashtags,
      onQueryChanged = onHashtagQueryChanged,
      onToggleTag = onToggleHashtag,
      onDone = onDismissHashtagsSheet,
    )
  }
}

@Composable
private fun ErrorBannerChip(message: String) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.errorContainer,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onErrorContainer,
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
    )
  }
}

@Composable
private fun TextComposerCard(
  text: String,
  theme: PostBackgroundTheme,
  fontOption: PostFontOption,
  onTextChanged: (String) -> Unit,
) {
  val ink = if (theme.isLightInk) Color.White else Color(0xFF1C1F26)
  val cardStyle = postCardTextStyle(fontOption, ink).copy(textAlign = TextAlign.Center)

  PostThemeCanvasBox(
    theme = theme,
    modifier = Modifier
      .fillMaxWidth()
      .height(320.dp)
      .clip(RoundedCornerShape(22.dp)),
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(24.dp),
      contentAlignment = Alignment.Center,
    ) {
      if (text.isEmpty()) {
        Text(
          text = stringResource(R.string.post_text_placeholder),
          style = cardStyle.copy(color = ink.copy(alpha = 0.72f)),
          textAlign = TextAlign.Center,
        )
      }
      BasicTextField(
        value = text,
        onValueChange = onTextChanged,
        textStyle = cardStyle,
        cursorBrush = SolidColor(ink),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("post_text_input"),
      )
    }

    Text(
      text = "${text.length}/${TelefamPostRules.MAX_TEXT_POST_CHARS}",
      style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
      color = ink.copy(alpha = 0.8f),
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(end = 16.dp, bottom = 14.dp),
    )
  }
}

@Composable
private fun ThemeSwatchesRow(
  selectedThemeId: String,
  onSelectTheme: (String) -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    TelefamPostRules.backgroundThemes.forEach { theme ->
      val selected = theme.id == selectedThemeId
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .border(
            width = if (selected) 2.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outlineVariant,
            shape = CircleShape,
          )
          .padding(if (selected) 3.dp else 0.dp)
          .clip(CircleShape)
          .clickable { onSelectTheme(theme.id) }
          .testTag("post_theme_${theme.id}"),
      ) {
        PostThemeCanvasBox(theme = theme, modifier = Modifier.fillMaxSize()) {
          if (selected) {
            Icon(
              imageVector = Icons.Filled.Check,
              contentDescription = theme.label,
              tint = if (theme.isLightInk) Color.White else Color(0xFF1C1F26),
              modifier = Modifier.size(18.dp).align(Alignment.Center),
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FontPillsFlow(
  selectedFont: PostFontOption,
  onSelectFont: (PostFontOption) -> Unit,
) {
  val isDark = LocalTelefamDarkTheme.current
  val selectedBg = if (isDark) TelefamDarkBlush else TelefamBlushWave

  FlowRow(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    PostFontOption.entries.forEach { option ->
      val selected = option == selectedFont
      Surface(
        onClick = { onSelectFont(option) },
        shape = RoundedCornerShape(18.dp),
        color = if (selected) selectedBg else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
          width = 1.dp,
          color = if (selected) TelefamCrimson else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier.testTag("post_font_${option.id}"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = if (option == PostFontOption.BOLD) "AA" else "Aa",
            style = TextStyle(
              fontFamily = postFontFamily(option),
              fontWeight = FontWeight.Bold,
              fontSize = 13.5.sp,
            ),
            color = if (selected) TelefamCrimson else MaterialTheme.colorScheme.onSurface,
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = option.label,
            style = MaterialTheme.typography.bodyMedium.copy(
              fontSize = 13.sp,
              fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = if (selected) TelefamCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun PhotosComposerCanvas(
  photos: List<ProcessedPostPhoto>,
  onLaunchPicker: () -> Unit,
  onRemovePhoto: (String) -> Unit,
) {
  if (photos.isEmpty()) {
    DashedMediaUploadCard(
      icon = Icons.Outlined.AddPhotoAlternate,
      title = stringResource(R.string.post_add_photos),
      subtitle = stringResource(R.string.post_up_to_10),
      onClick = onLaunchPicker,
      testTag = "post_add_photos_box",
    )
    return
  }

  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "${photos.size}/${TelefamPostRules.MAX_PHOTOS_COUNT}",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.weight(1f))
        if (photos.size < TelefamPostRules.MAX_PHOTOS_COUNT) {
          Surface(
            onClick = onLaunchPicker,
            shape = RoundedCornerShape(16.dp),
            color = TelefamCrimson,
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(stringResource(R.string.post_add), color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(photos, key = { it.localId }) { photo ->
          val bmp = remember(photo.localFilePath) {
            BitmapFactory.decodeFile(photo.localFilePath)?.asImageBitmap()
          }
          Box(
            modifier = Modifier
              .size(148.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant),
          ) {
            if (bmp != null) {
              Image(
                bitmap = bmp,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
              )
            }
            Box(
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable { onRemovePhoto(photo.localId) },
              contentAlignment = Alignment.Center,
            ) {
              Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun VideoComposerCanvas(
  video: PreparedPostVideo?,
  isEditing: Boolean = false,
  trimStartMs: Long,
  trimEndMs: Long,
  muteOriginalAudio: Boolean,
  onLaunchPicker: () -> Unit,
  onClearVideo: () -> Unit,
  onTrimChanged: (Long, Long) -> Unit,
  onSelectThumbnailFrame: (Int) -> Unit,
  onToggleMuteOriginalAudio: () -> Unit,
) {
  if (video == null) {
    if (isEditing) {
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleMuteOriginalAudio)
            .padding(horizontal = 14.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = if (muteOriginalAudio) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = null,
            tint = if (muteOriginalAudio) TelefamCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = stringResource(R.string.post_mute_audio),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
          )
          Switch(
            checked = muteOriginalAudio,
            onCheckedChange = { onToggleMuteOriginalAudio() },
            colors = SwitchDefaults.colors(
              checkedThumbColor = TelefamPureWhite,
              checkedTrackColor = TelefamCrimson,
            ),
            modifier = Modifier.testTag("video_mute_audio_switch"),
          )
        }
      }
      return
    }
    DashedMediaUploadCard(
      icon = Icons.Filled.Movie,
      title = stringResource(R.string.post_add_video),
      subtitle = stringResource(R.string.post_up_to_200mb),
      onClick = onLaunchPicker,
      testTag = "post_add_video_box",
    )
    return
  }

  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      // Selected Thumbnail Hero Preview
      val thumbBmp = remember(video.selectedThumbnailPath) {
        if (video.selectedThumbnailPath.isNotBlank() && File(video.selectedThumbnailPath).exists()) {
          BitmapFactory.decodeFile(video.selectedThumbnailPath)?.asImageBitmap()
        } else null
      }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(210.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(Color(0xFF10131A)),
      ) {
        if (thumbBmp != null) {
          Image(
            bitmap = thumbBmp,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          Icon(
            imageVector = Icons.Filled.Movie,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(44.dp).align(Alignment.Center),
          )
        }

        // Top badges: trimmed clip duration + close/replace
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color.Black.copy(alpha = 0.65f),
          modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
        ) {
          val span = (trimEndMs - trimStartMs).coerceAtLeast(0L)
          Text(
            text = TelefamPostRules.formatMediaDuration(span),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }

        Box(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(10.dp)
            .size(30.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(onClick = onClearVideo),
          contentAlignment = Alignment.Center,
        ) {
          Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
      }

      // Extracted Timeline Frames — tap any frame to set thumbnail
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        items(video.frames, key = { it.index }) { frame ->
          val selectedThumb = frame.index == video.selectedThumbnailIndex
          val fBmp = remember(frame.previewPath) {
            if (frame.previewPath.isNotBlank() && File(frame.previewPath).exists()) {
              BitmapFactory.decodeFile(frame.previewPath)?.asImageBitmap()
            } else null
          }
          Box(
            modifier = Modifier
              .size(width = 56.dp, height = 68.dp)
              .clip(RoundedCornerShape(8.dp))
              .border(
                width = if (selectedThumb) 2.5.dp else 1.dp,
                color = if (selectedThumb) TelefamCrimson else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp),
              )
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .clickable { onSelectThumbnailFrame(frame.index) }
              .testTag("video_frame_${frame.index}"),
          ) {
            if (fBmp != null) {
              Image(
                bitmap = fBmp,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
              )
            }
          }
        }
      }

      // Trim RangeSlider
      val maxDur = video.durationMs.coerceAtLeast(1000L).toFloat()
      Column {
        RangeSlider(
          value = trimStartMs.toFloat().coerceIn(0f, maxDur)..trimEndMs.toFloat().coerceIn(0f, maxDur),
          onValueChange = { range ->
            onTrimChanged(range.start.toLong(), range.endInclusive.toLong())
          },
          valueRange = 0f..maxDur,
          colors = SliderDefaults.colors(
            thumbColor = TelefamCrimson,
            activeTrackColor = TelefamCrimson,
          ),
          modifier = Modifier.fillMaxWidth().testTag("video_trim_slider"),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(
            text = TelefamPostRules.formatMediaDuration(trimStartMs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = TelefamPostRules.formatMediaDuration(trimEndMs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Mute original audio toggle
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .clickable(onClick = onToggleMuteOriginalAudio)
          .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = if (muteOriginalAudio) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
          contentDescription = null,
          tint = if (muteOriginalAudio) TelefamCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = stringResource(R.string.post_mute_audio),
          style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.weight(1f),
        )
        Switch(
          checked = muteOriginalAudio,
          onCheckedChange = { onToggleMuteOriginalAudio() },
          colors = SwitchDefaults.colors(
            checkedThumbColor = TelefamPureWhite,
            checkedTrackColor = TelefamCrimson,
          ),
          modifier = Modifier.testTag("video_mute_audio_switch"),
        )
      }
    }
  }
}

@Composable
private fun CaptionInputCard(
  caption: String,
  onCaptionChanged: (String) -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 112.dp),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
    ) {
      if (caption.isEmpty()) {
        Text(
          text = stringResource(R.string.post_caption_placeholder),
          style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      BasicTextField(
        value = caption,
        onValueChange = onCaptionChanged,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
          fontSize = 15.sp,
          color = MaterialTheme.colorScheme.onSurface,
        ),
        cursorBrush = SolidColor(TelefamCrimson),
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 20.dp)
          .testTag("post_caption_input"),
      )
      Text(
        text = "${caption.length}/${TelefamPostRules.MAX_CAPTION_CHARS}",
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.align(Alignment.BottomEnd),
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagPeopleModalSheet(
  query: String,
  people: List<ContactUiItem>,
  selectedIds: Set<String>,
  onQueryChanged: (String) -> Unit,
  onTogglePerson: (String) -> Unit,
  onDone: () -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val isDark = LocalTelefamDarkTheme.current
  val blush = if (isDark) TelefamDarkBlush else TelefamBlushWave

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 18.dp, vertical = 6.dp),
    ) {
      Text(
        text = stringResource(R.string.post_tag_people),
        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(12.dp))

      // Crimson-outlined search pill matching Ref Image 2
      CrimsonSearchBar(
        query = query,
        placeholder = stringResource(R.string.post_search_people),
        onQueryChanged = onQueryChanged,
        testTag = "tag_people_search",
      )

      Spacer(modifier = Modifier.height(12.dp))

      val cleanQ = query.trim().lowercase()
      val filtered = remember(people, cleanQ) {
        if (cleanQ.isEmpty()) people
        else people.filter {
          it.fullName.lowercase().contains(cleanQ) || it.username.lowercase().contains(cleanQ)
        }
      }

      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 240.dp, max = 380.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(filtered, key = { it.userId }) { person ->
          val checked = person.userId in selectedIds
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .clickable { onTogglePerson(person.userId) }
              .padding(vertical = 6.dp, horizontal = 4.dp)
              .testTag("tag_person_${person.userId}"),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            val bmp = remember(person.avatarLocalPath) {
              if (!person.avatarLocalPath.isNullOrBlank() && File(person.avatarLocalPath).exists()) {
                BitmapFactory.decodeFile(person.avatarLocalPath)?.asImageBitmap()
              } else null
            }
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(blush),
              contentAlignment = Alignment.Center,
            ) {
              Text(person.initials, color = TelefamCrimson, fontWeight = FontWeight.Bold, fontSize = 14.sp)
              if (bmp != null) {
                Image(bitmap = bmp, contentDescription = person.fullName, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
              } else if (!person.avatarLocalPath.isNullOrBlank()) {
                AsyncImage(
                  model = person.avatarLocalPath,
                  contentDescription = person.fullName,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize(),
                )
              }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = person.fullName,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "@${person.username}",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
              )
            }
            Box(
              modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (checked) TelefamCrimson else Color.Transparent)
                .border(
                  width = 1.5.dp,
                  color = if (checked) TelefamCrimson else MaterialTheme.colorScheme.outline,
                  shape = CircleShape,
                ),
              contentAlignment = Alignment.Center,
            ) {
              if (checked) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
      Button(
        onClick = onDone,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = TelefamCrimson, contentColor = Color.White),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("tag_people_done_button"),
      ) {
        Text(stringResource(R.string.post_done), fontWeight = FontWeight.Bold, fontSize = 15.sp)
      }
      Spacer(modifier = Modifier.height(10.dp))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongSearchModalSheet(
  query: String,
  tracks: List<telefam.company.core.PostSongTrack>,
  isLoading: Boolean,
  playingTrackId: String?,
  onQueryChanged: (String) -> Unit,
  onTogglePreview: (telefam.company.core.PostSongTrack) -> Unit,
  onSelectTrack: (telefam.company.core.PostSongTrack) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val isDark = LocalTelefamDarkTheme.current
  val blush = if (isDark) TelefamDarkBlush else TelefamBlushWave

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 18.dp, vertical = 6.dp),
    ) {
      Text(
        text = stringResource(R.string.post_song),
        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(12.dp))

      CrimsonSearchBar(
        query = query,
        placeholder = stringResource(R.string.post_search_songs),
        onQueryChanged = onQueryChanged,
        testTag = "song_search_input",
      )

      Spacer(modifier = Modifier.height(12.dp))

      if (isLoading) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = TelefamCrimson, modifier = Modifier.size(28.dp))
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 240.dp, max = 400.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(tracks, key = { it.trackId }) { track ->
            val playing = track.trackId == playingTrackId
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onSelectTrack(track) }
                .padding(vertical = 8.dp, horizontal = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box(
                modifier = Modifier
                  .size(44.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(blush),
                contentAlignment = Alignment.Center,
              ) {
                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = TelefamCrimson, modifier = Modifier.size(22.dp))
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = track.title,
                  style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.5.sp, fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = "${track.artist} · ${TelefamPostRules.formatMediaDuration(track.durationMs)}",
                  style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              if (track.previewUrl.isNotBlank()) {
                IconButton(
                  onClick = { onTogglePreview(track) },
                  modifier = Modifier.size(48.dp),
                ) {
                  Icon(
                    imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = TelefamCrimson,
                  )
                }
              }
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HashtagsModalSheet(
  query: String,
  items: List<telefam.company.core.HashtagItem>,
  selectedTags: List<String>,
  isLoading: Boolean,
  onQueryChanged: (String) -> Unit,
  onToggleTag: (String) -> Unit,
  onDone: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val isDark = LocalTelefamDarkTheme.current
  val blush = if (isDark) TelefamDarkBlush else TelefamBlushWave

  ModalBottomSheet(
    onDismissRequest = onDone,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 18.dp, vertical = 6.dp),
    ) {
      Text(
        text = stringResource(R.string.post_hashtags),
        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(12.dp))

      CrimsonSearchBar(
        query = query,
        placeholder = stringResource(R.string.post_search_hashtags),
        onQueryChanged = onQueryChanged,
        testTag = "hashtags_search_input",
      )

      Spacer(modifier = Modifier.height(12.dp))

      if (isLoading && items.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = TelefamCrimson, modifier = Modifier.size(26.dp))
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp, max = 360.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(items, key = { it.tag }) { tagItem ->
            val selected = tagItem.tag in selectedTags
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onToggleTag(tagItem.tag) }
                .padding(vertical = 8.dp, horizontal = 4.dp)
                .testTag("hashtag_item_${tagItem.tag}"),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box(
                modifier = Modifier
                  .size(38.dp)
                  .clip(CircleShape)
                  .background(blush),
                contentAlignment = Alignment.Center,
              ) {
                Icon(Icons.Filled.Tag, contentDescription = null, tint = TelefamCrimson, modifier = Modifier.size(18.dp))
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "#${tagItem.tag}",
                  style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = "${tagItem.postCount} posts",
                  style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Box(
                modifier = Modifier
                  .size(22.dp)
                  .clip(CircleShape)
                  .background(if (selected) TelefamCrimson else Color.Transparent)
                  .border(
                    width = 1.5.dp,
                    color = if (selected) TelefamCrimson else MaterialTheme.colorScheme.outline,
                    shape = CircleShape,
                  ),
                contentAlignment = Alignment.Center,
              ) {
                if (selected) {
                  Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
      Button(
        onClick = onDone,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = TelefamCrimson, contentColor = Color.White),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("hashtags_done_button"),
      ) {
        Text(stringResource(R.string.post_done), fontWeight = FontWeight.Bold, fontSize = 15.sp)
      }
      Spacer(modifier = Modifier.height(10.dp))
    }
  }
}

@Composable
private fun CrimsonSearchBar(
  query: String,
  placeholder: String,
  onQueryChanged: (String) -> Unit,
  testTag: String,
) {
  Surface(
    shape = RoundedCornerShape(22.dp),
    color = MaterialTheme.colorScheme.background,
    border = BorderStroke(1.5.dp, TelefamCrimson),
    modifier = Modifier
      .fillMaxWidth()
      .height(44.dp),
  ) {
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Filled.Search,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
        if (query.isEmpty()) {
          Text(
            text = placeholder,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        BasicTextField(
          value = query,
          onValueChange = onQueryChanged,
          singleLine = true,
          textStyle = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
          ),
          cursorBrush = SolidColor(TelefamCrimson),
          modifier = Modifier.fillMaxWidth().testTag(testTag),
        )
      }
    }
  }
}
