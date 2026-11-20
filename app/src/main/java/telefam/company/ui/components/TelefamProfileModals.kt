package telefam.company.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.telefam.company.app.R
import telefam.company.core.ProfileBackgroundItem
import telefam.company.core.TelefamProfileSanitizer
import telefam.company.data.model.CountryCatalog
import telefam.company.data.model.CountryDialOption
import telefam.company.media.MediaUploadProcessor
import telefam.company.ui.screens.CountrySearchDialog
import telefam.company.ui.theme.LocalTelefamDarkTheme
import telefam.company.ui.theme.TelefamCrimson
import telefam.company.ui.theme.TelefamCrimsonBright
import telefam.company.ui.theme.TelefamPureWhite
import telefam.company.ui.theme.ThemeMode
import telefam.company.ui.viewmodel.ProfileFormState
import telefam.company.ui.viewmodel.UserProfileScreenState
import java.io.File
import java.util.Calendar
import java.util.TimeZone

private const val TWO_WEEKS_MILLIS = 14L * 24L * 60L * 60L * 1000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerProfileSettingsSheet(
  themeMode: ThemeMode,
  username: String,
  email: String,
  onSignOut: () -> Unit,
  onToggleTheme: () -> Unit,
  onDismiss: () -> Unit,
) {
  val isDark = LocalTelefamDarkTheme.current
  val accentCrimson = if (isDark) TelefamCrimsonBright else TelefamCrimson
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 20.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(R.string.profile_settings_title),
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface,
        )
        IconButton(onClick = onDismiss) {
          Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = stringResource(R.string.cd_back),
          )
        }
      }

      // 1. LOGOUT FIRST IN SETTINGS (strictly placed at the top)
      Button(
        onClick = {
          onDismiss()
          onSignOut()
        },
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = accentCrimson,
          contentColor = TelefamPureWhite,
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("settings_logout_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Logout,
          contentDescription = stringResource(R.string.profile_settings_logout),
          modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = stringResource(R.string.profile_settings_logout),
          style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
        )
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      // 2. Theme Appearance Toggle Row
      Surface(
        onClick = onToggleTheme,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("settings_theme_button"),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = stringResource(R.string.profile_settings_theme),
              style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = when (themeMode) {
                ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                ThemeMode.DARK -> stringResource(R.string.theme_dark)
              },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Surface(
            color = accentCrimson.copy(alpha = 0.14f),
            shape = RoundedCornerShape(10.dp),
          ) {
            Text(
              text = stringResource(R.string.cd_theme_toggle),
              color = accentCrimson,
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
          }
        }
      }

      // 3. Account Details Summary
      if (username.isNotBlank() || email.isNotBlank()) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = stringResource(R.string.profile_settings_account),
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (username.isNotBlank()) {
              Text(
                text = "@$username",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
            if (email.isNotBlank()) {
              Text(
                text = email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBackgroundsBottomSheet(
  backgrounds: List<ProfileBackgroundItem>,
  isUploading: Boolean,
  errorMessage: String?,
  onAddUris: (List<Uri>) -> Unit,
  onDeleteBackground: (String) -> Unit,
  onReorderBackgrounds: (List<String>) -> Unit,
  onDismiss: () -> Unit,
) {
  val isDark = LocalTelefamDarkTheme.current
  val accentCrimson = if (isDark) TelefamCrimsonBright else TelefamCrimson
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val remainingSlots = (5 - backgrounds.size).coerceAtLeast(0)

  val singlePicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) onAddUris(listOf(uri))
  }

  val multiPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia(5),
  ) { uris ->
    if (uris.isNotEmpty()) onAddUris(uris.take(remainingSlots))
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 20.dp, vertical = 8.dp)
        .heightIn(max = 560.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "${stringResource(R.string.profile_backgrounds_title)} (${backgrounds.size}/5)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = stringResource(R.string.profile_backgrounds_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(onClick = onDismiss) {
          Icon(imageVector = Icons.Filled.Close, contentDescription = stringResource(R.string.cd_back))
        }
      }

      if (isUploading) {
        LinearProgressIndicator(
          color = accentCrimson,
          modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
        )
      }

      if (!errorMessage.isNullOrBlank()) {
        Text(
          text = errorMessage,
          color = accentCrimson,
          style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
        )
      }

      Button(
        onClick = {
          val req = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
          if (remainingSlots <= 1) {
            singlePicker.launch(req)
          } else {
            multiPicker.launch(req)
          }
        },
        enabled = !isUploading && remainingSlots > 0,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = accentCrimson,
          contentColor = TelefamPureWhite,
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(46.dp)
          .testTag("backgrounds_add_button"),
      ) {
        Icon(imageVector = Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "${stringResource(R.string.profile_backgrounds_add)} ($remainingSlots left)",
          fontWeight = FontWeight.SemiBold,
        )
      }

      if (backgrounds.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = stringResource(R.string.profile_backgrounds_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = false),
          verticalArrangement = Arrangement.spacedBy(10.dp),
          contentPadding = PaddingValues(bottom = 14.dp),
        ) {
          itemsIndexed(backgrounds, key = { _, item -> item.mediaId }) { idx, bg ->
            val previewModel = bg.localFilePath.takeIf { it.isNotBlank() && File(it).exists() } ?: bg.url
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Box(
                  modifier = Modifier
                    .size(width = 86.dp, height = 58.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF141820)),
                ) {
                  val localBmp = remember(previewModel) {
                    if (previewModel.startsWith("/") && File(previewModel).exists()) {
                      BitmapFactory.decodeFile(previewModel)?.asImageBitmap()
                    } else null
                  }
                  if (localBmp != null) {
                    Image(
                      bitmap = localBmp,
                      contentDescription = null,
                      contentScale = ContentScale.Crop,
                      modifier = Modifier.fillMaxSize(),
                    )
                  } else {
                    AsyncImage(
                      model = previewModel,
                      contentDescription = null,
                      contentScale = ContentScale.Crop,
                      modifier = Modifier.fillMaxSize(),
                    )
                  }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text(
                    text = "Slide #${idx + 1}",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  if (bg.width > 0 && bg.height > 0) {
                    Text(
                      text = "${bg.width}×${bg.height} · ${(bg.byteSize / 1024).coerceAtLeast(1)} KB",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  if (bg.sha256Fingerprint.isNotBlank() || bg.dHashFingerprint.isNotBlank()) {
                    Text(
                      text = buildString {
                        if (bg.sha256Fingerprint.isNotBlank()) append("SHA:${bg.sha256Fingerprint.take(8)} ")
                        if (bg.dHashFingerprint.isNotBlank()) append("dH:${bg.dHashFingerprint.take(8)}")
                      }.trim(),
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                      ),
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }

                if (idx > 0) {
                  IconButton(
                    enabled = !isUploading,
                    onClick = {
                      val ids = backgrounds.map { it.mediaId }.toMutableList()
                      val tmp = ids[idx - 1]
                      ids[idx - 1] = ids[idx]
                      ids[idx] = tmp
                      onReorderBackgrounds(ids)
                    },
                  ) {
                    Icon(Icons.Filled.ArrowUpward, contentDescription = "Move up", modifier = Modifier.size(18.dp))
                  }
                }
                if (idx < backgrounds.lastIndex) {
                  IconButton(
                    enabled = !isUploading,
                    onClick = {
                      val ids = backgrounds.map { it.mediaId }.toMutableList()
                      val tmp = ids[idx + 1]
                      ids[idx + 1] = ids[idx]
                      ids[idx] = tmp
                      onReorderBackgrounds(ids)
                    },
                  ) {
                    Icon(Icons.Filled.ArrowDownward, contentDescription = "Move down", modifier = Modifier.size(18.dp))
                  }
                }
                IconButton(
                  enabled = !isUploading,
                  onClick = { onDeleteBackground(bg.mediaId) },
                  modifier = Modifier.testTag("background_delete_${bg.mediaId}"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete background",
                    tint = accentCrimson,
                    modifier = Modifier.size(20.dp),
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileModalSheet(
  profile: UserProfileScreenState,
  formState: ProfileFormState,
  cropBitmap: Bitmap?,
  isSaving: Boolean,
  errorMessage: String?,
  onPhotoPicked: (Bitmap) -> Unit,
  onDismissCrop: () -> Unit,
  onConfirmCrop: (Bitmap, Float, Float, Float) -> Unit,
  onSaveProfile: (
    fullName: String,
    username: String,
    country: CountryDialOption,
    phoneDigits: String,
    gender: String,
    dobYear: Int,
    dobMonthZeroBased: Int,
    dobDay: Int,
    location: String,
    bio: String,
  ) -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val isDark = LocalTelefamDarkTheme.current
  val accentCrimson = if (isDark) TelefamCrimsonBright else TelefamCrimson

  val initialCountry = remember(profile.countryIso, formState.selectedCountry) {
    CountryCatalog.allCountries.firstOrNull { it.iso.equals(profile.countryIso, ignoreCase = true) }
      ?: formState.selectedCountry
  }
  val initialPhoneDigits = remember(profile.phoneNumber, initialCountry, formState.phoneDigits) {
    val raw = profile.phoneNumber.ifBlank { formState.phoneDigits }
    val cleanDial = initialCountry.dialCode.filter { it.isDigit() }
    val digits = raw.filter { it.isDigit() }
    if (cleanDial.isNotBlank() && digits.startsWith(cleanDial) && digits.length > cleanDial.length) {
      digits.removePrefix(cleanDial)
    } else {
      digits
    }
  }
  val initialDobTriple = remember(profile.dateOfBirth, formState.dobYear) {
    val parts = profile.dateOfBirth.split("-")
    if (parts.size == 3) {
      val y = parts[0].toIntOrNull()
      val m = parts[1].toIntOrNull()?.minus(1)
      val d = parts[2].toIntOrNull()
      if (y != null && m != null && d != null && m in 0..11 && d in 1..31) {
        Triple(y, m, d)
      } else null
    } else if (formState.dobYear != null && formState.dobMonthZeroBased != null && formState.dobDay != null) {
      Triple(formState.dobYear, formState.dobMonthZeroBased, formState.dobDay)
    } else {
      Triple(2000, 0, 1)
    }
  } ?: Triple(2000, 0, 1)

  var fullName by remember(profile.userId, profile.fullName) {
    mutableStateOf(profile.fullName.ifBlank { formState.fullName })
  }
  var username by remember(profile.userId, profile.username) {
    mutableStateOf(profile.username.ifBlank { formState.username })
  }
  var selectedCountry by remember(profile.userId, initialCountry) { mutableStateOf(initialCountry) }
  var phoneDigits by remember(profile.userId, initialPhoneDigits) { mutableStateOf(initialPhoneDigits) }
  var gender by remember(profile.userId, profile.gender) {
    mutableStateOf(profile.gender.ifBlank { formState.gender.ifBlank { "Prefer not to say" } })
  }
  var dobYear by remember(profile.userId, initialDobTriple) { mutableStateOf(initialDobTriple.first) }
  var dobMonthZeroBased by remember(profile.userId, initialDobTriple) { mutableStateOf(initialDobTriple.second) }
  var dobDay by remember(profile.userId, initialDobTriple) { mutableStateOf(initialDobTriple.third) }
  var location by remember(profile.userId, profile.location) {
    mutableStateOf(profile.location.ifBlank { formState.location })
  }
  var bio by remember(profile.userId, profile.bio) {
    mutableStateOf(profile.bio.ifBlank { formState.bio })
  }

  var showCountryDialog by remember { mutableStateOf(false) }
  var showGenderMenu by remember { mutableStateOf(false) }
  var showDobPicker by remember { mutableStateOf(false) }
  var localValidationError by remember { mutableStateOf<String?>(null) }

  val nowMs = System.currentTimeMillis()
  val nextAllowedMs = remember(profile.usernameChangedAtMs, profile.nextUsernameChangeMs) {
    when {
      profile.nextUsernameChangeMs > 0L -> profile.nextUsernameChangeMs
      profile.usernameChangedAtMs > 0L -> profile.usernameChangedAtMs + TWO_WEEKS_MILLIS
      else -> 0L
    }
  }
  val isUsernameLocked = nextAllowedMs > nowMs
  val remainingLockLabel = remember(nextAllowedMs, isUsernameLocked) {
    if (!isUsernameLocked) "" else {
      val remMs = (nextAllowedMs - nowMs).coerceAtLeast(0L)
      val days = remMs / (24L * 60L * 60L * 1000L)
      val hours = (remMs % (24L * 60L * 60L * 1000L)) / (60L * 60L * 1000L)
      if (days > 0) "Locked for ${days}d ${hours}h (once per 2 weeks)" else "Locked for ${hours.coerceAtLeast(1)}h (once per 2 weeks)"
    }
  }

  val avatarPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) {
      MediaUploadProcessor.loadSafePreviewBitmap(context, uri)?.let(onPhotoPicked)
    }
  }

  cropBitmap?.let { raw ->
    TelefamPhotoCropperDialog(
      bitmap = raw,
      onCancel = onDismissCrop,
      onConfirmCrop = { cx, cy, scale -> onConfirmCrop(raw, cx, cy, scale) },
    )
  }

  if (showCountryDialog) {
    CountrySearchDialog(
      onDismiss = { showCountryDialog = false },
      onSelect = {
        selectedCountry = it
        showCountryDialog = false
      },
    )
  }

  if (showDobPicker) {
    val calInit = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
      set(dobYear, dobMonthZeroBased, dobDay, 12, 0, 0)
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = calInit.timeInMillis)
    DatePickerDialog(
      onDismissRequest = { showDobPicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let { millis ->
              val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }
              val y = cal.get(Calendar.YEAR)
              val m = cal.get(Calendar.MONTH)
              val d = cal.get(Calendar.DAY_OF_MONTH)
              if (!TelefamProfileSanitizer.isAtLeast13YearsOld(y, m, d)) {
                localValidationError = context.getString(R.string.profile_age_error)
              } else {
                dobYear = y
                dobMonthZeroBased = m
                dobDay = d
                localValidationError = null
              }
            }
            showDobPicker = false
          },
        ) {
          Text("OK", color = accentCrimson)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDobPicker = false }) {
          Text(stringResource(R.string.profile_crop_cancel))
        }
      },
    ) {
      DatePicker(state = datePickerState)
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      modifier = Modifier
        .fillMaxSize()
        .testTag("edit_profile_modal"),
      color = MaterialTheme.colorScheme.background,
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .navigationBarsPadding()
          .imePadding(),
      ) {
        // Header bar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          IconButton(onClick = onDismiss, modifier = Modifier.testTag("edit_profile_close")) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.profile_crop_cancel))
          }
          Text(
            text = stringResource(R.string.profile_edit_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
          )
          TextButton(
            enabled = !isSaving,
            onClick = {
              localValidationError = null
              if (fullName.trim().isBlank() || location.trim().isBlank()) {
                localValidationError = context.getString(R.string.profile_required_error)
                return@TextButton
              }
              if (!TelefamProfileSanitizer.isValidUsername(username)) {
                localValidationError = context.getString(R.string.profile_username_invalid)
                return@TextButton
              }
              if (!TelefamProfileSanitizer.isAtLeast13YearsOld(dobYear, dobMonthZeroBased, dobDay)) {
                localValidationError = context.getString(R.string.profile_age_error)
                return@TextButton
              }
              onSaveProfile(
                fullName,
                username,
                selectedCountry,
                phoneDigits,
                gender,
                dobYear,
                dobMonthZeroBased,
                dobDay,
                location,
                bio,
              )
            },
            modifier = Modifier.testTag("edit_profile_save_top"),
          ) {
            Text(
              text = stringResource(R.string.profile_edit_save),
              color = accentCrimson,
              fontWeight = FontWeight.Bold,
            )
          }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          // Avatar / DP Picker
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Box(
              modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(2.dp, accentCrimson, CircleShape)
                .clickable {
                  avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                .testTag("edit_profile_avatar_button"),
              contentAlignment = Alignment.Center,
            ) {
              val av = profile.avatarUrl
              val bmp = remember(av) {
                if (!av.isNullOrBlank() && av.startsWith("/") && File(av).exists()) {
                  BitmapFactory.decodeFile(av)?.asImageBitmap()
                } else null
              }
              if (bmp != null) {
                Image(
                  bitmap = bmp,
                  contentDescription = stringResource(R.string.cd_profile_avatar),
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize(),
                )
              } else if (!av.isNullOrBlank()) {
                AsyncImage(
                  model = av,
                  contentDescription = stringResource(R.string.cd_profile_avatar),
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize(),
                )
              } else {
                Icon(Icons.Filled.Person, contentDescription = null, tint = accentCrimson, modifier = Modifier.size(36.dp))
              }
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .background(Color.Black.copy(alpha = 0.28f)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Filled.CameraAlt,
                  contentDescription = stringResource(R.string.profile_change_photo),
                  tint = Color.White,
                  modifier = Modifier.size(26.dp),
                )
              }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = stringResource(R.string.profile_change_photo),
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
              color = accentCrimson,
              modifier = Modifier.clickable {
                avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
              },
            )
          }

          val combinedError = localValidationError ?: errorMessage
          if (!combinedError.isNullOrBlank()) {
            Surface(
              color = accentCrimson.copy(alpha = 0.12f),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = combinedError,
                color = accentCrimson,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(12.dp),
              )
            }
          }

          // Full Name
          EditLabel(Icons.Filled.Person, stringResource(R.string.profile_full_name))
          OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it.take(64) },
            singleLine = true,
            placeholder = { Text(stringResource(R.string.profile_full_name_hint)) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("edit_profile_name_input"),
          )

          // Username with 2-week cooldown enforcement
          EditLabel(Icons.Outlined.AlternateEmail, stringResource(R.string.profile_username))
          OutlinedTextField(
            value = username,
            onValueChange = {
              if (!isUsernameLocked) {
                username = TelefamProfileSanitizer.sanitizeUsernameInput(it)
              }
            },
            enabled = !isUsernameLocked,
            singleLine = true,
            leadingIcon = { Text("@", color = accentCrimson, fontWeight = FontWeight.Bold) },
            trailingIcon = if (isUsernameLocked) {
              { Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) }
            } else null,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("edit_profile_username_input"),
          )
          Text(
            text = if (isUsernameLocked) remainingLockLabel else stringResource(R.string.profile_username_cooldown_note),
            style = MaterialTheme.typography.labelSmall,
            color = if (isUsernameLocked) accentCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
          )

          // Country & Phone
          EditLabel(Icons.Filled.Phone, stringResource(R.string.profile_phone))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .clip(RoundedCornerShape(12.dp))
              .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
              .background(MaterialTheme.colorScheme.surface),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              modifier = Modifier
                .clickable { showCountryDialog = true }
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .testTag("edit_profile_country_button"),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(text = selectedCountry.flagEmoji, fontSize = 18.sp)
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "${selectedCountry.iso} ${selectedCountry.dialCode}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
              )
              Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Box(
              modifier = Modifier
                .width(1.dp)
                .height(28.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
            )
            OutlinedTextField(
              value = phoneDigits,
              onValueChange = { phoneDigits = it.filter { c -> c.isDigit() }.take(15) },
              singleLine = true,
              placeholder = { Text(stringResource(R.string.profile_phone_hint)) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
              ),
              modifier = Modifier.weight(1f).testTag("edit_profile_phone_input"),
            )
          }

          // Gender
          EditLabel(Icons.Outlined.Group, stringResource(R.string.profile_gender))
          Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
              onClick = { showGenderMenu = true },
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surface,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
              modifier = Modifier.fillMaxWidth().height(50.dp).testTag("edit_profile_gender_button"),
            ) {
              Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(text = gender, color = MaterialTheme.colorScheme.onSurface)
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
              }
            }
            DropdownMenu(expanded = showGenderMenu, onDismissRequest = { showGenderMenu = false }) {
              listOf("Male", "Female", "Non-binary", "Prefer not to say").forEach { opt ->
                DropdownMenuItem(
                  text = { Text(opt) },
                  onClick = {
                    gender = opt
                    showGenderMenu = false
                  },
                )
              }
            }
          }

          // Date of Birth
          EditLabel(Icons.Filled.CalendarMonth, stringResource(R.string.profile_dob))
          Surface(
            onClick = { showDobPicker = true },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("edit_profile_dob_button"),
          ) {
            Row(
              modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = String.format("%04d-%02d-%02d", dobYear, dobMonthZeroBased + 1, dobDay),
                color = MaterialTheme.colorScheme.onSurface,
              )
              Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
            }
          }

          // Location
          EditLabel(Icons.Filled.LocationOn, stringResource(R.string.profile_location))
          OutlinedTextField(
            value = location,
            onValueChange = { location = it.take(80) },
            singleLine = true,
            placeholder = { Text(stringResource(R.string.profile_location_hint)) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("edit_profile_location_input"),
          )

          // Bio
          EditLabel(Icons.Outlined.ChatBubbleOutline, stringResource(R.string.profile_bio))
          OutlinedTextField(
            value = bio,
            onValueChange = { bio = it.take(150) },
            placeholder = { Text(stringResource(R.string.profile_bio_hint)) },
            maxLines = 4,
            supportingText = { Text("${bio.length}/150") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("edit_profile_bio_input"),
          )

          Spacer(modifier = Modifier.height(6.dp))

          Button(
            enabled = !isSaving,
            onClick = {
              localValidationError = null
              if (fullName.trim().isBlank() || location.trim().isBlank()) {
                localValidationError = context.getString(R.string.profile_required_error)
                return@Button
              }
              if (!TelefamProfileSanitizer.isValidUsername(username)) {
                localValidationError = context.getString(R.string.profile_username_invalid)
                return@Button
              }
              if (!TelefamProfileSanitizer.isAtLeast13YearsOld(dobYear, dobMonthZeroBased, dobDay)) {
                localValidationError = context.getString(R.string.profile_age_error)
                return@Button
              }
              onSaveProfile(
                fullName,
                username,
                selectedCountry,
                phoneDigits,
                gender,
                dobYear,
                dobMonthZeroBased,
                dobDay,
                location,
                bio,
              )
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = accentCrimson,
              contentColor = TelefamPureWhite,
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("edit_profile_save_button"),
          ) {
            if (isSaving) {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), color = TelefamPureWhite, strokeWidth = 2.dp)
            } else {
              Text(
                text = stringResource(R.string.profile_edit_save),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))
        }
      }
    }
  }
}

@Composable
private fun EditLabel(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(icon, contentDescription = null, tint = TelefamCrimson, modifier = Modifier.size(16.dp))
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = text,
      style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
      color = MaterialTheme.colorScheme.onBackground,
    )
  }
}
