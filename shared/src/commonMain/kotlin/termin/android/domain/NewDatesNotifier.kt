package termin.android.domain

interface NewDatesNotifier {
    suspend fun notify(task: MonitorTask, dates: List<String>)
}
