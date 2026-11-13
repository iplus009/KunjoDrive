package com.novo.drive.core.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

class AndroidMlKitOcrExtractor(
    private val context: Context
) : OcrExtractor {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    override suspend fun extractText(localPath: String): String? {
        val file = File(localPath)
        if (!file.exists() || !file.isFile) return null
        val image = runCatching {
            InputImage.fromFilePath(context, Uri.fromFile(file))
        }.getOrNull() ?: return null

        return suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    if (cont.isActive) {
                        cont.resume(visionText.text.trim())
                    }
                }
                .addOnFailureListener {
                    if (cont.isActive) {
                        cont.resume(null)
                    }
                }
        }
    }
}

