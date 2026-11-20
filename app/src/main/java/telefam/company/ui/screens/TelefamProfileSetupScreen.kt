package telefam.company.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.telefam.company.app.R
import telefam.company.data.model.CountryCatalog
import telefam.company.data.model.CountryDialOption
import telefam.company.media.MediaUploadProcessor
import telefam.company.ui.components.CompactThemeToggleChip
import telefam.company.ui.components.TelefamPhotoCropperDialog
import telefam.company.ui.theme.LocalTelefamDarkTheme
import telefam.company.ui.theme.TelefamBlushWave
import telefam.company.ui.theme.TelefamCrimson
import telefam.company.ui.theme.TelefamDarkBlush
import telefam.company.ui.theme.TelefamPureWhite
import telefam.company.ui.theme.TelefamStatusOnline
import telefam.company.ui.theme.ThemeMode
import telefam.company.ui.viewmodel.ProfileFormState
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelefamProfileSetupScreen(
  formState: ProfileFormState,
  themeMode: ThemeMode,
  onToggleTheme: () -> Unit,
  onBackSignOut: () -> Unit,
  onPhotoPickedForCrop: (android.graphics.Bitmap) -> Unit,
  onDismissCrop: () -> Unit,
  onConfirmCrop: (android.graphics.Bitmap, Float, Float, Float) -> Unit,
  onFullNameChanged: (String) -> Unit,
  onUsernameChanged: (String) -> Unit,
  onCountrySelected: (CountryDialOption) -> Unit,
  onPhoneDigitsChanged: (String) -> Unit,
  onGenderSelected: (String) -> Unit,
  onDobSelected: (Int, Int, Int) -> Unit,
  onLocationChanged: (String) -> Unit,
  onBioChanged: (String) -> Unit,
  onContinue: () -> Unit,
) {
  BackHandler { onBackSignOut() }

  val context = LocalContext.current
  var showCountryDialog by remember { mutableStateOf(false) }
  var showGenderMenu by remember { mutableStateOf(false) }
  var showDobPicker by remember { mutableStateOf(false) }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri: Uri? ->
    if (uri != null) {
      MediaUploadProcessor.loadSafePreviewBitmap(context, uri)?.let { bmp ->
        onPhotoPickedForCrop(bmp)
      }
    }
  }

  val openPhotoPicker: () -> Unit = {
    photoPickerLauncher.launch(
      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
    )
  }

  formState.rawBitmapForCrop?.let { rawBmp ->
    TelefamPhotoCropperDialog(
      bitmap = rawBmp,
      onCancel = onDismissCrop,
      onConfirmCrop = { cx, cy, scale -> onConfirmCrop(rawBmp, cx, cy, scale) },
    )
  }

  if (showCountryDialog) {
    CountrySearchDialog(
      onDismiss = { showCountryDialog = false },
      onSelect = {
        onCountrySelected(it)
        showCountryDialog = false
      },
    )
  }

  if (showDobPicker) {
    val datePickerState = rememberDatePickerState()
    DatePickerDialog(
      onDismissRequest = { showDobPicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let { millis ->
              val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = millis
              }
              onDobSelected(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH),
              )
            }
            showDobPicker = false
          },
        ) {
          Text("OK", color = MaterialTheme.colorScheme.primary)
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

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(TelefamCrimson),
  ) {
    // Red Header matching reference image 1
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
      IconButton(
        onClick = onBackSignOut,
        modifier = Modifier
          .align(Alignment.CenterStart)
          .testTag("profile_back_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = stringResource(R.string.cd_back),
          tint = TelefamPureWhite,
        )
      }

      Column(
        modifier = Modifier.align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = stringResource(R.string.profile_setup_title),
          style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
          color = TelefamPureWhite,
        )
        Text(
          text = stringResource(R.string.profile_setup_subtitle),
          style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
          color = TelefamPureWhite.copy(alpha = 0.88f),
        )
      }

      CompactThemeToggleChip(
        themeMode = themeMode,
        onToggle = onToggleTheme,
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .padding(end = 6.dp),
      )
    }

    // Rounded-top Sheet Body (adapts automatically to Light/Dark & Tablet/Desktop)
    Surface(
      modifier = Modifier.fillMaxSize(),
      shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
      color = MaterialTheme.colorScheme.background,
    ) {
      BoxWithConstraints(
        modifier = Modifier
          .fillMaxSize()
          .navigationBarsPadding()
          .imePadding(),
      ) {
        val isWideLayout = maxWidth >= 720.dp
        val scrollState = rememberScrollState()

        if (isWideLayout) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .verticalScroll(scrollState)
              .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Row(
              modifier = Modifier.widthIn(max = 860.dp).fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(28.dp),
              verticalAlignment = Alignment.Top,
            ) {
              Column(
                modifier = Modifier.weight(0.42f),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                AvatarPickerSection(formState = formState, onClick = openPhotoPicker)
                Spacer(modifier = Modifier.height(18.dp))
                ProfileBioField(bio = formState.bio, onBioChanged = onBioChanged)
              }
              Column(
                modifier = Modifier.weight(0.58f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                ProfileIdentityAndContactFields(
                  formState = formState,
                  onFullNameChanged = onFullNameChanged,
                  onUsernameChanged = onUsernameChanged,
                  onCountryClick = { showCountryDialog = true },
                  onPhoneDigitsChanged = onPhoneDigitsChanged,
                  showGenderMenu = showGenderMenu,
                  onToggleGenderMenu = { showGenderMenu = it },
                  onGenderSelected = onGenderSelected,
                  onDobClick = { showDobPicker = true },
                  onLocationChanged = onLocationChanged,
                )
                ProfileSubmitSection(
                  errorMessage = formState.errorMessage,
                  isSaving = formState.isSaving,
                  isCompactButton = true,
                  onContinue = onContinue,
                )
              }
            }
          }
        } else {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .verticalScroll(scrollState)
              .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Column(
              modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              AvatarPickerSection(formState = formState, onClick = openPhotoPicker)
              ProfileIdentityAndContactFields(
                formState = formState,
                onFullNameChanged = onFullNameChanged,
                onUsernameChanged = onUsernameChanged,
                onCountryClick = { showCountryDialog = true },
                onPhoneDigitsChanged = onPhoneDigitsChanged,
                showGenderMenu = showGenderMenu,
                onToggleGenderMenu = { showGenderMenu = it },
                onGenderSelected = onGenderSelected,
                onDobClick = { showDobPicker = true },
                onLocationChanged = onLocationChanged,
              )
              ProfileBioField(bio = formState.bio, onBioChanged = onBioChanged)
              Spacer(modifier = Modifier.height(4.dp))
              ProfileSubmitSection(
                errorMessage = formState.errorMessage,
                isSaving = formState.isSaving,
                isCompactButton = false,
                onContinue = onContinue,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AvatarPickerSection(
  formState: ProfileFormState,
  onClick: () -> Unit,
) {
  val isDark = LocalTelefamDarkTheme.current
  val circleFill = if (isDark) TelefamDarkBlush else TelefamBlushWave

  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Box(
      modifier = Modifier
        .size(96.dp)
        .clickable(onClick = onClick)
        .testTag("profile_photo_picker"),
      contentAlignment = Alignment.Center,
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
          color = TelefamCrimson,
          style = Stroke(
            width = 1.6.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
          ),
        )
      }

      Box(
        modifier = Modifier
          .size(86.dp)
          .clip(CircleShape)
          .background(circleFill),
        contentAlignment = Alignment.Center,
      ) {
        val processed = formState.processedAvatar
        if (processed != null) {
          val imgBmp = remember(processed.mediaId) { processed.previewBitmap.asImageBitmap() }
          Image(
            bitmap = imgBmp,
            contentDescription = stringResource(R.string.cd_upload_photo),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          Icon(
            imageVector = Icons.Filled.CameraAlt,
            contentDescription = stringResource(R.string.cd_upload_photo),
            tint = TelefamCrimson,
            modifier = Modifier.size(30.dp),
          )
        }
      }

      Box(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .offset(x = (-2).dp, y = (-2).dp)
          .size(28.dp)
          .clip(CircleShape)
          .background(TelefamCrimson)
          .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Filled.Add,
          contentDescription = null,
          tint = TelefamPureWhite,
          modifier = Modifier.size(16.dp),
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Text(
      text = stringResource(R.string.profile_upload_photo),
      style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = stringResource(R.string.profile_upload_photo_desc),
      style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
      modifier = Modifier.widthIn(max = 260.dp),
    )
  }
}

@Composable
private fun ProfileIdentityAndContactFields(
  formState: ProfileFormState,
  onFullNameChanged: (String) -> Unit,
  onUsernameChanged: (String) -> Unit,
  onCountryClick: () -> Unit,
  onPhoneDigitsChanged: (String) -> Unit,
  showGenderMenu: Boolean,
  onToggleGenderMenu: (Boolean) -> Unit,
  onGenderSelected: (String) -> Unit,
  onDobClick: () -> Unit,
  onLocationChanged: (String) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    // Full Name
    ProfileFieldLabel(Icons.Filled.Person, stringResource(R.string.profile_full_name))
    OutlinedTextField(
      value = formState.fullName,
      onValueChange = onFullNameChanged,
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .testTag("profile_full_name_input"),
      singleLine = true,
      placeholder = { Text(stringResource(R.string.profile_full_name_hint), fontSize = 14.sp) },
      shape = RoundedCornerShape(12.dp),
      colors = profileTextFieldColors(),
    )

    // Username with live uniqueness check
    ProfileFieldLabel(Icons.Outlined.AlternateEmail, stringResource(R.string.profile_username))
    OutlinedTextField(
      value = formState.username,
      onValueChange = onUsernameChanged,
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .testTag("profile_username_input"),
      singleLine = true,
      placeholder = { Text(stringResource(R.string.profile_username_hint), fontSize = 14.sp) },
      trailingIcon = {
        when (formState.isUsernameAvailable) {
          true -> Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = TelefamStatusOnline, modifier = Modifier.size(18.dp))
          false -> Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
          null -> Unit
        }
      },
      shape = RoundedCornerShape(12.dp),
      colors = profileTextFieldColors(),
    )

    // Phone Number with Country Code Selector
    ProfileFieldLabel(Icons.Filled.Phone, stringResource(R.string.profile_phone))
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
          .clickable(onClick = onCountryClick)
          .padding(horizontal = 12.dp, vertical = 12.dp)
          .testTag("profile_country_button"),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(text = formState.selectedCountry.flagEmoji, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = formState.selectedCountry.dialCode,
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
          color = MaterialTheme.colorScheme.onSurface,
        )
        Icon(
          imageVector = Icons.Filled.KeyboardArrowDown,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp),
        )
      }

      Box(
        modifier = Modifier
          .width(1.dp)
          .height(28.dp)
          .background(MaterialTheme.colorScheme.outlineVariant),
      )

      OutlinedTextField(
        value = formState.phoneDigits,
        onValueChange = onPhoneDigitsChanged,
        modifier = Modifier
          .weight(1f)
          .testTag("profile_phone_input"),
        singleLine = true,
        placeholder = { Text(stringResource(R.string.profile_phone_hint), fontSize = 14.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = Color.Transparent,
          unfocusedBorderColor = Color.Transparent,
          focusedContainerColor = Color.Transparent,
          unfocusedContainerColor = Color.Transparent,
          cursorColor = MaterialTheme.colorScheme.primary,
        ),
      )
    }

    // Gender Selector
    ProfileFieldLabel(Icons.Outlined.Group, stringResource(R.string.profile_gender))
    Box(modifier = Modifier.fillMaxWidth()) {
      SelectorRowBox(
        text = formState.gender.ifBlank { stringResource(R.string.profile_gender_hint) },
        isPlaceholder = formState.gender.isBlank(),
        onClick = { onToggleGenderMenu(true) },
        testTag = "profile_gender_selector",
      )
      DropdownMenu(
        expanded = showGenderMenu,
        onDismissRequest = { onToggleGenderMenu(false) },
      ) {
        listOf("Male", "Female", "Non-binary", "Prefer not to say").forEach { option ->
          DropdownMenuItem(
            text = { Text(option) },
            onClick = {
              onGenderSelected(option)
              onToggleGenderMenu(false)
            },
          )
        }
      }
    }

    // Date of Birth (rejects below 13)
    ProfileFieldLabel(Icons.Filled.CalendarMonth, stringResource(R.string.profile_dob))
    SelectorRowBox(
      text = formState.dobDisplay.ifBlank { stringResource(R.string.profile_dob_hint) },
      isPlaceholder = formState.dobDisplay.isBlank(),
      onClick = onDobClick,
      testTag = "profile_dob_selector",
    )

    // Location
    ProfileFieldLabel(Icons.Filled.LocationOn, stringResource(R.string.profile_location))
    OutlinedTextField(
      value = formState.location,
      onValueChange = onLocationChanged,
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .testTag("profile_location_input"),
      singleLine = true,
      placeholder = { Text(stringResource(R.string.profile_location_hint), fontSize = 14.sp) },
      shape = RoundedCornerShape(12.dp),
      colors = profileTextFieldColors(),
    )
  }
}

@Composable
private fun ProfileBioField(
  bio: String,
  onBioChanged: (String) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    ProfileFieldLabel(Icons.Outlined.ChatBubbleOutline, stringResource(R.string.profile_bio))
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.surface),
    ) {
      OutlinedTextField(
        value = bio,
        onValueChange = onBioChanged,
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 98.dp)
          .padding(bottom = 18.dp)
          .testTag("profile_bio_input"),
        placeholder = { Text(stringResource(R.string.profile_bio_hint), fontSize = 14.sp) },
        maxLines = 4,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = Color.Transparent,
          unfocusedBorderColor = Color.Transparent,
          focusedContainerColor = Color.Transparent,
          unfocusedContainerColor = Color.Transparent,
          cursorColor = MaterialTheme.colorScheme.primary,
        ),
      )
      Text(
        text = "${bio.length}/150",
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(end = 12.dp, bottom = 8.dp),
      )
    }
  }
}

@Composable
private fun ProfileSubmitSection(
  errorMessage: String?,
  isSaving: Boolean,
  isCompactButton: Boolean,
  onContinue: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    if (!errorMessage.isNullOrBlank()) {
      Text(
        text = errorMessage,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 8.dp)
          .testTag("profile_error_text"),
      )
    }

    Button(
      onClick = onContinue,
      enabled = !isSaving,
      modifier = Modifier
        .then(if (isCompactButton) Modifier.widthIn(min = 180.dp) else Modifier.fillMaxWidth())
        .height(48.dp)
        .testTag("profile_continue_button"),
      shape = RoundedCornerShape(12.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = TelefamCrimson,
        contentColor = TelefamPureWhite,
      ),
    ) {
      if (isSaving) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = TelefamPureWhite, strokeWidth = 2.dp)
      } else {
        Text(
          text = stringResource(R.string.profile_continue),
          style = MaterialTheme.typography.labelLarge,
        )
      }
    }
  }
}

@Composable
private fun ProfileFieldLabel(icon: ImageVector, label: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = TelefamCrimson,
      modifier = Modifier.size(16.dp),
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
      color = MaterialTheme.colorScheme.onBackground,
    )
  }
}

@Composable
private fun SelectorRowBox(
  text: String,
  isPlaceholder: Boolean,
  onClick: () -> Unit,
  testTag: String,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(50.dp)
      .clip(RoundedCornerShape(12.dp))
      .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
      .background(MaterialTheme.colorScheme.surface)
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp)
      .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
      color = if (isPlaceholder) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
      } else {
        MaterialTheme.colorScheme.onSurface
      },
    )
    Icon(
      imageVector = Icons.Filled.KeyboardArrowDown,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(20.dp),
    )
  }
}

@Composable
internal fun CountrySearchDialog(
  onDismiss: () -> Unit,
  onSelect: (CountryDialOption) -> Unit,
) {
  var query by remember { mutableStateOf("") }
  val filtered = remember(query) {
    val q = query.trim().lowercase()
    if (q.isEmpty()) {
      CountryCatalog.allCountries
    } else {
      CountryCatalog.allCountries.filter {
        it.name.lowercase().contains(q) ||
          it.dialCode.contains(q) ||
          it.iso.lowercase().contains(q)
      }
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 520.dp),
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("country_search_input"),
          singleLine = true,
          leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
          placeholder = { Text(stringResource(R.string.profile_search_country), fontSize = 14.sp) },
          shape = RoundedCornerShape(12.dp),
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
          items(filtered, key = { it.iso }) { country ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(country) }
                .padding(horizontal = 8.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(text = country.flagEmoji, fontSize = 20.sp)
              Spacer(modifier = Modifier.width(12.dp))
              Text(
                text = country.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
              )
              Text(
                text = country.dialCode,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.primary,
              )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
          }
        }
      }
    }
  }
}

@Composable
private fun profileTextFieldColors() = OutlinedTextFieldDefaults.colors(
  focusedBorderColor = MaterialTheme.colorScheme.primary,
  unfocusedBorderColor = MaterialTheme.colorScheme.outline,
  focusedContainerColor = MaterialTheme.colorScheme.surface,
  unfocusedContainerColor = MaterialTheme.colorScheme.surface,
  cursorColor = MaterialTheme.colorScheme.primary,
)
