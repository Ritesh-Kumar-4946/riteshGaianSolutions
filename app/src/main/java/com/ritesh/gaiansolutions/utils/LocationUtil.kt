package com.ritesh.gaiansolutions.utils

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.location.LocationManager
import android.util.Log
import android.widget.Toast
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.SettingsClient
import com.ritesh.gaiansolutions.livedata.LocationLiveData
import com.ritesh.gaiansolutions.utils.Constants.LOCATION_PERMISSION_REQUEST

/**
 * Created by Ritesh Kumar on 08/12/22 , 7:08 PM
 * Contact: riteshkumar.4946@gmail.com , +91-7415984946
 */

class LocationUtil(private val context: Context) {

    private val settingsClient: SettingsClient = LocationServices.getSettingsClient(context)
    private val locationSettingsRequest: LocationSettingsRequest
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    init {
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(LocationLiveData.locationRequest)
        locationSettingsRequest = builder.build()
        builder.setAlwaysShow(true)
    }

    fun turnGPSOn(OnGpsListener: OnLocationOnListener?) {

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            OnGpsListener?.locationStatus(true)
        } else {
            settingsClient
                .checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener(context as Activity) {
                    // GPS enabled already
                    OnGpsListener?.locationStatus(true)
                }
                .addOnFailureListener(context) { e ->
                    when ((e as ApiException).statusCode) {
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED ->

                            try {
                                val rae = e as ResolvableApiException
                                rae.startResolutionForResult(
                                    context,
                                    LOCATION_PERMISSION_REQUEST
                                )
                            } catch (sie: IntentSender.SendIntentException) {
                                Log.i(ContentValues.TAG, "PendingIntent unable to execute request.")
                            }

                        LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                            val errorMessage =
                                "Enable location services from settings."
                            Log.e(ContentValues.TAG, errorMessage)

                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                }
        }
    }

    interface OnLocationOnListener {
        fun locationStatus(isLocationOn: Boolean)
    }
}