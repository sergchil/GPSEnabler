package com.chilisoft.gpsenabler

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import java.lang.ref.WeakReference

class GpsUtils(activity: AppCompatActivity) {
    private val GPS_REQUEST_CODE = 1001
    private val weakActivity = WeakReference(activity)

    private val mSettingsClient: SettingsClient = LocationServices.getSettingsClient(activity)
    private val mLocationSettingsRequest: LocationSettingsRequest
    private val locationManager: LocationManager =
        activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val mFusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
    private val locationRequest = LocationRequest.create()


    // ======= CALLBACKS =======
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            locationResult?.locations ?: return

            locationResult.locations.filterNotNull().forEach {
                locationResultCallback?.invoke(it.latitude, it.longitude)
                mFusedLocationClient?.removeLocationUpdates(this)
            }
        }
    }
    private var locationResultCallback: ((lat: Double, long: Double) -> Unit)? = null
    private val gpsEnabledListener: (enabled: Boolean) -> Unit = {
        getLocation()
    }

    init {
        locationRequest.apply {
            priority = PRIORITY_HIGH_ACCURACY
            interval = (10 * 1000).toLong()
            fastestInterval = (1 * 1000).toLong()
        }

        mLocationSettingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)
            .build()
    }


    // check GPS state, if disabled open dialog and send back the result
    fun checkGpsEnabled(listener: (lat: Double, long: Double) -> Unit) {
        locationResultCallback = listener

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            gpsEnabledListener(true)
        } else {
            weakActivity.safeGet {
                mSettingsClient
                    .checkLocationSettings(mLocationSettingsRequest)
                    .addOnSuccessListener(this, ::onEnabledInsettings)
                    .addOnFailureListener(this, ::onDisabledInSettings)
            }
        }


    }

    // enabled in settings
    private fun onEnabledInsettings(locationSettingsResponse: LocationSettingsResponse) {
        gpsEnabledListener(true)
    }

    // disabled in settings, prompt to enable with dialog, result will be sent to onActivityResult
    private fun onDisabledInSettings(e: Exception) {
        if (e !is ApiException) return

        when (e.statusCode) {
            LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> weakActivity.safeGet {
                (e as? ResolvableApiException)?.startResolutionForResult(this, GPS_REQUEST_CODE)
            }
            LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                //Location settings are inadequate, and cannot be " + "fixed here. Fix in Settings.
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun getLocation() {
        mFusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                locationResultCallback?.invoke(location.latitude, location.longitude)
            } else {
                mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
            }
        }

    }


    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GPS_REQUEST_CODE) {
                getLocation()
            }
        }
    }

    fun <T> WeakReference<T>.safeGet(body: T.() -> Unit) {
        this.get()?.body()
    }

}