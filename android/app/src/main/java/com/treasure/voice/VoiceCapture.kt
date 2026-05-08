package com.treasure.voice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlin.random.Random

/**
 * Full-screen mic overlay with real Android [SpeechRecognizer] backing.
 *
 * Lifecycle:
 * 1. Requests RECORD_AUDIO permission if missing.
 * 2. Creates a SpeechRecognizer, starts listening with zh-CN free-form,
 *    streams partial results into the overlay text.
 * 3. User taps anywhere to commit → stopListening → onResult(final).
 * 4. Dispose tears the recognizer down.
 *
 * If [SpeechRecognizer.isRecognitionAvailable] returns false (some Chinese
 * ROMs without Google services), we fall through with [onUnavailable] so
 * the caller can offer the prior stub flow instead.
 */
@Composable
fun VoiceCapture(
    onResult: (String) -> Unit,
    onCancel: () -> Unit,
    onUnavailable: () -> Unit,
) {
    val context = LocalContext.current
    var permGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permGranted = granted
        if (!granted) onCancel()
    }
    LaunchedEffect(Unit) {
        if (!permGranted) permLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
    if (!permGranted) {
        // The system permission dialog covers the screen; once it's
        // dismissed our LaunchedEffect's onCancel will fire if denied.
        return
    }

    var partial by remember { mutableStateOf("") }
    var rmsBars by remember { mutableStateOf(List(15) { 24f }) }
    var done by remember { mutableStateOf(false) }
    var stopRequested by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onUnavailable()
            return@DisposableEffect onDispose {}
        }
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {
                // Convert dB into a height multiplier. SpeechRecognizer
                // hands us values in roughly [-2, 10]; clamp + animate.
                val scaled = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                rmsBars = rmsBars.shuffled().mapIndexed { idx, h ->
                    val target = 14f + scaled * 50f + (Random.nextFloat() - 0.5f) * 10f
                    (h * 0.6f + target * 0.4f).coerceAtLeast(8f)
                }
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.let { partial = it }
            }
            override fun onResults(results: Bundle?) {
                if (done) return
                done = true
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?: partial
                if (text.isNotBlank()) onResult(text) else onCancel()
            }
            override fun onError(error: Int) {
                if (done) return
                done = true
                if (partial.isNotBlank()) onResult(partial) else onCancel()
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        recognizer.setRecognitionListener(listener)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        recognizer.startListening(intent)
        onDispose {
            recognizer.stopListening()
            recognizer.destroy()
        }
    }

    LaunchedEffect(stopRequested) {
        if (stopRequested) {
            // Listener will fire onResults / onError shortly; we let it
            // call onResult / onCancel so the caller's flow stays uniform.
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC1A1815))
            .clickable(onClick = {
                if (!stopRequested) {
                    stopRequested = true
                    if (partial.isNotBlank()) {
                        if (!done) {
                            done = true
                            onResult(partial)
                        }
                    }
                    // The DisposableEffect's onDispose will stop the
                    // recognizer once this composable leaves composition.
                }
            }),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 130.dp),
        ) {
            LiveWaveform(
                bars = rmsBars,
                color = Color(0xFFF4F1EA),
            )
            Spacer(Modifier.height(22.dp))
            Text(
                text = if (partial.isBlank()) "（请讲话…）" else "\"$partial\"",
                color = Color(0xFFF4F1EA),
                style = MaterialTheme.typography.titleMedium,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "TAP TO STOP",
                color = Color(0xFFF4F1EA).copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun LiveWaveform(bars: List<Float>, color: Color) {
    Canvas(
        modifier = Modifier
            .height(60.dp)
            .width((bars.size * 7).dp),
    ) {
        val unit = 7.dp.toPx()
        val barWidth = 4.dp.toPx()
        val cy = size.height / 2f
        bars.forEachIndexed { idx, h ->
            val left = idx * unit
            val px = h.dp.toPx().coerceAtLeast(8.dp.toPx())
            drawRect(
                color = color.copy(alpha = 0.85f),
                topLeft = Offset(left, cy - px / 2f),
                size = Size(width = barWidth, height = px),
            )
        }
    }
}
