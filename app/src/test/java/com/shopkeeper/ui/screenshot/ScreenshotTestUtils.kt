package com.shopkeeper.ui.screenshot

import androidx.compose.ui.test.SemanticsNodeInteraction
import com.dropbox.differ.SimpleImageComparator
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage

/**
 * Shared Roborazzi screenshot configuration.
 *
 * Configures [SimpleImageComparator] with a 2% color distance tolerance (`maxDistance = 0.02f`)
 * and 1-pixel shift window (`hShift = 1`, `vShift = 1`).
 *
 * This mathematically eliminates subpixel anti-aliasing variations caused by differing
 * text rasterization engines across macOS (CoreText) and Linux/CI (FreeType) while strictly
 * catching any structural layout, padding, color, or content visual regressions.
 */
val ROBORAZZI_CROSS_PLATFORM_OPTIONS = RoborazziOptions(
    compareOptions = RoborazziOptions.CompareOptions(
        imageComparator = SimpleImageComparator(
            maxDistance = 0.02f,
            hShift = 1,
            vShift = 1
        )
    )
)

/**
 * Canonical screenshot capture helper for Compose UI nodes in tests.
 */
fun SemanticsNodeInteraction.captureScreenshot(
    filePath: String? = null,
    options: RoborazziOptions = ROBORAZZI_CROSS_PLATFORM_OPTIONS
) {
    if (filePath != null) {
        captureRoboImage(filePath = filePath, roborazziOptions = options)
    } else {
        captureRoboImage(roborazziOptions = options)
    }
}
