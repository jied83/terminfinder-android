package termin.android.data

import androidx.datastore.core.okio.OkioSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource
import termin.android.domain.AppSettings
import termin.android.domain.CheckRecord
import termin.android.domain.MonitorTask

@Serializable
data class StoredState(
    val tasks: List<MonitorTask> = emptyList(),
    val records: Map<Long, List<CheckRecord>> = emptyMap(),
    val knownDates: Map<Long, List<String>> = emptyMap(),
    val settings: AppSettings = AppSettings(),
)

object StoredStateSerializer : OkioSerializer<StoredState> {
    private val json = Json { ignoreUnknownKeys = true }

    override val defaultValue: StoredState = StoredState()

    override suspend fun readFrom(source: BufferedSource): StoredState =
        json.decodeFromString(StoredState.serializer(), source.readUtf8())

    override suspend fun writeTo(t: StoredState, sink: BufferedSink) {
        sink.writeUtf8(json.encodeToString(StoredState.serializer(), t))
    }
}
