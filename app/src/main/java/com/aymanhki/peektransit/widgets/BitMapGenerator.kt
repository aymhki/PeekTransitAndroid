import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.annotation.ColorInt
import androidx.annotation.FontRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap


fun generateTextBitmap(
    context: Context,
    @FontRes fontResId: Int,
    maxImageWidthSize: Int?,
    maxLines: Int,
    fontSize: Float,
    @ColorInt fontColor: Int,
    text: String
): Bitmap? {
    val typeface = ResourcesCompat.getFont(context, fontResId) ?: return null
    val density = context.resources.displayMetrics.density

    val textPaint = TextPaint().apply {
        isAntiAlias = true
        this.typeface = typeface
        this.textSize = fontSize * density
        color = fontColor
    }

    val maxImageWidthInPixels = if (maxImageWidthSize != null) {
        (maxImageWidthSize * density).toInt()
    } else {
        textPaint.measureText(text).toInt()
    }.coerceAtLeast(1)

    val builder = StaticLayout.Builder.obtain(
        text, 0, text.length, textPaint, maxImageWidthInPixels
    ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setMaxLines(maxLines)

    if (maxImageWidthSize != null) {
        builder.setEllipsize(TextUtils.TruncateAt.END)
    }

    val staticLayout = builder.build()

    val bitmap = createBitmap(staticLayout.width, staticLayout.height)
    val canvas = Canvas(bitmap)

    staticLayout.draw(canvas)

    return bitmap
}