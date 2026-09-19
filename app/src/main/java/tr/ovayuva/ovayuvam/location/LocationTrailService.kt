package tr.ovayuva.ovayuvam.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import tr.ovayuva.ovayuvam.MainActivity
import tr.ovayuva.ovayuvam.R
import tr.ovayuva.ovayuvam.domain.WorldCell
import tr.ovayuva.ovayuvam.storage.VisitRepository

class LocationTrailService : Service() {
    private lateinit var locationManager: LocationManager
    private lateinit var repository: VisitRepository
    private lateinit var trackingState: TrackingState
    private var listener: LocationListener? = null

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LocationManager::class.java)
        repository = VisitRepository(this)
        trackingState = TrackingState(this)
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!canTrackLocation()) {
            trackingState.setTracking(false)
            stopSelf()
            return START_NOT_STICKY
        }
        startAsForegroundLocationService()
        startTracking()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopTracking()
        super.onDestroy()
    }

    @SuppressLint("MissingPermission")
    private fun startTracking() {
        if (!canTrackLocation()) {
            trackingState.setTracking(false)
            stopSelf()
            return
        }
        trackingState.setTracking(true)
        if (listener != null) return
        val next = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val cell = WorldCell.fromLocation(location.latitude, location.longitude)
                repository.record(cell, System.currentTimeMillis())
                trackingState.setCurrentCell(cell)
            }

            override fun onProviderDisabled(provider: String) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            @Deprecated("Legacy Android callback")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }
        val activeProviders = providersForGrantedPermissions()
        var registered = false
        activeProviders.forEach { provider ->
            runCatching {
                locationManager.requestLocationUpdates(
                    provider,
                    10_000L,
                    20f,
                    next,
                    Looper.getMainLooper(),
                )
                registered = true
            }
        }
        if (registered) {
            listener = next
        } else {
            trackingState.setTracking(false)
            stopSelf()
        }
    }

    private fun stopTracking() {
        listener?.let(locationManager::removeUpdates)
        listener = null
        trackingState.setTracking(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun hasFineLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun canTrackLocation(): Boolean = hasLocationPermission() && isDeviceLocationEnabled()

    private fun isDeviceLocationEnabled(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            providerEnabled(LocationManager.GPS_PROVIDER) ||
                providerEnabled(LocationManager.NETWORK_PROVIDER)
        }

    private fun providersForGrantedPermissions(): List<String> = buildList {
        if (hasFineLocationPermission() && providerEnabled(LocationManager.GPS_PROVIDER)) {
            add(LocationManager.GPS_PROVIDER)
        }
        if (providerEnabled(LocationManager.NETWORK_PROVIDER)) {
            add(LocationManager.NETWORK_PROVIDER)
        }
        if (providerEnabled(LocationManager.PASSIVE_PROVIDER)) {
            add(LocationManager.PASSIVE_PROVIDER)
        }
    }

    private fun providerEnabled(provider: String): Boolean =
        runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false)

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.tracking_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun startAsForegroundLocationService() {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else {
                0
            },
        )
    }

    private fun notification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_map)
            .setContentTitle(getString(R.string.tracking_notification_title))
            .setContentText(getString(R.string.tracking_notification_text))
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "location-tracking"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, LocationTrailService::class.java),
            )
        }
    }
}
