package com.mizani.money.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mizani.money.BuildConfig
import com.mizani.money.core.legal.LegalDocuments
import com.mizani.money.ui.components.MizaniCard
import com.mizani.money.ui.components.MizaniTopBar
import com.mizani.money.ui.components.Spacing
import com.mizani.money.ui.components.StatusBanner

enum class FeedbackTopic(
    val title: String,
    val subjectLabel: String,
) {
    SUGGESTION("Suggestion", "suggestion"),
    PROBLEM("Report a problem", "problem report"),
    QUESTION("Question", "question"),
    OTHER("Something else", "feedback"),
}

data class FeedbackValidationResult(
    val message: String? = null,
)

/** Builds the user-confirmed email handoff without adding a network client or app secret. */
object FeedbackComposer {
    const val MAX_MESSAGE_LENGTH = 4_000
    const val OFFICIAL_EMAIL = LegalDocuments.PRIVACY_CONTACT

    fun validate(topic: FeedbackTopic?, message: String): FeedbackValidationResult {
        if (topic == null) return FeedbackValidationResult("Choose what you would like to share.")
        if (message.isBlank()) return FeedbackValidationResult("Write a few words so we know how to help.")
        if (message.length > MAX_MESSAGE_LENGTH) {
            return FeedbackValidationResult("Keep your feedback under $MAX_MESSAGE_LENGTH characters.")
        }
        return FeedbackValidationResult()
    }

    fun subject(topic: FeedbackTopic): String = "Mizani feedback: ${topic.subjectLabel}"

    fun body(
        topic: FeedbackTopic,
        message: String,
        appVersion: String,
        androidVersion: String,
    ): String = buildString {
        appendLine(message.trim())
        appendLine()
        appendLine("---")
        appendLine("Topic: ${topic.subjectLabel}")
        appendLine("Mizani version: $appVersion")
        appendLine("Android version: $androidVersion")
        appendLine()
        append("Please do not reply with balances, transaction details, PINs, or backup passphrases.")
    }

    fun createIntent(
        topic: FeedbackTopic,
        message: String,
        appVersion: String,
        androidVersion: String,
    ): Intent = Intent(Intent.ACTION_SENDTO).apply {
        // Use an opaque mailto URI. A path-based builder would create mailto:/address,
        // which some email clients do not resolve as a recipient.
        data = Uri.parse("mailto:$OFFICIAL_EMAIL")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(OFFICIAL_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, subject(topic))
        putExtra(Intent.EXTRA_TEXT, body(topic, message, appVersion, androidVersion))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeedbackScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var selectedTopicName by rememberSaveable { mutableStateOf(FeedbackTopic.SUGGESTION.name) }
    var message by remember { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf<String?>(null) }
    var statusIsError by rememberSaveable { mutableStateOf(false) }
    val selectedTopic = FeedbackTopic.entries.firstOrNull { it.name == selectedTopicName }
        ?: FeedbackTopic.SUGGESTION

    fun openEmail() {
        val validation = FeedbackComposer.validate(selectedTopic, message)
        if (validation.message != null) {
            status = validation.message
            statusIsError = true
            return
        }

        val intent = FeedbackComposer.createIntent(
            topic = selectedTopic,
            message = message,
            appVersion = BuildConfig.VERSION_NAME,
            androidVersion = android.os.Build.VERSION.RELEASE ?: "unknown",
        )
        if (intent.resolveActivity(context.packageManager) == null) {
            status = "No email app is available on this device. Install one to continue."
            statusIsError = true
            return
        }
        runCatching { context.startActivity(intent) }
            .onSuccess {
                status = "Your email app is ready. Review the message and tap Send to deliver it."
                statusIsError = false
            }
            .onFailure {
                status = "We couldn't open an email app. Try again to continue."
                statusIsError = true
            }
    }

    Column(Modifier.fillMaxSize()) {
        MizaniTopBar(title = "Send feedback", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.bottom),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            MizaniCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Feedback,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(30.dp),
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        "Help shape Mizani",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "Tell us what is working, what could be clearer, or where you got stuck. Your feedback goes to the Mizani team.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text("What would you like to share?", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    FeedbackTopic.entries.forEach { topic ->
                        FilterChip(
                            selected = topic == selectedTopic,
                            onClick = {
                                selectedTopicName = topic.name
                                status = null
                            },
                            label = { Text(topic.title) },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (topic) {
                                        FeedbackTopic.SUGGESTION -> Icons.Rounded.Lightbulb
                                        FeedbackTopic.PROBLEM -> Icons.Rounded.BugReport
                                        FeedbackTopic.QUESTION -> Icons.AutoMirrored.Rounded.HelpOutline
                                        FeedbackTopic.OTHER -> Icons.Rounded.Feedback
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = message,
                onValueChange = {
                    if (it.length <= FeedbackComposer.MAX_MESSAGE_LENGTH) {
                        message = it
                        if (statusIsError) status = null
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Your feedback") },
                placeholder = { Text("What happened or what would you like to see?") },
                supportingText = {
                    Text(
                        "${message.length}/${FeedbackComposer.MAX_MESSAGE_LENGTH}",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                    )
                },
                minLines = 6,
                maxLines = 10,
            )

            MizaniCard {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Rounded.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        "Mizani opens your email app with the address and message filled in. Review it before sending. Email delivery and spam filtering are managed by your email provider.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "For your privacy, do not include balances, transaction details, PINs, or backup passphrases.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (status != null) {
                StatusBanner(text = status.orEmpty(), isError = statusIsError)
            }

            Button(
                onClick = ::openEmail,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null)
                Spacer(Modifier.width(Spacing.sm))
                Text("Open email app")
            }
        }
    }
}
