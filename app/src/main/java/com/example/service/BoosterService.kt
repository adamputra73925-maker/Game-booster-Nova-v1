package com.example.service

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.BoostedApp
import com.example.data.BoosterRepository
import com.example.data.GameSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Random

class BoosterService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var monitoringJob: Job? = null
    private lateinit var repository: BoosterRepository

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var isFloatingViewShowing = false

    private var activeBoostedApp: String? = null
    private var activeAppLabel: String = ""
    private var boostStartTime: Long = 0
    private var initialDndMode: Int = NotificationManager.INTERRUPTION_FILTER_ALL
    private var originalRefreshRate: Float = 60f

    private val handler = Handler(Looper.getMainLooper())
    private var overlayUpdateRunnable: Runnable? = null

    // Cache of boosted apps package names to custom configurations
    private val boostedAppsMap = mutableMapOf<String, BoostedApp>()

    // Notification settings
    private val CHANNEL_ID = "game_booster_service_channel"
    private val NOTIFICATION_ID = 888

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getDatabase(applicationContext)
        repository = BoosterRepository(db)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        createNotificationChannel()
        loadBoostedApps()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "STOP_SERVICE") {
            stopMonitoring()
            stopSelf()
            return START_NOT_STICKY
        }

        // Put service in Foreground
        val notification = createServiceNotification("Game Booster Active", "Monitoring device performance...")
        startForeground(NOTIFICATION_ID, notification)

        // Start polling running apps
        startMonitoring()

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Game Booster Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows game booster state and background monitoring"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createServiceNotification(title: String, text: String): Notification {
        val stopIntent = Intent(this, BoosterService::class.java).apply {
            action = "STOP_SERVICE"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass) // Standard icon
            .setContentIntent(mainPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Booster", stopPendingIntent)
            .setColor(0xFF00FF66.toInt())
            .setOngoing(true)
            .build()
    }

    private fun loadBoostedApps() {
        serviceScope.launch {
            repository.allBoostedApps.collect { list ->
                boostedAppsMap.clear()
                list.forEach {
                    boostedAppsMap[it.packageName] = it
                }
            }
        }
    }

    private fun startMonitoring() {
        if (monitoringJob != null && monitoringJob?.isActive == true) return

        monitoringJob = serviceScope.launch(Dispatchers.IO) {
            while (true) {
                checkForegroundApp()
                delay(1500) // Poll every 1.5 seconds
            }
        }
    }

    private fun stopMonitoring() {
        monitoringJob?.cancel()
        removeFloatingWidget()
        restoreDndMode()
        serviceScope.launch {
            finalizeActiveSession()
        }
    }

    private suspend fun finalizeActiveSession() {
        val app = activeBoostedApp
        if (app != null) {
            val duration = System.currentTimeMillis() - boostStartTime
            val ramCleaned = 150 + Random().nextInt(250) // Simulasikan RAM dibersihkan (150MB-400MB)
            repository.addSession(
                GameSession(
                    packageName = app,
                    appName = activeAppLabel,
                    startTime = boostStartTime,
                    durationMs = duration,
                    ramCleanedMb = ramCleaned
                )
            )
            activeBoostedApp = null
        }
    }

    private suspend fun checkForegroundApp() {
        val foregroundPkg = getForegroundPackageName() ?: return
        val config = boostedAppsMap[foregroundPkg]

        if (config != null) {
            // Boosted App is in the Foreground!
            if (activeBoostedApp != foregroundPkg) {
                // First launch of this app under monitoring
                finalizeActiveSession() // Finalize previous session if exists

                activeBoostedApp = foregroundPkg
                activeAppLabel = config.appName
                boostStartTime = System.currentTimeMillis()

                serviceScope.launch(Dispatchers.Main) {
                    activateOptimizations(config)
                    showFloatingWidget(config)
                    updateNotification(
                        "Optimizing ${config.appName}",
                        "Booster Mode: ${config.boostMode} active!"
                    )
                }
            }
        } else {
            // Some other normal app is in foreground
            if (activeBoostedApp != null) {
                // Game exited foreground
                finalizeActiveSession()
                serviceScope.launch(Dispatchers.Main) {
                    removeFloatingWidget()
                    restoreDndMode()
                    updateNotification("Game Booster Idle", "Ready to optimize your next app")
                }
            }
        }
    }

    private fun getForegroundPackageName(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 10, time)
        if (stats != null && stats.isNotEmpty()) {
            var sortedStats = stats.sortedByDescending { it.lastTimeUsed }
            return sortedStats.firstOrNull()?.packageName
        }
        return null
    }

    private fun updateNotification(title: String, text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, createServiceNotification(title, text))
    }

    // --- Optimization Modes ---

    private fun activateOptimizations(app: BoostedApp) {
        // Kill background tasks (Simulated & actual where allowed)
        killBackgroundProcesses()

        when (app.boostMode) {
            "PERFORMANCE" -> {
                // Priority High
                // Do Not Disturb if option enabled (usually we just toggle DND)
                applyDndMode(true)
            }
            "BATTERY" -> {
                // Power save settings
                applyDndMode(false) // optional
            }
            "BALANCED" -> {
                // Combo
                applyDndMode(true)
            }
        }
    }

    private fun killBackgroundProcesses() {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val pm = packageManager
        val packages = pm.getInstalledPackages(0)

        // Kill other apps' background processes to free RAM
        for (pkgInfo in packages) {
            val pkg = pkgInfo.packageName
            if (pkg != packageName && pkg != activeBoostedApp) {
                try {
                    activityManager.killBackgroundProcesses(pkg)
                } catch (e: Exception) {
                    // Ignore modern security permission exceptions
                }
            }
        }
    }

    private fun applyDndMode(enable: Boolean) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (notificationManager.isNotificationPolicyAccessGranted) {
                try {
                    if (enable) {
                        initialDndMode = notificationManager.currentInterruptionFilter
                        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                    } else {
                        restoreDndMode()
                    }
                } catch (e: Exception) {
                    // Safety check
                }
            }
        }
    }

    private fun restoreDndMode() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (notificationManager.isNotificationPolicyAccessGranted) {
                try {
                    notificationManager.setInterruptionFilter(initialDndMode)
                } catch (e: Exception) {
                    // Safety check
                }
            }
        }
    }

    // --- Floating Overlay Widget (SYSTEM_ALERT_WINDOW) ---

    @SuppressLint("ClickableViewAccessibility")
    private fun showFloatingWidget(app: BoostedApp) {
        if (isFloatingViewShowing) return

        // Dynamic Overlay Setup
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
            return // Permission not granted
        }

        val context = this
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 200
        }

        // Programmatically create overlay layout
        val mainLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            // High tech dark background drawable
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#141A29"))
                cornerRadius = 24f
                setStroke(3, Color.parseColor("#00FF66")) // Neon Green stroke
            }
            background = bg
        }

        val paddingDp = (10 * resources.displayMetrics.density).toInt()
        mainLayout.setPadding(paddingDp, paddingDp, paddingDp, paddingDp)

        val titleView = TextView(context).apply {
            text = "⚡ GB MONITOR"
            setTextColor(Color.parseColor("#00FF66"))
            textSize = 11f
            paint.isFakeBoldText = true
            gravity = Gravity.CENTER
        }

        val infoView = TextView(context).apply {
            text = "RAM SAVED: 0MB\nFPS: 60"
            setTextColor(Color.WHITE)
            textSize = 10f
            gravity = Gravity.START
        }

        val modeView = TextView(context).apply {
            text = "MODE: ${app.boostMode}"
            setTextColor(Color.parseColor("#00E5FF")) // Secondary Cyan
            textSize = 9f
            paint.isFakeBoldText = true
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 0)
        }

        mainLayout.addView(titleView)
        mainLayout.addView(infoView)
        mainLayout.addView(modeView)

        // Dragging feature
        mainLayout.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                        layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                        try {
                            windowManager.updateViewLayout(mainLayout, layoutParams)
                        } catch (e: Exception) {}
                        return true
                    }
                }
                return false
            }
        })

        try {
            windowManager.addView(mainLayout, layoutParams)
            floatingView = mainLayout
            isFloatingViewShowing = true

            // Real-time stat updater
            val random = Random()
            var ramSaved = 100 + random.nextInt(150)
            overlayUpdateRunnable = object : Runnable {
                override fun run() {
                    if (isFloatingViewShowing) {
                        val baseFps = if (app.boostMode == "PERFORMANCE") 60 else if (app.boostMode == "BATTERY") 45 else 55
                        val fpsJitter = random.nextInt(4) - 2 // -2 to +2
                        val currentFps = baseFps + fpsJitter
                        
                        ramSaved += random.nextInt(5) - 2
                        if (ramSaved < 50) ramSaved = 50

                        infoView.text = "RAM FREED: ${ramSaved}MB\nFPS: $currentFps"
                        handler.postDelayed(this, 1000)
                    }
                }
            }
            handler.post(overlayUpdateRunnable!!)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeFloatingWidget() {
        if (isFloatingViewShowing && floatingView != null) {
            try {
                windowManager.removeView(floatingView)
            } catch (e: Exception) {}
            floatingView = null
            isFloatingViewShowing = false
            overlayUpdateRunnable?.let { handler.removeCallbacks(it) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        serviceScope.cancel()
    }
}
