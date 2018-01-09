package leocr.easyclockin

import android.app.NotificationManager
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.ToggleButton
import leocr.easyclockin.TimerService.LocalBinder
import org.joda.time.DateTime

class TimerActivity : AppCompatActivity() {

    private var mService: TimerService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // registering UPDATE action
        val updateFilter = IntentFilter(Constants.TIMER_UPDATE_ACTION)
        LocalBroadcastManager.getInstance(this).registerReceiver(timerReceiver, updateFilter)

        // registering NOTIFY action
        val notifyFilter = IntentFilter(Constants.TIMER_NOTIFY_ACTION)
        LocalBroadcastManager.getInstance(this).registerReceiver(timerReceiver, notifyFilter)

        intent = Intent(this, TimerService::class.java)
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStart() {
        super.onStart()
        restart()
    }

    override fun onDestroy() {
        unbindService(mConnection)
        super.onDestroy()
    }

    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as LocalBinder

            if (mService != null) {
                val timerData = getTimerData(mService!!)
                mService = binder.service
                setTimerData(mService, timerData)
            } else {
                mService = binder.service
            }

            restart()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            cancel()
        }
    }

    private fun setTimerData(mService: TimerService?, timerData: TimerService.TimerData) {
        mService!!.outTime = timerData.outTime
        mService.timeToBack = timerData.timeToBack
        mService.running = timerData.isRunning!!
    }

    private fun getTimerData(mService: TimerService): TimerService.TimerData {
        return TimerService.TimerData(false, mService.outTime, mService.timeToBack,
                mService.running)
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
        if (mService != null) {
            mService!!.countTime()
            updateToggleButton(false)
        }
    }

    private fun updateToggleButton(isOn: Boolean) {
        val toggle = findViewById<ToggleButton>(R.id.toggleTimeButton)
        toggle.isChecked = !isOn
    }

    private fun handleUpdateAction(intent: Intent?) {
        val updateBundle: Bundle? = intent?.extras
        val outTime: DateTime? = updateBundle!!.getSerializable("outTime") as DateTime?
        updateOutTimeLabel(outTime!!.toString("HH:mm"))
        val timeToBack: DateTime? = updateBundle.getSerializable("timeToBack") as DateTime?
        updateTimeToBackLabel(timeToBack!!.toString("HH:mm"))
        val status: String = updateBundle.getString("status")
        val progress: Int = updateBundle.getInt("progress")
        updateTimerProgress(progress)
        updateTimerTextView(status)
        updateToggleButton(true)
    }

    private fun handleNotifyAction(@Suppress("UNUSED_PARAMETER") intent: Intent?) {
        notifyClockIn()
        updateToggleButton(false)
    }

    private fun updateTimerTextView(label: String) {
        updateTextView(R.id.textView_timer, label)
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

    private fun updateTimerProgress(value: Int) {
        updateProgressBar(R.id.progressBar, value)
    }

    private fun updateProgressBar(id: Int, value: Int) {
        Thread(Runnable {
            val progressBar = findViewById<ProgressBar>(id)
            this@TimerActivity.runOnUiThread({
                progressBar.progress = value
            })
        }).start()
    }

    fun toggleTime(@Suppress("UNUSED_PARAMETER") view: View) {
        val on = (view as ToggleButton).isChecked

        if (!on) {
            val now = DateTime.now().toString("HH:mm:ss")
            updateTimerTextView(now)
            mService!!.countTime()
        } else {
            cancel()
        }
    }

    private fun notifyClockIn() {
        notify("Olha o Ponto!", "Você já pode bater o ponto! Bom Trabalho.")
    }

    private fun cancel() {
        mService!!.cancelTiming()
        resetInterface()
    }

    private fun resetInterface() {
        updateTimerTextView("Almoço?")
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
