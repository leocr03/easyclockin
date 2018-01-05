package leocr.easyclockin

import android.app.NotificationManager
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import io.reactivex.Observable
import leocr.easyclockin.TimerService.LocalBinder
import org.joda.time.DateTime

class TimerActivity : AppCompatActivity() {

    private var mService: TimerService? = null
    private var mBound = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // registering UPDATE action
        val updateFilter = IntentFilter(Constants.TIMER_UPDATE_ACTION)
        LocalBroadcastManager.getInstance(this).registerReceiver(timerReceiver, updateFilter)

        // registering NOTIFY action
        val notifyFilter = IntentFilter(Constants.TIMER_NOTIFY_ACTION)
        LocalBroadcastManager.getInstance(this).registerReceiver(timerReceiver, notifyFilter)
    }

//    override fun onResume() {
//        super.onResume()
//        restart()
//    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, TimerService::class.java)
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        unbindService(mConnection)
        mBound = false
    }

    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as LocalBinder
            mService = binder.service
            mBound = true
            restart()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    private val timerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Constants.TIMER_UPDATE_ACTION -> handleUpdateAction(intent)
                Constants.TIMER_NOTIFY_ACTION -> handleNotifyAction(intent)
            }
        }
    }

    private fun restart() {
        if (mService != null && mService!!.isPaused()) {
            mService!!.countTime()
        }
    }

    private fun handleUpdateAction(intent: Intent?) {
        val updateBundle: Bundle? = intent?.extras
        val outTime: DateTime? = updateBundle!!.getSerializable("outTime") as DateTime?
        updateOutTimeLabel(outTime!!.toString("HH:mm"))
        val timeToBack: DateTime? = updateBundle.getSerializable("timeToBack") as DateTime?
        updateTimeToBackLabel(timeToBack!!.toString("HH:mm"))
        val status: String = updateBundle.getString("status")
        updateTimerTextView(status)
        updateToggleButton("Pausar")
    }

    private fun handleNotifyAction(@Suppress("UNUSED_PARAMETER") intent: Intent?) {
        notifyClockIn()
    }

    private fun updateTimerTextView(label: String) {
        updateTextView(R.id.textView_timer, label)
    }

    private fun updateToggleButton(label: String) {
        updateTextView(R.id.toggleTimeButton, label)
    }

    private fun updateOutTimeLabel(label: String) {
        updateTextView(R.id.outTimeLabel, label)
//        val a = AnimationUtils.loadAnimation(this, R.anim.scale)
//        a.reset()
//        val tv = findViewById<TextView>(R.id.outTimeLabel)
//        tv.clearAnimation()
//        tv.startAnimation(a)
    }

    private fun updateTimeToBackLabel(label: String) {
        updateTextView(R.id.timeToBackLabel, label)
    }

    private fun updateTextView(id: Int, label: String) {
        Thread(Runnable {
            val textView = findViewById<TextView>(id)
            this@TimerActivity.runOnUiThread({
                textView.text = label
            })
        }).start()
    }

    fun toggleTime(@Suppress("UNUSED_PARAMETER") view: View) {
        if (!mService!!.isRunning()) {
            val now = DateTime.now().toString("HH:mm:ss")
            updateTimerTextView(now)
            Observable.just(mService!!.countTime())
                    .takeUntil { mService!!.isRunning() }
                    .subscribe()
        } else {
            mService!!.pauseTime()
            resetInterface()
        }
    }

    private fun notifyClockIn() {
        notify("Olha o Ponto!", "Você já pode bater o ponto! Bom Trabalho.")
        updateToggleButton("Começar")
    }

    private fun resetInterface() {
        updateTimerTextView("Almoço?")
        updateToggleButton("Começar")
    }

    private fun notify(title: String, message: String) {
        val context: Context = applicationContext
        val strtitle = context.getString(R.string.app_name)

        intent.putExtra("title", strtitle)
        intent.putExtra("text", message)

        val channelId = "001"
        val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setPriority(2)
                .setVibrate(longArrayOf(0, 500, 250, 500))
                .setTicker(message)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        notificationManager.notify(0, builder.build())
    }
}
