package com.astriex.checkweather

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
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
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.astriex.checkweather.databinding.ActivityMainBinding
import com.astriex.checkweather.databinding.DialogCustomProgressBinding
import com.astriex.checkweather.models.WeatherResponse
import com.astriex.checkweather.network.WeatherService
import com.astriex.checkweather.utils.Constants
import com.astriex.checkweather.utils.Constants.BASE_URL
import com.astriex.checkweather.utils.StringUtils.Companion.showMessage
import com.google.android.gms.location.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {
    private lateinit var bindingMain: ActivityMainBinding
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var mProgressDialog: Dialog? = null
    private lateinit var bindingDialog: DialogCustomProgressBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingMain = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bindingMain.root)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        checkLocationService()
        checkPermissions()
    }

    private fun getLocationWeatherDetails(latitude: Double, longitude: Double) {
        if (Constants.isNetworkAvailable(this)) {
            val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service: WeatherService = retrofit.create(WeatherService::class.java)

            val listCall: Call<WeatherResponse> = service.getWeather(
                latitude, longitude,
                Constants.METRIC_UNIT,
                Constants.APP_ID
            )

            showCustomProgressDialog()
            listCall.enqueue(object: Callback<WeatherResponse> {
                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    if(response.isSuccessful) {
                        hideProgressDialog()
                        val weatherList: WeatherResponse = response.body()!!
                        Log.i("Response Result", "$weatherList")
                    } else {
                        showErrorCodes(response)
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    Log.e("Errorrrrr", t.message.toString())
                    hideProgressDialog()
                }
            })
        } else {
            showMessage(this, "no internet connection")
        }
    }

    private fun showErrorCodes(response: Response<WeatherResponse>) {
        when (response.code()) {
            400 -> Log.e("Error 400", "Bad Connection")
            404 -> Log.e("Error 404", "Not Found")
            else -> Log.e("Error", "Generic Error")
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
        AlertDialog.Builder(this)
            .setMessage("Enable permissions in app settings.")
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
            val longitude = mLastLocation.longitude

            getLocationWeatherDetails(latitude, longitude)
        }
    }

    private fun showCustomProgressDialog() {
        bindingDialog = DialogCustomProgressBinding.inflate(LayoutInflater.from(this))
        mProgressDialog = Dialog(this).apply {
            setContentView(bindingDialog.root)
            show()
        }
    }

    private fun hideProgressDialog() {
        mProgressDialog?.dismiss()
    }
}