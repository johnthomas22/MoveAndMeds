package com.jaytt.moveandmeds

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jaytt.moveandmeds.ui.theme.MoveAndMedsTheme

class ExerciseInfoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val name = intent.getStringExtra("exercise_name") ?: ""
        val notes = intent.getStringExtra("exercise_notes") ?: ""
        val imagePath = intent.getStringExtra("exercise_image_path")

        setContent {
            MoveAndMedsTheme {
                AlertDialog(
                    onDismissRequest = { finish() },
                    title = {
                        Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (notes.isNotBlank()) notes else "No description available.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (imagePath != null) {
                                val bitmap = remember(imagePath) {
                                    runCatching { BitmapFactory.decodeFile(imagePath) }.getOrNull()
                                }
                                bitmap?.let {
                                    Surface(
                                        shape = MaterialTheme.shapes.medium,
                                        tonalElevation = 2.dp
                                    ) {
                                        Image(
                                            bitmap = it.asImageBitmap(),
                                            contentDescription = "Exercise illustration",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 300.dp),
                                            contentScale = ContentScale.FillWidth
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { finish() }) { Text("Close") }
                    }
                )
            }
        }
    }
}
