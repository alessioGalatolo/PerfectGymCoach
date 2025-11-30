package agdesignes.elevatefitness.shared

import agdesignes.elevatefitness.shared.grpc.Workout
import android.util.Log
import com.google.protobuf.InvalidProtocolBufferException
import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream

object WorkoutDataStaticSerializer : Serializer<Workout.WorkoutStaticData> {
    override val defaultValue: Workout.WorkoutStaticData
        get() = Workout.WorkoutStaticData.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): Workout.WorkoutStaticData =
        try {
            Workout.WorkoutStaticData.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            Log.e("WorkoutDataSerializer", "Cannot read proto.", exception)
            defaultValue
        }

    override suspend fun writeTo(t: Workout.WorkoutStaticData, output: OutputStream) {
        t.writeTo(output)
    }
}

object WorkoutDataDynamicSerializer : Serializer<Workout.WorkoutDynamicData> {
    override val defaultValue: Workout.WorkoutDynamicData
        get() = Workout.WorkoutDynamicData.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): Workout.WorkoutDynamicData =
        try {
            Workout.WorkoutDynamicData.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            Log.e("WorkoutDataSerializer", "Cannot read proto.", exception)
            defaultValue
        }

    override suspend fun writeTo(t: Workout.WorkoutDynamicData, output: OutputStream) {
        t.writeTo(output)
    }
}