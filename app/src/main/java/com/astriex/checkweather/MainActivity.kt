package com.astriex.checkweather

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.astriex.checkweather.databinding.ActivityMainBinding
import com.astriex.checkweather.utils.Constants
import com.astriex.checkweather.utils.StringUtils.Companion.showMessage
import com.google.android.gms.location.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener

class MainActivity : AppCompatActivity() {
    private lateinit var bindingMain: ActivityMainBinding
    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingMain = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bindingMain.root)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        checkLocationService()
        checkPermissions()
    }

    private fun getLocationWeatherDetails() {
        if(Constants.isNetworkAvailable(this)) {
            showMessage(this, "you are connected to the internet")
        } else {
            showMessage(this, "no internet connection")
        }
    }

    private fun checkLocationService() {
        if (!isLocationEnabled()) {
            showMessage(this, "Your location provider is turned off. Please turn it on.")
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }

    private fun checkPermissions() {
        if (isLocationEnabled()) {
            Dexter.withContext(this)
                .withPermissions(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report!!.areAllPermissionsGranted()) {
                            requestLocationData()
                        }

                        if (report.isAnyPermissionPermanentlyDenied) {
                            showMessage(
                                this@MainActivity, "You have denied location permission. " +
                                        "Please enable them as it is mandatory for the app to work."
                            )
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        request: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        showRationaleDialogForPermissions()
                    }
                }).onSameThread().check()
        }
    }

    private fun showRationaleDialogForPermissions() {
        AlertDialog.Builder(this).setMessage("Enable permissions in app settings.")
            .setPositiveButton("GO TO SETTINGS") { _, _ ->
                try {
                    goToSettings()
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    private fun goToSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest,
            mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            val mLastLocation: Location = locationResult!!.lastLocation
            val latitude = mLastLocation.latitude
            Log.i("current latitude", "$latitude")
            val longitude = mLastLocation.longitude
            Log.i("current longitude", "$longitude")

            getLocationWeatherDetails()
        }
    }
}