package agdesigns.elevatefitness.shared

import agdesigns.elevatefitness.shared.grpc.Media
import agdesigns.elevatefitness.shared.grpc.Workout
import android.util.Log
import com.google.protobuf.InvalidProtocolBufferException
import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream

object MediaSerializer : Serializer<Media.MediaPlaying> {
    override val defaultValue: Media.MediaPlaying
        get() = Media.MediaPlaying.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): Media.MediaPlaying =
        try {
            Media.MediaPlaying.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            Log.e("MediaSerializer", "Cannot read proto.", exception)
            defaultValue
        }

    override suspend fun writeTo(t: Media.MediaPlaying, output: OutputStream) {
        t.writeTo(output)
    }
}