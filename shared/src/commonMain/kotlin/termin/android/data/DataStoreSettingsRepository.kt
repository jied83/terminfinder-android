package termin.android.data

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import termin.android.domain.AppSettings
import termin.android.domain.SettingsRepository

class DataStoreSettingsRepository(private val store: DataStore<StoredState>) : SettingsRepository {

    override fun observe(): Flow<AppSettings> = store.data.map { it.settings }.distinctUntilChanged()

    override suspend fun setNotifyNewDates(enabled: Boolean) {
        store.updateData { state -> state.copy(settings = state.settings.copy(notifyNewDates = enabled)) }
    }
}
