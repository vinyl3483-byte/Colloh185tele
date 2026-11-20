package telefam.company.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.telefam.company.app.R
import telefam.company.core.SmartEntity
import telefam.company.ui.theme.TelefamCrimson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartPhoneActionSheet(
  phone: SmartEntity.PhoneNumber,
  onDismiss: () -> Unit,
  onCopied: (String) -> Unit,
) {
  val context = LocalContext.current
  val clipboard = LocalClipboardManager.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Box(
      modifier = Modifier.fillMaxWidth(),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 560.dp)
          .fillMaxWidth()
          .padding(bottom = 24.dp),
      ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
          Text(
            text = phone.formattedDisplay,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SmartSheetActionRow(
          icon = Icons.Rounded.Call,
          label = stringResource(R.string.smart_phone_call),
          accentColor = TelefamCrimson,
          testTag = "smart_phone_action_call",
          onClick = {
            runCatching {
              context.startActivity(
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.dialNumber}"))
                  .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
              )
            }
            onDismiss()
          },
        )
        SmartSheetActionRow(
          icon = Icons.Rounded.ContentCopy,
          label = stringResource(R.string.smart_phone_copy),
          accentColor = MaterialTheme.colorScheme.primary,
          testTag = "smart_phone_action_copy",
          onClick = {
            clipboard.setText(AnnotatedString(phone.dialNumber))
            onCopied(phone.dialNumber)
            onDismiss()
          },
        )
        SmartSheetActionRow(
          icon = Icons.Rounded.PersonAdd,
          label = stringResource(R.string.smart_phone_save_contact),
          accentColor = Color(0xFF0EA5E9),
          testTag = "smart_phone_action_save_contact",
          onClick = {
            runCatching {
              val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
                type = ContactsContract.RawContacts.CONTENT_TYPE
                putExtra(ContactsContract.Intents.Insert.PHONE, phone.dialNumber)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
              }
              context.startActivity(intent)
            }
            onDismiss()
          },
        )
        SmartSheetActionRow(
          icon = Icons.AutoMirrored.Rounded.Message,
          label = stringResource(R.string.smart_phone_send_message),
          accentColor = Color(0xFF16A34A),
          testTag = "smart_phone_action_sms",
          onClick = {
            runCatching {
              context.startActivity(
                Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${phone.dialNumber}"))
                  .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
              )
            }
            onDismiss()
          },
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartEmailActionSheet(
  email: SmartEntity.Email,
  onDismiss: () -> Unit,
  onCopied: (String) -> Unit,
) {
  val context = LocalContext.current
  val clipboard = LocalClipboardManager.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Box(
      modifier = Modifier.fillMaxWidth(),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 560.dp)
          .fillMaxWidth()
          .padding(bottom = 24.dp),
      ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
          Text(
            text = email.emailAddress,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SmartSheetActionRow(
          icon = Icons.Rounded.Email,
          label = stringResource(R.string.smart_email_send),
          accentColor = TelefamCrimson,
          testTag = "smart_email_action_send",
          onClick = {
            runCatching {
              context.startActivity(
                Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Uri.encode(email.emailAddress)}"))
                  .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
              )
            }
            onDismiss()
          },
        )
        SmartSheetActionRow(
          icon = Icons.Rounded.ContentCopy,
          label = stringResource(R.string.smart_email_copy),
          accentColor = MaterialTheme.colorScheme.primary,
          testTag = "smart_email_action_copy",
          onClick = {
            clipboard.setText(AnnotatedString(email.emailAddress))
            onCopied(email.emailAddress)
            onDismiss()
          },
        )
        Spacer(Modifier.height(4.dp))
      }
    }
  }
}

@Composable
private fun SmartSheetActionRow(
  icon: ImageVector,
  label: String,
  accentColor: Color,
  testTag: String,
  onClick: () -> Unit,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .defaultMinSize(minHeight = 52.dp)
      .clickable(onClick = onClick)
      .padding(horizontal = 20.dp, vertical = 10.dp)
      .testTag(testTag),
  ) {
    Surface(
      shape = CircleShape,
      color = accentColor.copy(alpha = 0.12f),
      modifier = Modifier.size(38.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = label,
          tint = accentColor,
          modifier = Modifier.size(20.dp),
        )
      }
    }
    Spacer(Modifier.width(14.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.bodyLarge,
      fontWeight = FontWeight.Medium,
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}
