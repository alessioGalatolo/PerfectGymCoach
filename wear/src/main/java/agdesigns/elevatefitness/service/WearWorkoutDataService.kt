package agdesigns.elevatefitness.service

import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.android.horologist.data.WearDataService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WearWorkoutDataService: WearDataService() {
    @OptIn(ExperimentalHorologistApi::class)
    @Inject
    override lateinit var registry: WearDataLayerRegistry
}