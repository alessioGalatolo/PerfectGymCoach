package agdesigns.elevatefitness.ui.common


data class SharedElementKey(
    val destination: String,
    val type: SharedElementType,
    val idInt: Int = 0,
    val idLong: Long = 0L,
    val idString: String = ""
)

enum class SharedElementType {
    Bounds,
    Image,
    Title,
    Description,
}

object SharedElementGeneralKeys {
    const val FAP_TO_VIEW = "fab2view"
}