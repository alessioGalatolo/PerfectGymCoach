package agdesigns.elevatefitness.data

import agdesigns.elevatefitness.data.datastore.PermissionStateDataStore
import agdesigns.elevatefitness.data.datastore.WorkoutDataStore
import agdesigns.elevatefitness.service.WorkoutService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearRepository @Inject constructor(
    private val workoutDataStore: WorkoutDataStore,
    val permissionStateDataStore: PermissionStateDataStore,
    @ApplicationContext private val context: Context
) {
    // The remaining variables are related to the binding/monitoring/interacting with the
    // service that gathers all the data to calculate walking points.
    private var foregroundOnlyServiceBound = false
    private val _service = MutableStateFlow<WorkoutService?>(null)
    val service: StateFlow<WorkoutService?> = _service

    var foregroundOnlyWalkingWorkoutService: WorkoutService? = null
        private set

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as WorkoutService.LocalBinder
            foregroundOnlyWalkingWorkoutService = binder.workoutService
            foregroundOnlyServiceBound = true
            _service.value = foregroundOnlyWalkingWorkoutService
        }
        override fun onServiceDisconnected(name: ComponentName) {
            foregroundOnlyWalkingWorkoutService = null
            foregroundOnlyServiceBound = false
            _service.value = null
        }
    }

    suspend fun startWorkout() {
        service.filterNotNull().first().startWorkout()
    }

    fun stopWorkout() {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            service.filterNotNull().first().stopWorkout()
        }
    }
    fun bindForegroundOnlyService() {
        val intent = Intent(context, WorkoutService::class.java)
        // If it's a foreground service that must actually run, start it as well:
        // ContextCompat.startForegroundService(context, intent)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun stopForegroundOnlyService() {
        if (foregroundOnlyServiceBound) {
            context.unbindService(connection)
            foregroundOnlyServiceBound = false
            _service.value = null
        }
    }

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: WearRepository? = null

        fun getInstance(workoutDataStore: WorkoutDataStore, permissionStateDataStore: PermissionStateDataStore, context: Context) =
            instance ?: synchronized(this) {
                instance ?: WearRepository(workoutDataStore, permissionStateDataStore, context).also { instance = it }
            }
    }
}