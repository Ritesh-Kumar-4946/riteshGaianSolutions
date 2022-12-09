package com.ritesh.gaiansolutions.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.DefaultTimeBar
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.ui.TimeBar
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import com.ritesh.gaiansolutions.R
import com.ritesh.gaiansolutions.databinding.ActivityMainsBinding
import com.ritesh.gaiansolutions.glide.GlideThumbnailTransformation
import com.ritesh.gaiansolutions.permission.EzPermission
import com.ritesh.gaiansolutions.utils.Constants.LOCATION_PERMISSION_REQUEST
import com.ritesh.gaiansolutions.utils.Constants.SAMPLE_MP4_IMAGE_URL
import com.ritesh.gaiansolutions.utils.Constants.SAMPLE_MP4_URL
import com.ritesh.gaiansolutions.utils.Constants.STATE_PLAYER_FULLSCREEN
import com.ritesh.gaiansolutions.utils.Constants.STATE_PLAYER_PLAYING
import com.ritesh.gaiansolutions.utils.Constants.STATE_RESUME_POSITION
import com.ritesh.gaiansolutions.utils.Constants.STATE_RESUME_WINDOW
import com.ritesh.gaiansolutions.utils.LocationUtil
import com.ritesh.gaiansolutions.utils.ShakeDetector
import com.ritesh.gaiansolutions.utils.ShakeDetector.OnShakeListener
import com.ritesh.gaiansolutions.viewmodel.LocationViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainsBinding
    private lateinit var locationViewModel: LocationViewModel
    private var isGPSEnabled = false
    private var initLat: Double = 0.0
    private var initLong: Double = 0.0
    private var theBoolean : Boolean = true

    private val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )


    private lateinit var exoPlayer: SimpleExoPlayer
    private lateinit var dataSourceFactory: DataSource.Factory
    private lateinit var playerView: PlayerView
    private lateinit var exoProgress: DefaultTimeBar
    private lateinit var previewFrameLayout: FrameLayout
    private lateinit var previewImage: ImageView

    private var currentWindow = 0
    private var playbackPosition: Long = 0
    private var isFullscreen = false
    private var isPlayerPlaying = true
    private val mediaItem = MediaItem.Builder()
        .setUri(SAMPLE_MP4_URL)
        .setMimeType(MimeTypes.APPLICATION_MP4)
        .build()


    private var mSensorManager: SensorManager? = null
    private var mAccelerometer: Sensor? = null
    private var mShakeDetector: ShakeDetector? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // TODO: Instance of LocationViewModel
        locationViewModel = ViewModelProvider(this).get(LocationViewModel::class.java)

        //Check weather Location/GPS is ON or OFF
        LocationUtil(this).turnGPSOn(object :
            LocationUtil.OnLocationOnListener {

            override fun locationStatus(isLocationOn: Boolean) {
                this@MainActivity.isGPSEnabled = isLocationOn
            }
        })


        playerView = findViewById(R.id.player_view)
        exoProgress = playerView.findViewById(R.id.exo_progress)
        previewFrameLayout = playerView.findViewById(R.id.preview_frame_layout)
        previewImage = playerView.findViewById(R.id.preview_image)

        dataSourceFactory = DefaultDataSourceFactory(
            this,
            Util.getUserAgent(this, "testapp")
        )

        if (savedInstanceState != null) {
            currentWindow = savedInstanceState.getInt(STATE_RESUME_WINDOW)
            playbackPosition = savedInstanceState.getLong(STATE_RESUME_POSITION)
            isFullscreen = savedInstanceState.getBoolean(STATE_PLAYER_FULLSCREEN)
            isPlayerPlaying = savedInstanceState.getBoolean(STATE_PLAYER_PLAYING)
        }

        initSensors()

    }


    private fun initSensors(){
        // TODO: ShakeDetector initialization
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mShakeDetector = ShakeDetector()
        mShakeDetector!!.setOnShakeListener(OnShakeListener { count ->
//            theBoolean = !theBoolean;
            theBoolean = theBoolean xor true
            Log.e("Shake", "onShake= $count, theBoolean= $theBoolean")
//            exoPlayer.playWhenReady = theBoolean // play pause video
            exoPlayer.seekTo(0)
            exoPlayer.playWhenReady = true

        })
    }



    private fun initPlayer() {
        exoPlayer = SimpleExoPlayer.Builder(this).build().apply {
            playWhenReady = isPlayerPlaying
            seekTo(currentWindow, playbackPosition)
            setMediaItem(mediaItem, false)
            prepare()
        }
        playerView.player = exoPlayer

        exoProgress.addListener(object : TimeBar.OnScrubListener {
            override fun onScrubMove(timeBar: TimeBar, position: Long) {
                previewFrameLayout.visibility = View.VISIBLE
                previewFrameLayout.x =
                    updatePreviewX(position.toInt(), exoPlayer.duration.toInt()).toFloat()

                val drawable = previewImage.drawable
                var glideOptions = RequestOptions().dontAnimate().skipMemoryCache(false)
                if (drawable != null) {
                    glideOptions = glideOptions.placeholder(drawable)
                }

                Glide.with(previewImage).asBitmap()
                    .apply(glideOptions)
                    .load(SAMPLE_MP4_IMAGE_URL)
                    .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .transform(GlideThumbnailTransformation(position))
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .into(previewImage)
            }

            override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                previewFrameLayout.visibility = View.INVISIBLE
            }

            override fun onScrubStart(timeBar: TimeBar, position: Long) {}
        })

    }

    private fun updatePreviewX(progress: Int, max: Int): Int {
        if (max == 0) return 0

        val parent = previewFrameLayout.parent as ViewGroup
        val layoutParams = previewFrameLayout.layoutParams as ViewGroup.MarginLayoutParams
        val offset = progress.toFloat() / max
        val minimumX: Int = previewFrameLayout.left
        val maximumX = (parent.width - parent.paddingRight - layoutParams.rightMargin)

        val previewPaddingRadius: Int =
            resources.getDimensionPixelSize(R.dimen.scrubber_dragged_size).div(2)
        val previewLeftX = (exoProgress as View).left.toFloat()
        val previewRightX = (exoProgress as View).right.toFloat()
        val previewSeekBarStartX: Float = previewLeftX + previewPaddingRadius
        val previewSeekBarEndX: Float = previewRightX - previewPaddingRadius
        val currentX = (previewSeekBarStartX + (previewSeekBarEndX - previewSeekBarStartX) * offset)
        val startX: Float = currentX - previewFrameLayout.width / 2f
        val endX: Float = startX + previewFrameLayout.width

        // Clamp the moves
        return if (startX >= minimumX && endX <= maximumX) {
            startX.toInt()
        } else if (startX < minimumX) {
            minimumX
        } else {
            maximumX - previewFrameLayout.width
        }
    }

    private fun releasePlayer() {
        isPlayerPlaying = exoPlayer.playWhenReady
        playbackPosition = exoPlayer.currentPosition
        currentWindow = exoPlayer.currentWindowIndex
        exoPlayer.release()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_RESUME_WINDOW, exoPlayer.currentWindowIndex)
        outState.putLong(STATE_RESUME_POSITION, exoPlayer.currentPosition)
        outState.putBoolean(STATE_PLAYER_FULLSCREEN, isFullscreen)
        outState.putBoolean(STATE_PLAYER_PLAYING, isPlayerPlaying)
        super.onSaveInstanceState(outState)
    }


    // TODO: onStart lifecycle of activity
    override fun onStart() {
        super.onStart()
        startLocationUpdates()
        if (Util.SDK_INT > 23) {
            initPlayer()
            playerView.onResume()
        }
    }


    // TODO: onResume lifecycle of activity
    override fun onResume() {
        super.onResume()
        // TODO: Add the following line to register the Session Manager Listener onResume
        mSensorManager!!.registerListener(
            mShakeDetector,
            mAccelerometer,
            SensorManager.SENSOR_DELAY_UI
        )

        if (Util.SDK_INT <= 23) {
            initPlayer()
            playerView.onResume()
        }
    }


    // TODO: onPause lifecycle of activity
    override fun onPause() {
        // TODO: Add the following line to unregister the Sensor Manager onPause
        mSensorManager!!.unregisterListener(mShakeDetector)
        super.onPause()
        if (Util.SDK_INT <= 23) {
            playerView.onPause()
            releasePlayer()
        }
    }


    // TODO: onStop lifecycle of activity
    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            playerView.onPause()
            releasePlayer()
        }
    }


    // TODO: On Activity Result for locations permissions updates
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == LOCATION_PERMISSION_REQUEST) {
                isGPSEnabled = true
                startLocationUpdates()
            }
        }
    }


    // TODO: Observe LocationViewModel LiveData to get updated location
    @SuppressLint("SetTextI18n")
    private fun observeLocationUpdates() {
        locationViewModel.getLocationData.observe(this, Observer {
            binding.longitude.text = "CurrentLong: ${it.longitude}"
            binding.latitude.text = "CurrentLat: ${it.latitude}"
            binding.info.text = getString(R.string.location_successfully_received)
            if (initLat == 0.0 && initLong == 0.0) {
                Log.e(
                    "observeLocationUpdates",
                    "If_NeedUpdate: initLat= $initLat,  initLong= $initLong"
                )
                initLat = it.latitude
                initLong = it.longitude
                binding.tvInitvalues.text = "initLat= $initLat , initLong= $initLong"
                Log.e(
                    "observeLocationUpdates",
                    "AfterUUpdate: initLat= $initLat,  initLong= $initLong"
                )
            } else {
                Log.e("observeLocationUpdates", "Else_NoNeedUpdate= $initLat,  initLong= $initLong")
            }

            Log.e(
                "getLocation",
                "mLocation_NotNull: Lat= " + it.latitude.toString() + ",  Long= " + it.longitude.toString()
            )

            val startPoint = Location("locationA")
            startPoint.latitude = initLat
            startPoint.longitude = initLong
            Log.e("startPoint", "Lat= $initLat,  Long= $initLong")

            val endPoint = Location("locationA")
            endPoint.latitude = it.latitude
            endPoint.longitude = it.longitude
            Log.e("endPoint", "Lat= " + it.latitude + ", Long= " + it.latitude)
            val distance = startPoint.distanceTo(endPoint).toDouble()
            Log.e("distance", "distance= $distance")
            binding.tvDistance.text = "Distance= $distance"
//            Toast.makeText(this, "Location_distance:$distance", Toast.LENGTH_SHORT).show()

            if (distance >= 10) {
                initLat = 0.0
                initLong = 0.0
                binding.tvInitvalues.text = "initLat= $initLat , initLong= $initLong"
                exoPlayer.seekTo(0)
                exoPlayer.playWhenReady = true
                Toast.makeText(this, "ResetLatLong :$distance", Toast.LENGTH_SHORT).show()
            }

        })
    }


    // TODO: Initiate Location updated by checking Location/GPS settings is ON or OFF, Requesting permissions to read location.
    private fun startLocationUpdates() {
        when {
            !isGPSEnabled -> {
                binding.info.text = getString(R.string.enable_gps)
            }

            isLocationPermissionsGranted() -> {
                observeLocationUpdates()
            }
            else -> {
                askLocationPermission()
            }
        }
    }


    // TODO: Check the availability of location permissions
    private fun isLocationPermissionsGranted(): Boolean {
        return (EzPermission.isGranted(this, locationPermissions[0])
                && EzPermission.isGranted(this, locationPermissions[1]))
    }


    private fun askLocationPermission() {
        EzPermission
            .with(this)
            .permissions(locationPermissions[0], locationPermissions[1])
            .request { granted, denied, permanentlyDenied ->
                if (granted.contains(locationPermissions[0]) &&
                    granted.contains(locationPermissions[1])
                ) { // Granted
                    startLocationUpdates()

                } else if (denied.contains(locationPermissions[0]) ||
                    denied.contains(locationPermissions[1])
                ) { // Denied

                    showDeniedDialog()

                } else if (permanentlyDenied.contains(locationPermissions[0]) ||
                    permanentlyDenied.contains(locationPermissions[1])
                ) { // Permanently denied
                    showPermanentlyDeniedDialog()
                }

            }
    }


    private fun showPermanentlyDeniedDialog() {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle(getString(R.string.title_permission_permanently_denied))
        dialog.setMessage(getString(R.string.message_permission_permanently_denied))
        dialog.setNegativeButton(getString(R.string.not_now)) { _, _ -> }
        dialog.setPositiveButton(getString(R.string.settings)) { _, _ ->
            startActivity(
                EzPermission.appDetailSettingsIntent(
                    this
                )
            )
        }
        dialog.setOnCancelListener { } //important
        dialog.show()
    }


    private fun showDeniedDialog() {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle(getString(R.string.title_permission_denied))
        dialog.setMessage(getString(R.string.message_permission_denied))
        dialog.setNegativeButton(getString(R.string.cancel)) { _, _ -> }
        dialog.setPositiveButton(getString(R.string.allow)) { _, _ ->
            askLocationPermission()
        }
        dialog.setOnCancelListener { } //important
        dialog.show()
    }


}