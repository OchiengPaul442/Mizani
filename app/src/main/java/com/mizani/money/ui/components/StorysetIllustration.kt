package com.mizani.money.ui.components

import android.graphics.Picture
import android.graphics.RectF
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.caverock.androidsvg.SVG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Keep the three bundled illustrations available after navigation so revisiting a screen does not
// read, parse, and render the same SVG again. The entry limit bounds retained Picture memory.
private val renderedIllustrations = LruCache<String, Picture>(3)

/** Renders a bundled Storyset SVG as vector artwork without network access. */
@Composable
fun StorysetIllustration(
    assetName: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val context = LocalContext.current
    val picture = produceState<Picture?>(initialValue = null, assetName) {
        value = withContext(Dispatchers.IO) {
            renderedIllustrations[assetName] ?: runCatching {
                SVG.getFromAsset(context.assets, "storyset/$assetName").renderToPicture()
            }.getOrNull()?.also { renderedIllustrations.put(assetName, it) }
        }
    }.value
    val imageModifier = if (contentDescription == null) {
        modifier
    } else {
        modifier.semantics { this.contentDescription = contentDescription }
    }

    Canvas(imageModifier) {
        val image = picture ?: return@Canvas
        if (image.width <= 0 || image.height <= 0) return@Canvas

        val scale = minOf(size.width / image.width, size.height / image.height)
        val width = image.width * scale
        val height = image.height * scale
        val left = (size.width - width) / 2f
        val top = (size.height - height) / 2f
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawPicture(
                image,
                RectF(left, top, left + width, top + height),
            )
        }
    }
}
