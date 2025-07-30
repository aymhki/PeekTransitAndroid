package com.aymanhki.peektransit.widgets

import com.aymanhki.peektransit.R



class PeekTransitSmallWidgetProvider : BaseWidgetProvider() {
    override val mainLayoutFrameResId: Int = R.id.peek_transit_small_layout_frame
    override val backgroundImageResId: Int = R.id.peek_transit_small_widget_background_image
    override val mainLayoutResId: Int = R.layout.peek_transit_small_widget_layout
    override val mainLayoutContainerResId: Int = R.id.peek_transit_small_widget_layout
    override val locationCoordinatesLayoutResId: Int = R.id.peek_transit_small_widget_user_location_coordinates_layout
    override val locationCoordinatesTextImagedResId: Int = R.id.peek_transit_small_widget_user_location_coordinates_text_image
    override val lastUpdatedLayoutResId: Int = R.id.peek_transit_small_widget_last_updated_status_layout
    override val lastUpdatedTextImageResId: Int = R.id.peek_transit_small_widget_last_updated_status_text_image
    override val initialLayoutResId: Int = R.layout.peek_transit_small_widget_initial_layout
    override val configureButtonResId: Int = R.id.configure_small_widget_btn
    override val errorLayoutResId: Int = R.layout.peek_transit_small_widget_error_layout
    override val errorTextResId: Int = R.id.peek_transit_small_widget_error_message_text_view
    override val configurationActivityClass: Class<*> = PeekTransitSmallWidgetConfigurationActivity::class.java
    override val busSchedulesComponentsResIds: Map<Int, Map<Pair<Int, Int>, Map<Int, List<Int>>>> = mapOf(
        R.id.peek_transit_small_widget_bus_stop_1_layout to mapOf(
            Pair (R.id.peek_transit_small_widget_bus_stop_1_title_layout, R.id.peek_transit_small_widget_bus_stop_1_title_text_image) to mapOf(
                R.id.peek_transit_small_widget_bus_stop_1_schedule_entry_1_layout to listOf<Int>(
                    R.id.peek_transit_small_widget_bus_stop_1_schedule_entry_1_route_number_text_image,
                    R.id.peek_transit_small_widget_bus_stop_1_schedule_entry_1_route_name_text_image,
                    R.id.peek_transit_small_widget_bus_stop_1_schedule_entry_1_status_text_image,
                    R.id.peek_transit_small_widget_bus_stop_1_schedule_entry_1_arrival_time_text_image
                ),
                R.id.peek_transit_small_widget_bus_stop_1_schedule_entry_2_layout to listOf<Int>(
                    R.id.peek_transit_small_widget_bus_stop_1_schedule_entry_2_route_number_text_image,
                    R.id.peek_transit_small_widget_bus_stop_1_schedule_entry_2_route_name_text_image,
                    R.id.peek_transit_small_widget_bus_stop_1_schedule_entry_2_status_text_image,
                    R.id.peek_transit_small_widget_bus_stop_1_schedule_entry_2_arrival_time_text_image
                )
            ),

        )
    )
    override val logTag: String = "SmallWidgetProvider"
}

class PeekTransitMediumWidgetProvider : BaseWidgetProvider() {
    override val mainLayoutFrameResId: Int = R.id.peek_transit_medium_layout_frame
    override val backgroundImageResId: Int = R.id.peek_transit_medium_widget_background_image
    override val mainLayoutResId: Int = R.layout.peek_transit_medium_widget_layout
    override val mainLayoutContainerResId: Int = R.id.peek_transit_medium_widget_layout
    override val locationCoordinatesLayoutResId: Int = R.id.peek_transit_medium_widget_user_location_coordinates_layout
    override val locationCoordinatesTextImagedResId: Int = R.id.peek_transit_medium_widget_user_location_coordinates_text_image
    override val lastUpdatedLayoutResId: Int = R.id.peek_transit_medium_widget_last_updated_status_layout
    override val lastUpdatedTextImageResId: Int = R.id.peek_transit_medium_widget_last_updated_status_text_image
    override val initialLayoutResId: Int = R.layout.peek_transit_medium_widget_initial_layout
    override val configureButtonResId: Int = R.id.configure_medium_widget_btn
    override val errorLayoutResId: Int = R.layout.peek_transit_medium_widget_error_layout
    override val errorTextResId: Int = R.id.peek_transit_medium_widget_error_message_text_view
    override val configurationActivityClass: Class<*> = PeekTransitMediumWidgetConfigurationActivity::class.java
    override val busSchedulesComponentsResIds: Map<Int, Map<Pair<Int, Int>, Map<Int, List<Int>>>> = mapOf(
        R.id.peek_transit_medium_widget_bus_stop_1_layout to mapOf(
            Pair (R.id.peek_transit_medium_widget_bus_stop_1_title_layout, R.id.peek_transit_medium_widget_bus_stop_1_title_text_image) to mapOf(
                R.id.peek_transit_medium_widget_bus_stop_1_schedule_entry_1_layout to listOf<Int>(
                    R.id.peek_transit_medium_widget_bus_stop_1_schedule_entry_1_route_number_text_image,
                    R.id.peek_transit_medium_widget_bus_stop_1_schedule_entry_1_route_name_text_image,
                    R.id.peek_transit_medium_widget_bus_stop_1_schedule_entry_1_status_text_image,
                    R.id.peek_transit_medium_widget_bus_stop_1_schedule_entry_1_arrival_time_text_image
                ),
                R.id.peek_transit_medium_widget_bus_stop_1_schedule_entry_2_layout to listOf<Int>(
                    R.id.peek_transit_medium_widget_bus_stop_1_schedule_entry_2_route_number_text_image,
                    R.id.peek_transit_medium_widget_bus_stop_1_schedule_entry_2_route_name_text_image,
                    R.id.peek_transit_medium_widget_bus_stop_1_schedule_entry_2_status_text_image,
                    R.id.peek_transit_medium_widget_bus_stop_1_schedule_entry_2_arrival_time_text_image
                )
            )
        ),
        R.id.peek_transit_medium_widget_bus_stop_2_layout to mapOf(
            Pair (R.id.peek_transit_medium_widget_bus_stop_2_title_layout, R.id.peek_transit_medium_widget_bus_stop_2_title_text_image) to mapOf(
                R.id.peek_transit_medium_widget_bus_stop_2_schedule_entry_1_layout to listOf<Int>(
                    R.id.peek_transit_medium_widget_bus_stop_2_schedule_entry_1_route_number_text_image,
                    R.id.peek_transit_medium_widget_bus_stop_2_schedule_entry_1_route_name_text_image,
                    R.id.peek_transit_medium_widget_bus_stop_2_schedule_entry_1_status_text_image,
                    R.id.peek_transit_medium_widget_bus_stop_2_schedule_entry_1_arrival_time_text_image
                ),
                R.id.peek_transit_medium_widget_bus_stop_2_schedule_entry_2_layout to listOf<Int>(
                    R.id.peek_transit_medium_widget_bus_stop_2_schedule_entry_2_route_number_text_image,
                    R.id.peek_transit_medium_widget_bus_stop_2_schedule_entry_2_route_name_text_image,
                    R.id.peek_transit_medium_widget_bus_stop_2_schedule_entry_2_status_text_image,
                    R.id.peek_transit_medium_widget_bus_stop_2_schedule_entry_2_arrival_time_text_image
                )
            )
        )
    )
    override val logTag: String = "MediumWidgetProvider"
}

class PeekTransitLargeWidgetProvider : BaseWidgetProvider() {
    override val mainLayoutFrameResId: Int = R.id.peek_transit_large_layout_frame
    override val backgroundImageResId: Int = R.id.peek_transit_large_widget_background_image
    override val mainLayoutResId: Int = R.layout.peek_transit_large_widget_layout
    override val mainLayoutContainerResId: Int = R.id.peek_transit_large_widget_layout
    override val locationCoordinatesLayoutResId: Int = R.id.peek_transit_large_widget_user_location_coordinates_layout
    override val locationCoordinatesTextImagedResId: Int = R.id.peek_transit_large_widget_user_location_coordinates_text_image
    override val lastUpdatedLayoutResId: Int = R.id.peek_transit_large_widget_last_updated_status_layout
    override val lastUpdatedTextImageResId: Int = R.id.peek_transit_large_widget_last_updated_status_text_image
    override val initialLayoutResId: Int = R.layout.peek_transit_large_widget_initial_layout
    override val configureButtonResId: Int = R.id.configure_large_widget_btn
    override val errorLayoutResId: Int = R.layout.peek_transit_large_widget_error_layout
    override val errorTextResId: Int = R.id.peek_transit_large_widget_error_message_text_view
    override val configurationActivityClass: Class<*> = PeekTransitLargeWidgetConfigurationActivity::class.java
    override val busSchedulesComponentsResIds: Map<Int, Map<Pair<Int, Int>, Map<Int, List<Int>>>> = mapOf(
        R.id.peek_transit_large_widget_bus_stop_1_layout to mapOf(
            Pair (R.id.peek_transit_large_widget_bus_stop_1_title_layout, R.id.peek_transit_large_widget_bus_stop_1_title_text_image) to mapOf(
                R.id.peek_transit_large_widget_bus_stop_1_schedule_entry_1_layout to listOf<Int>(
                    R.id.peek_transit_large_widget_bus_stop_1_schedule_entry_1_route_number_text_image,
                    R.id.peek_transit_large_widget_bus_stop_1_schedule_entry_1_route_name_text_image,
                    R.id.peek_transit_large_widget_bus_stop_1_schedule_entry_1_status_text_image,
                    R.id.peek_transit_large_widget_bus_stop_1_schedule_entry_1_arrival_time_text_image
                ),
                R.id.peek_transit_large_widget_bus_stop_1_schedule_entry_2_layout to listOf<Int>(
                    R.id.peek_transit_large_widget_bus_stop_1_schedule_entry_2_route_number_text_image,
                    R.id.peek_transit_large_widget_bus_stop_1_schedule_entry_2_route_name_text_image,
                    R.id.peek_transit_large_widget_bus_stop_1_schedule_entry_2_status_text_image,
                    R.id.peek_transit_large_widget_bus_stop_1_schedule_entry_2_arrival_time_text_image
                ),
            )
        ),
        R.id.peek_transit_large_widget_bus_stop_2_layout to mapOf(
            Pair (R.id.peek_transit_large_widget_bus_stop_2_title_layout, R.id.peek_transit_large_widget_bus_stop_2_title_text_image) to mapOf(
                R.id.peek_transit_large_widget_bus_stop_2_schedule_entry_1_layout to listOf<Int>(
                    R.id.peek_transit_large_widget_bus_stop_2_schedule_entry_1_route_number_text_image,
                    R.id.peek_transit_large_widget_bus_stop_2_schedule_entry_1_route_name_text_image,
                    R.id.peek_transit_large_widget_bus_stop_2_schedule_entry_1_status_text_image,
                    R.id.peek_transit_large_widget_bus_stop_2_schedule_entry_1_arrival_time_text_image
                ),
                R.id.peek_transit_large_widget_bus_stop_2_schedule_entry_2_layout to listOf<Int>(
                    R.id.peek_transit_large_widget_bus_stop_2_schedule_entry_2_route_number_text_image,
                    R.id.peek_transit_large_widget_bus_stop_2_schedule_entry_2_route_name_text_image,
                    R.id.peek_transit_large_widget_bus_stop_2_schedule_entry_2_status_text_image,
                    R.id.peek_transit_large_widget_bus_stop_2_schedule_entry_2_arrival_time_text_image
                )
            )
        ),
        R.id.peek_transit_large_widget_bus_stop_3_layout to mapOf(
            Pair (R.id.peek_transit_large_widget_bus_stop_3_title_layout, R.id.peek_transit_large_widget_bus_stop_3_title_text_image) to mapOf(
                R.id.peek_transit_large_widget_bus_stop_3_schedule_entry_1_layout to listOf<Int>(
                    R.id.peek_transit_large_widget_bus_stop_3_schedule_entry_1_route_number_text_image,
                    R.id.peek_transit_large_widget_bus_stop_3_schedule_entry_1_route_name_text_image,
                    R.id.peek_transit_large_widget_bus_stop_3_schedule_entry_1_status_text_image,
                    R.id.peek_transit_large_widget_bus_stop_3_schedule_entry_1_arrival_time_text_image
                ),
                R.id.peek_transit_large_widget_bus_stop_3_schedule_entry_2_layout to listOf<Int>(
                    R.id.peek_transit_large_widget_bus_stop_3_schedule_entry_2_route_number_text_image,
                    R.id.peek_transit_large_widget_bus_stop_3_schedule_entry_2_route_name_text_image,
                    R.id.peek_transit_large_widget_bus_stop_3_schedule_entry_2_status_text_image,
                    R.id.peek_transit_large_widget_bus_stop_3_schedule_entry_2_arrival_time_text_image
                )
            )
        )
    )
    override val logTag: String = "LargeWidgetProvider"
}

class PeekTransitLockScreenWidgetProvider : BaseWidgetProvider() {
    override val mainLayoutFrameResId: Int = R.id.peek_transit_lockscreen_layout_frame
    override val backgroundImageResId: Int = R.id.peek_transit_lockscreen_widget_background_image
    override val mainLayoutResId: Int = R.layout.peek_transit_lockscreen_widget_layout
    override val mainLayoutContainerResId: Int = R.id.peek_transit_lockscreen_widget_layout
    override val locationCoordinatesLayoutResId: Int = R.id.peek_transit_lockscreen_widget_user_location_coordinates_layout
    override val locationCoordinatesTextImagedResId: Int = R.id.peek_transit_lockscreen_widget_user_location_coordinates_text_image
    override val lastUpdatedLayoutResId: Int = R.id.peek_transit_lockscreen_widget_last_updated_status_layout
    override val lastUpdatedTextImageResId: Int = R.id.peek_transit_lockscreen_widget_last_updated_status_text_image
    override val initialLayoutResId: Int = R.layout.peek_transit_lockscreen_widget_initial_layout
    override val configureButtonResId: Int = R.id.configure_lockscreen_widget_btn
    override val errorLayoutResId: Int = R.layout.peek_transit_lockscreen_widget_error_layout
    override val errorTextResId: Int = R.id.peek_transit_lockscreen_widget_error_message_text_view
    override val busSchedulesComponentsResIds: Map<Int, Map<Pair<Int, Int>, Map<Int, List<Int>>>> = mapOf(
        R.id.peek_transit_lockscreen_widget_bus_stop_1_layout to mapOf(
            Pair (R.id.peek_transit_lockscreen_widget_bus_stop_1_title_layout, R.id.peek_transit_lockscreen_widget_bus_stop_1_title_text_image) to mapOf(
                R.id.peek_transit_lockscreen_widget_bus_stop_1_schedule_entry_1_layout to listOf<Int>(
                    R.id.peek_transit_lockscreen_widget_bus_stop_1_schedule_entry_1_route_number_text_image,
                    R.id.peek_transit_lockscreen_widget_bus_stop_1_schedule_entry_1_route_name_text_image,
                    R.id.peek_transit_lockscreen_widget_bus_stop_1_schedule_entry_1_status_text_image,
                    R.id.peek_transit_lockscreen_widget_bus_stop_1_schedule_entry_1_arrival_time_text_image
                ),
                R.id.peek_transit_lockscreen_widget_bus_stop_1_schedule_entry_2_layout to listOf<Int>(
                    R.id.peek_transit_lockscreen_widget_bus_stop_1_schedule_entry_2_route_number_text_image,
                    R.id.peek_transit_lockscreen_widget_bus_stop_1_schedule_entry_2_route_name_text_image,
                    R.id.peek_transit_lockscreen_widget_bus_stop_1_schedule_entry_2_status_text_image,
                    R.id.peek_transit_lockscreen_widget_bus_stop_1_schedule_entry_2_arrival_time_text_image
                )
            )
        )
    )
    override val configurationActivityClass: Class<*> = PeekTransitLockScreenWidgetConfigurationActivity::class.java
    override val logTag: String = "LockScreenWidgetProvider"
}

