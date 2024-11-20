package com.example.snapmedia.server

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.snapmedia.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manages the foreground service that instantiates the SmartPilotAPI
 */
class SmartPilotService : Service() {

    private lateinit var server : SmartPilotAPI
    private val backgroundScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        server = SmartPilotAPI(applicationContext)
    }

    override fun onBind(p0: Intent?) :IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Actions.START.toString() -> start()
            Actions.STOP.toString() -> stop()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        server.stopServer()
    }

    private fun start() {
        val notification = NotificationCompat.Builder(this, "running_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Server: Active")
            .setContentText("The server is active and accepting requests")
            .build()
        startForeground(1, notification)

        startServer()
    }

    private fun stop(){
        stopSelf()
        stopServer()
    }

    private fun startServer() {
        backgroundScope.launch {
            try {
                server.startServer()
                Log.d("SmartPilotService", "Server started on port 8080")
            } catch (e: Exception) {
                Log.e("SmartPilotService", "Failed to start server")
            }
        }
    }

    private fun stopServer() {
        server.stopServer()
        Log.d("SmartPilotService", "Server stopped")
    }

    enum class Actions {
        START, STOP
    }

}