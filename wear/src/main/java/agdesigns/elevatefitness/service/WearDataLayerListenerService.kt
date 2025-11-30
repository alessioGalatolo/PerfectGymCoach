package agdesigns.elevatefitness.service

import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.android.horologist.data.apphelper.DataLayerAppHelper
import com.google.android.horologist.data.apphelper.DataLayerAppHelperService
import com.google.android.horologist.datalayer.watch.WearDataLayerAppHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import javax.inject.Inject

@OptIn(ExperimentalHorologistApi::class)
public class WearDataLayerListenerService : DataLayerAppHelperService() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    public override val appHelper: DataLayerAppHelper by lazy {
        val registry = WearDataLayerRegistry.fromContext(this, serviceScope)
        WearDataLayerAppHelper(this, registry, serviceScope)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}