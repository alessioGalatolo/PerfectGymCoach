package agdesigns.elevatefitness.navigation

import android.net.Uri

class DeepLinkMatcher(
    val requestUri: Uri
) {
    fun match(): Route? {
        if (requestUri.scheme != "elevatefitness") return null
        if (requestUri.authority == "autoopenworkout") {
            return WorkoutDestination(
                programId = 0L
            )
        }
        // try match: workout/{programId}
        if (requestUri.authority == "workout") {
            return WorkoutDestination(
                programId = requestUri.pathSegments[0].toLongOrNull() ?: 0L
            )
        }
        return null
    }
}