package agdesigns.elevatefitness.genai


enum class ModelDownloadStatusType {
    NOT_DOWNLOADED,
    PARTIALLY_DOWNLOADED,
    IN_PROGRESS,
    SUCCEEDED,
    FAILED,
}


data class ModelDownloadStatus(
    val status: ModelDownloadStatusType,
    val totalBytes: Long = 0,
    val receivedBytes: Long = 0,
    val errorMessage: String = "",
    val bytesPerSecond: Long = 0,
    val remainingMs: Long = 0,
)