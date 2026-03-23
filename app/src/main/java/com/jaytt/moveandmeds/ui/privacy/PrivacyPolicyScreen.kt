package com.jaytt.moveandmeds.ui.privacy

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Move & Meds Privacy Policy",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Move & Meds does not collect, transmit, or share any personal data. All information you enter — including medicines, exercises, and reminder schedules — is stored locally on your device only.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "No internet connection is required or used by this app. No analytics, crash reporting, or advertising SDKs are included.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Your health data never leaves your device.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:movesandmeds@gmail.com"))
                    )
                },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    "Contact: movesandmeds@gmail.com",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
