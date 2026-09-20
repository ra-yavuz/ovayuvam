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
import tr.ovayuva.ovayuvam.domain.RevealCell
import tr.ovayuva.ovayuvam.domain.WorldCell
import tr.ovayuva.ovayuvam.storage.VisitRepository
import kotlin.math.ceil

class LocationTrailService : Service() {
    private lateinit var locationManager: LocationManager
    private lateinit var repository: VisitRepository
    private lateinit var trackingState: TrackingState
    private var listener: LocationListener? = null
    private var lastAcceptedLocation: Location? = null

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
                acceptLocation(location)
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
                    1_000L,
                    1f,
                    next,
                    Looper.getMainLooper(),
                )
                registered = true
            }
        }
        if (registered) {
            listener = next
            activeProviders
                .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
                .filter { it.isFreshEnoughForSeed(System.currentTimeMillis()) }
                .maxByOrNull { it.time }
                ?.let(next::onLocationChanged)
        } else {
            trackingState.setTracking(false)
            stopSelf()
        }
    }

    private fun stopTracking() {
        listener?.let(locationManager::removeUpdates)
        listener = null
        lastAcceptedLocation = null
        trackingState.setTracking(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun acceptLocation(location: Location) {
        val now = System.currentTimeMillis()
        if (!location.isUsable(now)) return
        val previous = lastAcceptedLocation
        recordTrail(previous, location, now)
        lastAcceptedLocation = Location(location)
        val cell = location.toWorldCell()
        trackingState.setCurrentLocation(location.latitude, location.longitude, cell)
    }

    private fun Location.isUsable(nowMs: Long): Boolean {
        if (hasAccuracy() && accuracy > MaxAcceptedAccuracyMeters) return false
        if (time > 0L && nowMs - time > MaxAcceptedAgeMs) return false
        val previous = lastAcceptedLocation ?: return true
        val elapsedSeconds = elapsedSecondsSince(previous)
        if (elapsedSeconds <= 0.0) return true
        val distance = previous.distanceTo(this)
        if (distance < JumpFilterDistanceMeters) return true
        val speedMetersPerSecond = distance / elapsedSeconds
        val accuracySlack = previous.safeAccuracy() + safeAccuracy() + JumpAccuracySlackMeters
        return speedMetersPerSecond <= MaxAcceptedJumpSpeedMetersPerSecond || distance <= accuracySlack
    }

    private fun Location.isFreshEnoughForSeed(nowMs: Long): Boolean =
        time > 0L && nowMs - time <= LastKnownMaxAgeMs && (!hasAccuracy() || accuracy <= MaxAcceptedAccuracyMeters)

    private fun recordTrail(previous: Location?, current: Location, seenMs: Long) {
        if (previous == null || previous.distanceTo(current) > MaxInterpolatedTrailMeters) {
            repository.recordVisitArea(current.toWorldCell(), seenMs, radiusCells = 0)
            repository.recordRevealArea(current.toRevealCell(), RevealCell.Kind.Core, seenMs, radiusCells = 1)
            return
        }
        val distance = previous.distanceTo(current)
        val steps = ceil(distance / TrailStepMeters).toInt().coerceIn(1, MaxInterpolatedPoints)
        val visited = LinkedHashSet<WorldCell>()
        val precise = LinkedHashSet<WorldCell>()
        for (index in 0..steps) {
            val fraction = index.toDouble() / steps
            val latitude = previous.latitude + (current.latitude - previous.latitude) * fraction
            val longitude = previous.longitude + (current.longitude - previous.longitude) * fraction
            visited += WorldCell.fromLocation(latitude, longitude)
            precise += WorldCell.fromLocation(latitude, longitude, WorldCell.RevealCellSizeMeters)
        }
        visited.forEach { cell ->
            repository.recordVisitArea(cell, seenMs, radiusCells = 0)
        }
        precise.forEach { cell ->
            repository.recordRevealArea(cell, RevealCell.Kind.Core, seenMs, radiusCells = 1)
        }
    }

    private fun Location.toWorldCell(): WorldCell = WorldCell.fromLocation(latitude, longitude)

    private fun Location.toRevealCell(): WorldCell =
        WorldCell.fromLocation(latitude, longitude, WorldCell.RevealCellSizeMeters)

    private fun Location.safeAccuracy(): Float = if (hasAccuracy()) accuracy else DefaultAccuracyMeters

    private fun Location.elapsedSecondsSince(previous: Location): Double {
        val elapsedNanos = elapsedRealtimeNanos - previous.elapsedRealtimeNanos
        if (elapsedNanos > 0L) return elapsedNanos / 1_000_000_000.0
        val elapsedMillis = time - previous.time
        return elapsedMillis / 1_000.0
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
        private const val MaxAcceptedAccuracyMeters = 75f
        private const val DefaultAccuracyMeters = 50f
        private const val MaxAcceptedAgeMs = 30_000L
        private const val LastKnownMaxAgeMs = 120_000L
        private const val JumpFilterDistanceMeters = 120f
        private const val JumpAccuracySlackMeters = 90f
        private const val MaxAcceptedJumpSpeedMetersPerSecond = 45f
        private const val TrailStepMeters = 10f
        private const val MaxInterpolatedTrailMeters = 2_000f
        private const val MaxInterpolatedPoints = 96

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, LocationTrailService::class.java),
            )
        }
    }
}
