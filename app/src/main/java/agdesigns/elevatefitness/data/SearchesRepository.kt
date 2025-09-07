package agdesigns.elevatefitness.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.InputStream
import java.io.OutputStream

object RecentSearchesSerializer : Serializer<RecentSearches> {
    override val defaultValue: RecentSearches = RecentSearches.getDefaultInstance()

    override suspend fun readFrom(input: InputStream) =
        RecentSearches.parseFrom(input)

    override suspend fun writeTo(t: RecentSearches, output: OutputStream) =
        t.writeTo(output)
}

val Context.recentSearchesDataStore by dataStore(
    fileName = "recent_searches.pb",
    serializer = RecentSearchesSerializer
)

class SearchesRepository(
    @ApplicationContext private val context: Context,
    private val maxItems: Int = 8
) {
    private val dataStore: DataStore<RecentSearches> = context.recentSearchesDataStore
    val recent: Flow<List<String>> = dataStore.data.map { it.queriesList }

    suspend fun push(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        dataStore.updateData { current ->
            val deduped = buildList {
                add(trimmed)
                current.queriesList.forEach { if (!it.equals(trimmed, true)) add(it) }
            }.take(maxItems)
            current.toBuilder().clearQueries().addAllQueries(deduped).build()
        }
    }

    suspend fun clear() {
        dataStore.updateData { it.toBuilder().clearQueries().build() }
    }
}
