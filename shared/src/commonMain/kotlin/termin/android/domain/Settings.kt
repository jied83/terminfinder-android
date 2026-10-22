package termin.android.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val notifyNewDates: Boolean = true,
)

interface SettingsRepository {
    fun observe(): Flow<AppSettings>
    suspend fun setNotifyNewDates(enabled: Boolean)
}
