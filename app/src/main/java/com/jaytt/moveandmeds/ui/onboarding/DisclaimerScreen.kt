package com.jaytt.moveandmeds.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DisclaimerScreen(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    var showDeclineDialog by remember { mutableStateOf(false) }

    if (showDeclineDialog) {
        AlertDialog(
            onDismissRequest = { showDeclineDialog = false },
            title = { Text("Please Delete This App") },
            text = {
                Text(
                    "You have not accepted the disclaimer. Please uninstall Move & Meds from your device.\n\n" +
                    "Go to Settings → Apps → Move & Meds → Uninstall."
                )
            },
            confirmButton = {
                TextButton(onClick = onDecline) { Text("Close App") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Important Disclaimer",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                "Please read carefully before using this app.",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Who this app is for",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Move & Meds is intended for patients recovering from surgery or serious illness who wish to record reminders for medicines and activities as advised by their professional medical team.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "No medical advice",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "This app does not offer any medical advice whatsoever. It is a personal reminder tool only. Always follow the guidance of your doctor, pharmacist, or other qualified medical professional.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Limitation of liability",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "There is a possibility that a software fault could result in a reminder not being delivered, or not being delivered on time. The authors of this free app accept no liability whatsoever for any harm arising from missed or delayed reminders.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "You use this app entirely at your own risk.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onAccept,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("I Understand & Accept", style = MaterialTheme.typography.titleSmall)
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = { showDeclineDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("I Do Not Accept", style = MaterialTheme.typography.titleSmall)
        }
    }
}
