package agdesigns.elevatefitness.utils

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.allVerticalHingeBounds
import androidx.compose.material3.adaptive.layout.HingePolicy
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.occludingVerticalHingeBounds
import androidx.compose.material3.adaptive.separatingVerticalHingeBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass

/**
 * Mimics Adaptive library PaneScaffoldDirective but also splits when in landscape
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun largeLandscapeDirective(
    windowAdaptiveInfo: WindowAdaptiveInfo,
    verticalHingePolicy: HingePolicy = HingePolicy.AvoidSeparating,
): PaneScaffoldDirective {
    val windowSizeClass = windowAdaptiveInfo.windowSizeClass
    val isLandscape = windowSizeClass.minWidthDp > windowSizeClass.minHeightDp

    val maxHorizontalPartitions: Int
    val horizontalPartitionSpacerSize: Dp
    val defaultPanePreferredWidth: Dp

    when {
        // Large/extra-large screens keep their original multi-pane behaviour
        windowSizeClass.minWidthDp == WindowSizeClass.WIDTH_DP_EXTRA_LARGE_LOWER_BOUND -> {
            maxHorizontalPartitions = 2
            horizontalPartitionSpacerSize = 24.dp
            defaultPanePreferredWidth = PaneScaffoldDirective.Default.defaultPanePreferredWidth
        }
        windowSizeClass.minWidthDp > WindowSizeClass.WIDTH_DP_EXTRA_LARGE_LOWER_BOUND -> {
            maxHorizontalPartitions = 3
            horizontalPartitionSpacerSize = 24.dp
            defaultPanePreferredWidth = PaneScaffoldDirective.Default.defaultPanePreferredWidth
        }
        // Compact/Medium in landscape → two panes
        isLandscape -> {
            maxHorizontalPartitions = 2
            horizontalPartitionSpacerSize = 16.dp
            defaultPanePreferredWidth = PaneScaffoldDirective.Default.defaultPanePreferredWidth
        }
        // Compact/Medium in portrait → single pane (original behaviour)
        else -> {
            maxHorizontalPartitions = 1
            horizontalPartitionSpacerSize = 0.dp
            defaultPanePreferredWidth = 412.dp
        }
    }

    val maxVerticalPartitions: Int
    val verticalPartitionSpacerSize: Dp

    if (
        windowAdaptiveInfo.windowPosture.isTabletop ||
        (maxHorizontalPartitions == 1 &&
                windowSizeClass.minHeightDp == WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND)
    ) {
        maxVerticalPartitions = 2
        verticalPartitionSpacerSize = 24.dp
    } else {
        maxVerticalPartitions = 1
        verticalPartitionSpacerSize = 0.dp
    }

    return PaneScaffoldDirective(
        maxHorizontalPartitions = maxHorizontalPartitions,
        horizontalPartitionSpacerSize = horizontalPartitionSpacerSize,
        maxVerticalPartitions = maxVerticalPartitions,
        verticalPartitionSpacerSize = verticalPartitionSpacerSize,
        defaultPanePreferredWidth = defaultPanePreferredWidth,
        defaultPanePreferredHeight = 420.dp,
        excludedBounds = getExcludedVerticalBounds(
            windowAdaptiveInfo.windowPosture,
            verticalHingePolicy,
        ),
        shouldAutoFocusCurrentDestination = false
    )
}

private fun getExcludedVerticalBounds(posture: Posture, hingePolicy: HingePolicy): List<Rect> {
    return when (hingePolicy) {
        HingePolicy.AvoidSeparating -> posture.separatingVerticalHingeBounds
        HingePolicy.AvoidOccluding -> posture.occludingVerticalHingeBounds
        HingePolicy.AlwaysAvoid -> posture.allVerticalHingeBounds
        else -> emptyList()
    }
}