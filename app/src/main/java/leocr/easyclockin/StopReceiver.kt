package leocr.easyclockin

import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context

class StopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val service = Intent(context, TimerService::class.java)
        service.action = Constants.STOP_ACTION
        context.startService(service)
    }
}