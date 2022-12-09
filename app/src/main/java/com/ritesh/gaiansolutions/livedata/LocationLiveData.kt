package com.ritesh.gaiansolutions.livedata

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.ritesh.gaiansolutions.utils.Constants.FASTEST_INTERVAL
import com.ritesh.gaiansolutions.utils.Constants.INTERVAL

/**
 * Created by Ritesh Kumar on 08/12/22 , 7:08 PM
 * Contact: riteshkumar.4946@gmail.com , +91-7415984946
 */

class LocationLiveData(context: Context) : MutableLiveData<Location>() {
    private var fusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    // TODO: Sets the value. If there are active observers, the value will be dispatched to them. This Method must be called from the main thread. If you need set a value from a background thread, you can use postValue(Object)
    private fun setLocationData(location: Location) {
        value = location
    }

    // TODO: Static object of location request
    companion object {
        val locationRequest: LocationRequest = LocationRequest.create().apply {
            interval = INTERVAL
            fastestInterval = FASTEST_INTERVAL
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    // TODO: Called when the number of active observers change to 1 from 0. This callback can be used to know that this Livedata is being used thus should be kept up to date.
    override fun onInactive() {
        super.onInactive()
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    // TODO: Called when the number of active observers change from 1 to 0. This does not mean that there are no observers left, there may still be observers but their lifecycle states aren't STARTED or RESUMED (like an Activity in the back stack). You can check if there are observers via hasObservers() method
    @SuppressLint("MissingPermission")
    override fun onActive() {
        super.onActive()
        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.also {
                    setLocationData(it)
                }
            }
        startLocationUpdates()
    }

    // TODO: Callback that triggers on location updates available
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            locationResult ?: return
            for (location in locationResult.locations) {
                setLocationData(location)
            }
        }
    }

    // TODO : Initiate Location Updates using Fused Location Provider and attaching callback to listen location updates
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null
        )
    }


}