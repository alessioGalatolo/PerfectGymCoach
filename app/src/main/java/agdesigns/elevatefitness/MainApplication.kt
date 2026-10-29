package agdesigns.elevatefitness

import agdesigns.elevatefitness.data.PreferenceRepository
import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MainApplication: Application() {
    @Inject lateinit var preferences: PreferenceRepository
    @Inject lateinit var scope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        // Crashlytics persists this flag itself, keeping it in sync also covers preference imports
        scope.launch {
            preferences.getCrashReporting().distinctUntilChanged().collect {
                FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = it
            }
        }
    }
}
