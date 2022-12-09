package com.ritesh.gaiansolutions.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.ritesh.gaiansolutions.livedata.LocationLiveData

/**
 * Created by Ritesh Kumar on 08/12/22 , 7:09 PM
 * Contact: riteshkumar.4946@gmail.com , +91-7415984946
 */
class LocationViewModel(application: Application) : AndroidViewModel(application) {

    // TODO: MutableLiveData private field to get/save location updated values
    private val locationData =
        LocationLiveData(application)

    // TODO: LiveData a public field to observe the changes of location
    val getLocationData: LiveData<Location> = locationData
}