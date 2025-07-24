package com.aymanhki.peektransit.widgets.helpers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.RemoteViews
import androidx.annotation.ColorInt
import androidx.annotation.FontRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import com.aymanhki.peektransit.R
import com.aymanhki.peektransit.data.models.WidgetModel
import com.aymanhki.peektransit.managers.SettingsManager
import com.aymanhki.peektransit.utils.PeekTransitConstants
import com.aymanhki.peektransit.utils.PeekTransitConstants.getWidgetBackgroundColor
import com.aymanhki.peektransit.utils.PeekTransitConstants.getWidgetTextColor
import com.aymanhki.peektransit.utils.PeekTransitConstants.getWidgetTextFont
import com.aymanhki.peektransit.utils.StopViewTheme
class CentralWidgetLooksUpdateManager
{

    companion object {
        fun getFinalWidgetLook(
            context: Context,
            widgetConfig: WidgetModel?,
            appWidgetId: Int,
            layoutId: Int,
            initialLayoutId: Int,
            configureButtonId: Int,
            configureActivity: Class<*>
        ): RemoteViews {
            var views: RemoteViews

            if (widgetConfig != null) {
                views = RemoteViews(context.packageName, layoutId)
                views = updateWidgetLooks(context, views, appWidgetId, widgetConfig)
            } else {
                views = RemoteViews(context.packageName, initialLayoutId)
                views = updateWidgetLooksIfNoConfig(
                    context,
                    views,
                    appWidgetId,
                    configureActivity,
                    configureButtonId
                )
            }

            return views
        }

        fun updateWidgetLooks(
            context: Context,
            views: RemoteViews,
            appWidgetId: Int,
            widgetConfig: WidgetModel
        ): RemoteViews {
            val widgetSize = widgetConfig.widgetData["size"] as? String ?: "medium"
            val showLastUpdatedStatus = widgetConfig.widgetData["showLastUpdatedStatus"] as? Boolean ?: false
            val widgetScheduleData = PeekTransitConstants.getWidgetSchedule(context, appWidgetId.toString(), widgetConfig.id)
            val lastUpdatedTimeString = widgetScheduleData?.lastUpdatedTime ?: ""
            val locationCoordsString = "${widgetScheduleData?.userLocationLat ?: ""}, ${widgetScheduleData?.userLocationLon ?: ""}"
            val settingsManager = SettingsManager.getInstance(context)
            val currentTheme = settingsManager.stopViewTheme
            val isNightMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

            updateBackgroundColor(views, currentTheme, isNightMode)

            updateWidgetLastUpdated(
                context,
                views,
                widgetSize,
                isNightMode,
                currentTheme,
                lastUpdatedTimeString,
                showLastUpdatedStatus
            )

            updateLocationCoordinates(
                context,
                views,
                widgetSize,
                isNightMode,
                currentTheme,
                locationCoordsString
            )

            return views
        }

        fun updateWidgetLooksIfNoConfig(
            context: Context,
            views: RemoteViews,
            appWidgetId: Int,
            configureActivity: Class<*>,
            buttonId: Int
        ): RemoteViews {
            val configIntent = Intent(context, configureActivity)
            .apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val configPendingIntent = PendingIntent.getActivity(context, appWidgetId,
                configIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(buttonId, configPendingIntent)

            return views
        }

        fun updateBackgroundColor(
            views: RemoteViews,
            currentTheme: StopViewTheme,
            isNightMode: Boolean
        ): RemoteViews {
            views.setInt(R.id.peek_transit_large_layout, "setBackgroundColor", getWidgetBackgroundColor(currentTheme, isNightMode))

            return views
        }

        fun updateWidgetLastUpdated(
            context: Context,
            views: RemoteViews,
            widgetSize: String,
            isNightMode: Boolean,
            currentTheme: StopViewTheme,
            lastUpdatedTimeString: String,
            shouldShowLastUpdatedTextImage: Boolean
        ): RemoteViews {
            return if (shouldShowLastUpdatedTextImage) {
                updateWidgetLastUpdatedTextImage(
                    context,
                    views,
                    widgetSize,
                    isNightMode,
                    currentTheme,
                    lastUpdatedTimeString
                )
            } else {
                resetLastUpdatedTextImage(views)
            }
        }


        fun resetLastUpdatedTextImage(
            views: RemoteViews,
        ): RemoteViews {



            views.setViewVisibility(
                R.id.peek_transit_large_widget_last_updated_status_layout, GONE
            )

            views.setViewVisibility(
                R.id.peek_transit_large_widget_last_updated_status_text_image, GONE
            )

            views.setContentDescription(
                R.id.peek_transit_large_widget_last_updated_status_text_image,
                "Last updated status text image"
            )

            return views
        }

        fun updateWidgetLastUpdatedTextImage(
            context: Context,
            views: RemoteViews,
            widgetSize: String,
            isNightMode: Boolean,
            currentTheme: StopViewTheme,
            lastUpdatedTimeString: String
        ): RemoteViews {

            views.setViewVisibility(
                R.id.peek_transit_large_widget_last_updated_status_layout, VISIBLE
            )

            views.setViewVisibility(
                R.id.peek_transit_large_widget_last_updated_status_text_image, VISIBLE
            )

            val lastUpdatedString = "Last updated: $lastUpdatedTimeString"

            views.setImageViewBitmap(
                R.id.peek_transit_large_widget_last_updated_status_text_image, generateTextBitmap(
                    context,
                    getWidgetTextFont(currentTheme),
                    null,
                    1,
                    PeekTransitConstants.getLastSeenFontSizeForWidgetSize(widgetSize),
                    getWidgetTextColor(currentTheme, isNightMode),
                    lastUpdatedString
                )
            )

            views.setContentDescription(
                R.id.peek_transit_large_widget_last_updated_status_text_image,
                lastUpdatedString
            )

            return views
        }

        fun updateLocationCoordinates(
            context: Context,
            views: RemoteViews,
            widgetSize: String,
            isNightMode: Boolean,
            currentTheme: StopViewTheme,
            locationCoordsString: String
        ): RemoteViews {
            return if (PeekTransitConstants.DEBUG_WIDGET_LOCATION_ACCESS) {
                updateLocationCoordinatesTextImage(
                    context,
                    views,
                    widgetSize,
                    isNightMode,
                    currentTheme,
                    locationCoordsString
                )
            } else {
                resetLocationCoordinatesTextImage(views)
            }
        }

        fun resetLocationCoordinatesTextImage(
            views: RemoteViews,
        ): RemoteViews {

            views.setViewVisibility(
                R.id.peek_transit_large_widget_user_location_coordinates_layout, GONE
            )

            views.setViewVisibility(
                R.id.peek_transit_large_widget_user_location_coordinates_text_image, GONE
            )

            views.setContentDescription(
                R.id.peek_transit_large_widget_user_location_coordinates_text_image,
                "User location coordinates text image"
            )

            return views
        }

        fun updateLocationCoordinatesTextImage(
            context: Context,
            views: RemoteViews,
            widgetSize: String,
            isNightMode: Boolean,
            currentTheme: StopViewTheme,
            locationCoordsString: String
        ): RemoteViews {

            views.setViewVisibility(
                R.id.peek_transit_large_widget_user_location_coordinates_layout, VISIBLE
            )

            views.setViewVisibility(
                R.id.peek_transit_large_widget_user_location_coordinates_text_image, VISIBLE
            )

            views.setImageViewBitmap(
                R.id.peek_transit_large_widget_user_location_coordinates_text_image, generateTextBitmap(
                    context,
                    getWidgetTextFont(currentTheme),
                    null,
                    1,
                    12f,
                    getWidgetTextColor(currentTheme, isNightMode),
                    locationCoordsString
                )
            )

            views.setContentDescription(
                R.id.peek_transit_large_widget_user_location_coordinates_text_image,
                locationCoordsString
            )

            return views
        }

    }
}

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