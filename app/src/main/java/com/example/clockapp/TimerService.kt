package com.example.clockapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Handler
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class TimerService : Service() {
    private var executorService: ScheduledExecutorService? = null
    interface TimerObserver {
        fun onTimerUpdate(time: String)
    }

    var isTimerRunning = false
    private var time = 0
    private var lastTime = 0
    private val lapsList = ArrayList<String>()
    private val observers = mutableListOf<TimerObserver>()
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getServiceInstance(): TimerService {
            return this@TimerService
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun registerObserver(observer: TimerObserver) {
        observers.add(observer)
    }

    fun unregisterObserver(observer: TimerObserver) {
        observers.remove(observer)
    }

    fun notifyObservers(context: Context) {
        val currentTimeString = getCurrentTimeString()
        observers.forEach { it.onTimerUpdate(currentTimeString) }

        if (isTimerRunning) {

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    "timer_channel_id",
                    "Timer Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                notificationManager.createNotificationChannel(channel)
            }


            val notificationIntent = Intent(context, TimerScreen::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE)

            val notificationBuilder = NotificationCompat.Builder(context, "timer_channel_id")
                .setContentTitle("Таймер працює")
                .setContentText("Поточний час: $currentTimeString")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            notificationManager.notify(0, notificationBuilder.build())
        }
    }
    fun lapTapped() {
        if (isTimerRunning) {
            val seconds = time - lastTime
            val minutes = seconds / 60
            val hours = minutes / 60

            lapsList.add(String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60))
            lastTime = time
        }
    }

    fun resetTapped() {
        time = 0
        lapsList.clear()
        stopTimer()
        isTimerRunning = false
        lastTime = 0
        notifyObservers(this)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(0)
    }

    fun startStopTapped() {
        if (!isTimerRunning) {
            isTimerRunning = true
            startTimer()
        } else {
            stopTimer()
            isTimerRunning = false
        }
        notifyObservers(this)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationBuilder = NotificationCompat.Builder(this, "timer_channel_id")
            .setContentTitle("Таймер зупинено")
            .setContentText("Поточний час: ${getCurrentTimeString()}")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)

        notificationManager.notify(0, notificationBuilder.build())
    }

    private fun startTimer() {
        executorService = Executors.newScheduledThreadPool(1)
        executorService?.scheduleAtFixedRate({
            if (isTimerRunning) {
                if (time == 0) {
                    lastTime = 0
                }
                time++
                notifyObservers(this@TimerService)
            }
        }, 0, 1000, TimeUnit.MILLISECONDS)
    }

    private fun stopTimer() {
        executorService?.shutdown()
    }

    fun getCurrentTimeString(): String {
        val seconds = time
        val minutes = seconds / 60
        val hours = minutes / 60

        return String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
    }

    fun getLapsList(): ArrayList<String> {
        return lapsList
    }
}
