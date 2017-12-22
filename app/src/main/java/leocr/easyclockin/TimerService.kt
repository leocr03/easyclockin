package leocr.easyclockin

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import org.joda.time.DateTime
import org.joda.time.Hours
import org.joda.time.Seconds
import java.util.concurrent.TimeUnit


class TimerService : Service() {

    private val mBinder: IBinder = LocalBinder()
    private var outTime: DateTime? = null
    private var timeToBack: DateTime? = null
    private var subscription: Disposable? = null

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    inner class LocalBinder : Binder() {
        internal val service: TimerService
            get() = this@TimerService
    }

    fun countTime() {
        val now = DateTime.now()

        if (!isRunning()) {
            outTime = now
            timeToBack = now.plus(Hours.hours(1)) // (Seconds.seconds(10))
        }

        val timerData = isInTime(now)

        Observable.fromArray(timerData)
                .filter { td -> td.isInTime }
                .take(1)
                .subscribe {
                    val secondsRange: Long = Seconds.secondsBetween(DateTime.now(),
                            timeToBack).seconds.toLong() + 1
                    subscription = Observable.interval(1000L, TimeUnit.MILLISECONDS)
                            .takeWhile { occurrence ->
                                occurrence < secondsRange
                            }
                            .timeInterval()
                            .subscribe(
                                    { update(timerData, DateTime.now().toString("HH:mm:ss")) },
                                    { error -> Log.e("COUNT_TIME", "Error: " + error) },
                                    {
                                        update(timerData, "Ponto!")
                                        notifyFinish()
                                        stopSelf()
                                    }
                            )
                }
    }

    data class TimerData(val isInTime: Boolean,
                         val outTime: DateTime?,
                         val timeToBack: DateTime?)

    private fun isInTime(date: DateTime): TimerData {
        return TimerData(outTime != null && timeToBack != null &&
                date >= outTime && date < timeToBack, outTime, timeToBack)
    }

    fun isRunning(): Boolean {
        return subscription != null && !subscription!!.isDisposed
    }


    fun pauseTime(): Boolean {
        return if(isRunning()) {
            subscription!!.dispose()
            true
        } else {
            false
        }
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
        val localIntent = Intent(Constants.TIMER_UPDATE_ACTION).putExtras(bundle)
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent)
    }
}
