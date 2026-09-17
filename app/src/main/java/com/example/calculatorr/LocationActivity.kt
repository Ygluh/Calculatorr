package com.example.calculatorr

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationActivity : AppCompatActivity() {

    private lateinit var tvLatitude: TextView
    private lateinit var tvLongitude: TextView
    private lateinit var tvAltitude: TextView
    private lateinit var tvTime: TextView

    private lateinit var locationManager: LocationManager

    private val jsonFile: File by lazy { File(filesDir, "locations.json") }

    private val LOCATION_PERMISSION_REQUEST = 200

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            showLocation(location)
            saveToJson(location)
        }
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        tvLatitude = findViewById(R.id.tvLatitude)
        tvLongitude = findViewById(R.id.tvLongitude)
        tvAltitude = findViewById(R.id.tvAltitude)
        tvTime = findViewById(R.id.tvTime)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        findViewById<Button>(R.id.btnGetLocation).setOnClickListener {
            checkPermissionAndGetLocation()
        }

        findViewById<Button>(R.id.btnSaveJson).setOnClickListener {
            Toast.makeText(this, "Файл: ${jsonFile.absolutePath}", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkPermissionAndGetLocation() {
        val fine = Manifest.permission.ACCESS_FINE_LOCATION
        val coarse = Manifest.permission.ACCESS_COARSE_LOCATION

        val grantedFine = ActivityCompat.checkSelfPermission(this, fine) == PackageManager.PERMISSION_GRANTED
        val grantedCoarse = ActivityCompat.checkSelfPermission(this, coarse) == PackageManager.PERMISSION_GRANTED

        if (grantedFine && grantedCoarse) {
            getLastLocation()
            startLocationUpdates()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(fine, coarse),
                LOCATION_PERMISSION_REQUEST
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() &&
                grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                getLastLocation()
                startLocationUpdates()
            } else {
                Toast.makeText(this, "Разрешение не дано", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )

        var lastLocation: Location? = null
        for (provider in providers) {
            try {
                if (locationManager.isProviderEnabled(provider)) {
                    val loc = locationManager.getLastKnownLocation(provider)
                    if (loc != null) {
                        lastLocation = loc
                        break
                    }
                }
            } catch (e: Exception) {
                // пропускаем
            }
        }

        if (lastLocation != null) {
            showLocation(lastLocation)
            saveToJson(lastLocation)
        } else {
            Toast.makeText(
                this,
                "Последняя локация неизвестна. Задай координаты в эмуляторе (⋮ → Location).",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000L,
                0f,
                locationListener
            )
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                5000L,
                0f,
                locationListener
            )
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showLocation(location: Location) {
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val time = timeFormat.format(Date())

        tvLatitude.text = "Latitude: ${location.latitude}"
        tvLongitude.text = "Longitude: ${location.longitude}"
        tvAltitude.text = "Altitude: ${location.altitude} м"
        tvTime.text = "Current Time: $time"
    }

    private fun saveToJson(location: Location) {
        try {
            val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val time = timeFormat.format(Date())

            val record = JSONObject().apply {
                put("latitude", location.latitude)
                put("longitude", location.longitude)
                put("altitude", location.altitude)
                put("time", time)
            }

            val array: JSONArray = if (jsonFile.exists()) {
                JSONArray(jsonFile.readText())
            } else {
                JSONArray()
            }

            array.put(record)
            jsonFile.writeText(array.toString(2))

        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка JSON: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            locationManager.removeUpdates(locationListener)
        } catch (e: Exception) {
            // ignore
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            locationManager.removeUpdates(locationListener)
        } catch (e: Exception) {
        }
    }
}