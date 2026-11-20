package telefam.company.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.telefam.company.app.BuildConfig
import com.telefam.company.app.R
import kotlinx.coroutines.launch
import telefam.company.core.TelefamCoreEngine
import telefam.company.ui.components.CompactThemeToggleChip
import telefam.company.ui.components.GoogleGLogo
import telefam.company.ui.components.TelefamBackgroundWaves
import telefam.company.ui.components.TelefamLogoBadge
import telefam.company.ui.theme.TelefamCrimson
import telefam.company.ui.theme.TelefamPureWhite
import telefam.company.ui.viewmodel.AuthStep
import telefam.company.ui.viewmodel.TelefamUiState

@Composable
fun TelefamAuthScreen(
  uiState: TelefamUiState,
  onEmailChanged: (String) -> Unit,
  onSendOtp: () -> Unit,
  onOtpChanged: (String) -> Unit,
  onVerifyOtp: () -> Unit,
  onResendOtp: () -> Unit,
  onChangeEmail: () -> Unit,
  onGoogleTokenSuccess: (idToken: String, email: String) -> Unit,
  onGoogleTokenError: () -> Unit,
  onToggleTheme: () -> Unit,
  onPrepareTryAnotherWay: () -> Unit = {},
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val launchGoogleSignIn: () -> Unit = {
    val serverClientId = BuildConfig.TELEFAM_GOOGLE_CLIENT_ID.trim()
    if (serverClientId.isBlank()) {
      onGoogleTokenError()
    } else {
      scope.launch {
        runCatching {
          val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .build()
          val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
          val result = credentialManager.getCredential(context = context, request = request)
          val credential = result.credential
          if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
          ) {
            val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
            onGoogleTokenSuccess(googleCred.idToken, googleCred.id)
          } else {
            onGoogleTokenError()
          }
        }.onFailure {
          onGoogleTokenError()
        }
      }
    }
  }

  val waitState = uiState.pendingLoginApprovalWait
  if ((uiState.authStep == AuthStep.DEVICE_APPROVAL_WAIT || uiState.authStep == AuthStep.DEVICE_APPROVAL_DENIED) && waitState != null) {
    DeviceApprovalWaitScreen(
      waitState = waitState,
      isDeniedBlocked = uiState.authStep == AuthStep.DEVICE_APPROVAL_DENIED,
      isLoading = uiState.isLoading,
      errorMessage = uiState.errorMessage,
      onBackChangeEmail = onChangeEmail,
      onTryAnotherWayGoogle = {
        onPrepareTryAnotherWay()
        launchGoogleSignIn()
      },
    )
    return
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
  ) {
    val isExpandedWidth = maxWidth >= 600.dp
    val isShortViewport = maxHeight < 640.dp
    val horizontalPadding = if (isExpandedWidth) 32.dp else 20.dp
    val cardMaxWidth = if (isExpandedWidth) 430.dp else 400.dp

    TelefamBackgroundWaves()

    CompactThemeToggleChip(
      themeMode = uiState.themeMode,
      onToggle = onToggleTheme,
      modifier = Modifier
        .align(Alignment.TopEnd)
        .statusBarsPadding()
        .padding(top = 10.dp, end = 14.dp),
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .imePadding()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = horizontalPadding, vertical = if (isShortViewport) 16.dp else 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Spacer(modifier = Modifier.height(if (isShortViewport) 4.dp else 12.dp))

      TelefamLogoBadge(
        size = if (isShortViewport) 62.dp else 74.dp,
        animated = true,
        contentDescriptionText = stringResource(R.string.cd_telefam_logo),
      )

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = stringResource(R.string.brand_tagline),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(if (isShortViewport) 18.dp else 24.dp))

      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = cardMaxWidth)
          .shadow(
            elevation = 16.dp,
            shape = RoundedCornerShape(24.dp),
            ambientColor = Color(0x18000000),
            spotColor = Color(0x1F000000),
          )
          .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
      ) {
        AnimatedContent(
          targetState = uiState.authStep,
          transitionSpec = { fadeIn() togetherWith fadeOut() },
          label = "auth_step_transition",
        ) { step ->
          when (step) {
            AuthStep.EMAIL_INPUT -> {
              EmailAuthCardContent(
                emailInput = uiState.emailInput,
                emailAccountNotice = uiState.emailAccountNotice,
                isLoading = uiState.isLoading,
                errorMessage = uiState.errorMessage,
                onEmailChanged = onEmailChanged,
                onSendOtp = onSendOtp,
                onContinueWithGoogle = launchGoogleSignIn,
              )
            }

            AuthStep.OTP_VERIFICATION -> {
              OtpAuthCardContent(
                email = uiState.emailInput,
                otpCode = uiState.otpCode,
                isLoading = uiState.isLoading,
                errorMessage = uiState.errorMessage,
                cooldownRemainingSeconds = uiState.cooldownRemainingSeconds,
                activeCooldownDurationSeconds = uiState.activeCooldownDurationSeconds,
                onOtpChanged = onOtpChanged,
                onVerifyOtp = onVerifyOtp,
                onResendOtp = onResendOtp,
                onChangeEmail = onChangeEmail,
              )
            }

            AuthStep.DEVICE_APPROVAL_WAIT,
            AuthStep.DEVICE_APPROVAL_DENIED,
            -> Unit
          }
        }
      }

      Spacer(modifier = Modifier.height(if (isShortViewport) 24.dp else 44.dp))
    }
  }
}

@Composable
private fun EmailAuthCardContent(
  emailInput: String,
  emailAccountNotice: String?,
  isLoading: Boolean,
  errorMessage: String?,
  onEmailChanged: (String) -> Unit,
  onSendOtp: () -> Unit,
  onContinueWithGoogle: () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp, vertical = 26.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = stringResource(R.string.auth_email_title),
      style = MaterialTheme.typography.headlineSmall,
      color = MaterialTheme.colorScheme.primary,
      textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
      text = stringResource(R.string.auth_email_subtitle),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(18.dp))

    Text(
      text = stringResource(R.string.auth_email_label),
      style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
      value = emailInput,
      onValueChange = onEmailChanged,
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .testTag("email_input"),
      singleLine = true,
      enabled = !isLoading,
      placeholder = {
        Text(
          text = stringResource(R.string.auth_email_placeholder),
          style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
      },
      leadingIcon = {
        Icon(
          imageVector = Icons.Outlined.Email,
          contentDescription = stringResource(R.string.cd_email_icon),
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
      },
      keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Email,
        imeAction = ImeAction.Done,
      ),
      keyboardActions = KeyboardActions(onDone = { onSendOtp() }),
      shape = RoundedCornerShape(12.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        cursorColor = MaterialTheme.colorScheme.primary,
      ),
    )

    if (!emailAccountNotice.isNullOrBlank() && errorMessage.isNullOrBlank()) {
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = emailAccountNotice,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Start,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("auth_email_account_notice"),
      )
    }

    if (!errorMessage.isNullOrBlank()) {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = errorMessage,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("auth_error_text"),
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Button(
      onClick = onSendOtp,
      enabled = !isLoading,
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .testTag("send_otp_button"),
      shape = RoundedCornerShape(12.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = TelefamCrimson,
        contentColor = TelefamPureWhite,
      ),
    ) {
      if (isLoading) {
        CircularProgressIndicator(
          modifier = Modifier.size(20.dp),
          color = TelefamPureWhite,
          strokeWidth = 2.dp,
        )
      } else {
        Text(
          text = stringResource(R.string.auth_send_otp),
          style = MaterialTheme.typography.labelLarge,
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      HorizontalDivider(modifier = Modifier.weight(1f), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
      Text(
        text = stringResource(R.string.auth_or),
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 12.dp),
      )
      HorizontalDivider(modifier = Modifier.weight(1f), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }

    Spacer(modifier = Modifier.height(14.dp))

    OutlinedButton(
      onClick = onContinueWithGoogle,
      enabled = !isLoading,
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .testTag("google_continue_button"),
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      colors = ButtonDefaults.outlinedButtonColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
      ),
      contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
      GoogleGLogo(size = 20.dp)
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = stringResource(R.string.auth_continue_google),
        style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
      )
    }
  }
}

@Composable
private fun OtpAuthCardContent(
  email: String,
  otpCode: String,
  isLoading: Boolean,
  errorMessage: String?,
  cooldownRemainingSeconds: Long,
  activeCooldownDurationSeconds: Long,
  onOtpChanged: (String) -> Unit,
  onVerifyOtp: () -> Unit,
  onResendOtp: () -> Unit,
  onChangeEmail: () -> Unit,
) {
  val otpFocusRequester = remember { FocusRequester() }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp, vertical = 26.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = stringResource(R.string.auth_otp_title),
      style = MaterialTheme.typography.headlineSmall,
      color = MaterialTheme.colorScheme.primary,
      textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text(
      text = stringResource(R.string.auth_otp_subtitle),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )

    Text(
      text = email,
      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
      color = MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
      text = stringResource(R.string.auth_otp_link_hint),
      style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(18.dp))

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
      BasicTextField(
        value = otpCode,
        onValueChange = onOtpChanged,
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .alpha(0.01f)
          .focusRequester(otpFocusRequester)
          .testTag("otp_hidden_input"),
        enabled = !isLoading,
        keyboardOptions = KeyboardOptions(
          keyboardType = KeyboardType.NumberPassword,
          imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onVerifyOtp() }),
        cursorBrush = SolidColor(Color.Transparent),
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable(enabled = !isLoading) { otpFocusRequester.requestFocus() },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        for (index in 0 until 6) {
          val char = otpCode.getOrNull(index)
          val isActiveSlot = index == otpCode.length.coerceAtMost(5)
          Box(
            modifier = Modifier
              .weight(1f)
              .height(50.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(MaterialTheme.colorScheme.surface)
              .border(
                width = if (isActiveSlot) 1.5.dp else 1.dp,
                color = if (isActiveSlot) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp),
              )
              .testTag("otp_box_$index"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = char?.toString() ?: "•",
              style = MaterialTheme.typography.titleMedium.copy(
                fontSize = if (char != null) 20.sp else 18.sp,
                fontWeight = if (char != null) FontWeight.Bold else FontWeight.Medium,
              ),
              color = if (char != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    if (!errorMessage.isNullOrBlank()) {
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = errorMessage,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("otp_error_text"),
      )
    }

    Spacer(modifier = Modifier.height(18.dp))

    Button(
      onClick = onVerifyOtp,
      enabled = !isLoading,
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .testTag("verify_otp_button"),
      shape = RoundedCornerShape(12.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = TelefamCrimson,
        contentColor = TelefamPureWhite,
      ),
    ) {
      if (isLoading) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = TelefamPureWhite, strokeWidth = 2.dp)
      } else {
        Text(text = stringResource(R.string.auth_verify_otp), style = MaterialTheme.typography.labelLarge)
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    if (cooldownRemainingSeconds > 0L) {
      val formattedRemaining = TelefamCoreEngine.formatCountdown(cooldownRemainingSeconds)
      val formattedWindow = TelefamCoreEngine.formatCountdown(activeCooldownDurationSeconds)

      Text(
        text = buildAnnotatedString {
          append(stringResource(R.string.auth_resend_in_prefix))
          append(" ")
          withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)) {
            append(formattedRemaining)
          }
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.testTag("resend_countdown_text"),
      )

      Spacer(modifier = Modifier.height(2.dp))

      Text(
        text = buildAnnotatedString {
          append(stringResource(R.string.auth_after_prefix))
          append(" ")
          append(formattedWindow)
          append(" ")
          append(stringResource(R.string.auth_you_can_prefix))
          append(" ")
          withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f), fontWeight = FontWeight.SemiBold)) {
            append(stringResource(R.string.auth_resend_code))
          }
        },
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    } else {
      TextButton(
        onClick = onResendOtp,
        enabled = !isLoading,
        modifier = Modifier
          .heightIn(min = 48.dp)
          .testTag("resend_otp_button"),
      ) {
        Text(
          text = stringResource(R.string.auth_resend_code),
          style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }

    Spacer(modifier = Modifier.height(2.dp))

    TextButton(
      onClick = onChangeEmail,
      enabled = !isLoading,
      modifier = Modifier
        .heightIn(min = 48.dp)
        .testTag("change_email_button"),
    ) {
      Text(
        text = stringResource(R.string.auth_change_email),
        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.primary,
      )
    }
  }
}
