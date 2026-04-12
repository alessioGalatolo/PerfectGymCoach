package agdesigns.elevatefitness.shared

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.ui.graphics.vector.ImageVector

enum class SetType(val nameResKey: String, val icon: ImageVector) {
    WARMUP("set_types_warmup", Icons.Default.Thermostat),
    NORMAL("set_types_normal", Icons.AutoMirrored.Filled.TrendingFlat),
    DROP_SET("set_types_drop_set", Icons.AutoMirrored.Filled.TrendingDown),
    FAILURE("set_types_failure", Icons.Default.Whatshot),
    AWESOME("set_types_awesome", Icons.Default.AutoAwesome);

    val displayRes: Int
        get() = when (this) {
            WARMUP -> R.string.set_types_warmup
            NORMAL -> R.string.set_types_normal
            DROP_SET -> R.string.set_types_drop_set
            FAILURE -> R.string.set_types_failure
            AWESOME -> R.string.set_types_awesome
        }

    companion object {
        val visibleEntries: List<SetType>
            get() = listOf(WARMUP, NORMAL, DROP_SET, FAILURE)

        fun fromResKey(resKey: String?): SetType {
            when (resKey) {
                "set_types_warmup" -> return WARMUP
                "set_types_normal" -> return NORMAL
                "set_types_drop_set" -> return DROP_SET
                "set_types_failure" -> return FAILURE
                "set_types_awesome" -> return AWESOME
            }
            return NORMAL
        }
    }
}