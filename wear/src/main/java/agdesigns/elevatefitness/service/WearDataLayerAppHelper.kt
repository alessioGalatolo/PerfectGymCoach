package agdesigns.elevatefitness.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.CheckResult
import androidx.concurrent.futures.await
import androidx.datastore.core.DataStore
import com.google.android.gms.wearable.Node
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.AppHelperResultCode
import com.google.android.horologist.data.SurfacesInfo
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.android.horologist.data.apphelper.DataLayerAppHelper
import com.google.android.horologist.data.apphelper.SurfacesInfoSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import androidx.core.net.toUri
import androidx.wear.remote.interactions.RemoteActivityHelper
import androidx.wear.tiles.TileService
import agdesigns.elevatefitness.shared.toProtoTimestamp
import com.google.android.gms.tasks.Tasks
import com.google.android.horologist.data.UsageStatus
import com.google.android.horologist.data.activityLaunched
import com.google.android.horologist.data.companionConfig
import com.google.android.horologist.data.copy
import com.google.android.horologist.data.launchRequest
import com.google.android.horologist.data.tileInfo
import com.google.android.horologist.data.usageInfo
import com.google.protobuf.Timestamp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.time.ZonedDateTime

@ExperimentalHorologistApi
public class WearDataLayerAppHelper internal constructor(
    context: Context,
    registry: WearDataLayerRegistry,
    private val appStoreUri: String?,
    private val scope: CoroutineScope,
    surfacesInfoDataStoreFn: () -> DataStore<SurfacesInfo>,
) : DataLayerAppHelper(context, registry) {
    public constructor(
        context: Context,
        registry: WearDataLayerRegistry,
        scope: CoroutineScope,
        appStoreUri: String? = null,
    ) : this(context, registry, appStoreUri, scope, {
        registry.protoDataStore(
            path = SURFACE_INFO_PATH,
            coroutineScope = scope,
            serializer = SurfacesInfoSerializer,
        )
    })

    private val surfacesInfoDataStore by lazy { surfacesInfoDataStoreFn() }

    /**
     * Return the [SurfacesInfo] of this node.
     */
    public val surfacesInfo: Flow<SurfacesInfo> by lazy { surfacesInfoDataStore.data }

    override val connectedAndInstalledNodes: Flow<Set<Node>>
        get() = connectedAndInstalledNodes(PHONE_CAPABILITY)

    override suspend fun installOnNode(nodeId: String): AppHelperResultCode {
        checkIsForegroundOrThrow()
        // we are always on android
        val intent = Intent(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setData(playStoreUri.toUri())
//        val intent = when (PhoneTypeHelper.getPhoneDeviceType(context)) {
//            PhoneTypeHelper.DEVICE_TYPE_ANDROID -> {
//                Intent(Intent.ACTION_VIEW)
//                    .addCategory(Intent.CATEGORY_BROWSABLE)
//                    .setData(playStoreUri.toUri())
//            }
//
//            PhoneTypeHelper.DEVICE_TYPE_IOS -> {
//                requireNotNull(appStoreUri) {
//                    "The uri for the app store should be provided when using this function with " +
//                            "an iOS device."
//                }
//
//                Intent(Intent.ACTION_VIEW)
//                    .addCategory(Intent.CATEGORY_BROWSABLE)
//                    .setData(Uri.parse(appStoreUri))
//            }
//
//            else -> {
//                return AppHelperResultCode.APP_HELPER_RESULT_CANNOT_DETERMINE_DEVICE_TYPE
//            }
//        }

        val availabilityStatus = remoteActivityHelper.availabilityStatus.first()

        // As per documentation, calls should be made when status is either STATUS_AVAILABLE
        // or STATUS_UNKNOWN.
        when (availabilityStatus) {
            RemoteActivityHelper.STATUS_UNAVAILABLE -> {
                return AppHelperResultCode.APP_HELPER_RESULT_UNAVAILABLE
            }

            RemoteActivityHelper.STATUS_TEMPORARILY_UNAVAILABLE -> {
                return AppHelperResultCode.APP_HELPER_RESULT_TEMPORARILY_UNAVAILABLE
            }
        }

        try {
            remoteActivityHelper.startRemoteActivity(intent, nodeId).await()
        } catch (e: RemoteActivityHelper.RemoteIntentException) {
            return AppHelperResultCode.APP_HELPER_RESULT_ERROR_STARTING_ACTIVITY
        }
        return AppHelperResultCode.APP_HELPER_RESULT_SUCCESS
    }

    @CheckResult
    override suspend fun startCompanion(nodeId: String): AppHelperResultCode {
        checkIsForegroundOrThrow()
        val localNode = registry.nodeClient.localNode.await()
        val request = launchRequest {
            companion = companionConfig {
                sourceNode = localNode.id
            }
        }
        return sendRequestWithTimeout(nodeId, LAUNCH_APP, request.toByteArray())
    }

    /**
     * Marks that the main activity has been launched at least once.
     */
    public suspend fun markActivityLaunchedOnce() {
        surfacesInfoDataStore.updateData { info ->
            info.copy {
                val launchTimestamp = ZonedDateTime.now().toProtoTimestamp()
                if (usageInfo.usageStatus == UsageStatus.USAGE_STATUS_UNSPECIFIED) {
                    usageInfo = usageInfo {
                        usageStatus = UsageStatus.USAGE_STATUS_LAUNCHED_ONCE
                        timestamp = launchTimestamp
                    }
                }

                // Temporarily support previous location for this information in [ActivityLaunched]
                // Remove in the longer term
                if (!activityLaunched.activityLaunchedOnce) {
                    activityLaunched = activityLaunched {
                        activityLaunchedOnce = true
                        timestamp = launchTimestamp
                    }
                }
            }
        }
    }

    /**
     * Marks that the necessary setup steps have been completed in the app such that it is ready for
     * use. Typically this should be called when any pairing/login has been completed. If used for
     * prompting login, it should also be called during startup if login happened before
     */
    public suspend fun markSetupComplete() {
        surfacesInfoDataStore.updateData { info ->
            info.copy {
                if (usageInfo.usageStatus != UsageStatus.USAGE_STATUS_SETUP_COMPLETE) {
                    usageInfo = usageInfo {
                        usageStatus = UsageStatus.USAGE_STATUS_SETUP_COMPLETE
                        timestamp = ZonedDateTime.now().toProtoTimestamp()
                    }
                }
            }
        }
    }

    /**
     * Marks that the app is no longer considered in a fully setup state. For example, the user has
     * logged out. This will roll the state back to the app having been used once - if the setup
     * had previously been completed, but will have no effect if this is not the case.
     */
    public suspend fun markSetupNoLongerComplete() {
        surfacesInfoDataStore.updateData { info ->
            info.copy {
                if (usageInfo.usageStatus == UsageStatus.USAGE_STATUS_SETUP_COMPLETE) {
                    usageInfo = usageInfo {
                        usageStatus = UsageStatus.USAGE_STATUS_LAUNCHED_ONCE
                        timestamp = ZonedDateTime.now().toProtoTimestamp()
                    }
                }
            }
        }
    }
}