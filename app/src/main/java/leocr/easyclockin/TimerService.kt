package leocr.easyclockin

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.preference.PreferenceManager
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import org.joda.time.DateTime
import org.joda.time.Seconds
import java.util.concurrent.TimeUnit

class TimerService : Service() {

    private val mBinder: IBinder = LocalBinder()
    private var subscription: Disposable? = null
    var outTime: DateTime? = null
    var timeToBack: DateTime? = null
    var running = false
    var intervalTime: Int = BuildConfig.INTERVAL_TIME_IN_SECONDS.toString().toInt()

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    inner class LocalBinder : Binder() {
        internal val service: TimerService
            get() = this@TimerService
    }

    fun countTime() {
        val now = DateTime.now()

        if (isCanceled()) {
            loadPreferences()
            outTime = now
            timeToBack = now.plus(Seconds.seconds(intervalTime))
        }

        running = true

        val timerData = isInTime(now)

        Observable.fromArray(timerData)
                .filter { td -> td.isInTime }
                .take(1)
                .subscribe {
                    val secondsRange: Long = Seconds.secondsBetween(DateTime.now(),
                            timeToBack).seconds.toLong() + 1
                    if (subscription != null) {
                        cancelTiming()
                        running = true
                    }

                    subscription = Observable.interval(1000L, TimeUnit.MILLISECONDS)
                            .takeWhile { occurrence -> occurrence < secondsRange }
                            .doOnNext { timerData.progress = calculateProgress(DateTime.now()) }
                            .timeInterval()
                            .subscribe(
                                    { update(timerData, DateTime.now().toString("HH:mm:ss")) },
                                    { error -> Log.e("COUNT_TIME", "Error: " + error) },
                                    { finish(timerData) }
                            )
                }
    }

    private fun calculateProgress(now: DateTime): Int {
        val previousSeconds = Seconds.secondsBetween(outTime, now).seconds
        val totalRange = Seconds.secondsBetween(outTime, timeToBack).seconds
        return ((previousSeconds.toFloat() / totalRange.toFloat()) * 100).toInt()
    }

    private fun finish(timerData: TimerData) {
        update(timerData, "Ponto!")
        Observable.just(stopSelf())
                .takeUntil { isRunning() }
        notifyFinish()
        outTime = null
        timeToBack = null
        running = false
    }

    data class TimerData(val isInTime: Boolean,
                         val outTime: DateTime?,
                         val timeToBack: DateTime?,
                         val isRunning: Boolean? = null,
                         var progress: Int = 0)

    private fun isInTime(date: DateTime): TimerData {
        return TimerData(outTime != null && timeToBack != null &&
                date >= outTime && date < timeToBack, outTime, timeToBack)
    }

    // for verification by false, please use isCanceled
    private fun isRunning(): Boolean {
        return running && subscription != null && !subscription!!.isDisposed
    }

    // for verification by false, please use isRunning
    private fun isCanceled(): Boolean {
        return !running && (subscription == null || subscription!!.isDisposed)
    }
//
//    // for verification by false, please use isRunning
//    fun isPaused(): Boolean {
//        return isRunning && (subscription == null || subscription!!.isDisposed)
//    }

    private fun pauseTiming() {
        subscription!!.dispose()
    }

    fun cancelTiming() {
        pauseTiming()
        running = false
    }

    private fun notifyFinish() {
        val localIntent = Intent(Constants.TIMER_NOTIFY_ACTION)
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent)
    }

    private fun update(timerData: TimerData, status: String) {
        val bundle = Bundle()
        bundle.putString("status", status)
        bundle.putBoolean("isInTime", timerData.isInTime)
        bundle.putSerializable("outTime", timerData.outTime)
        bundle.putSerializable("timeToBack", timerData.timeToBack)
        bundle.putSerializable("progress", timerData.progress)
        val localIntent = Intent(Constants.TIMER_UPDATE_ACTION).putExtras(bundle)
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent)
    }

    private fun loadPreferences() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        intervalTime = preferences.getString("pref_key_timer_period_seconds", "3600").toInt()
    }
}
