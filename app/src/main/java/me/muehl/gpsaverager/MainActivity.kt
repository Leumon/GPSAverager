package me.muehl.gpsaverager

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow


class MainActivity : AppCompatActivity() {

    private lateinit var mLocationListener: LocationListener
    lateinit var mLocationManager : LocationManager


    companion object {
        lateinit var points : MutableList<GeoPoint>
        lateinit var map : MapView
        var running : Boolean = false
        lateinit var avg_marker : Marker
        private const val MY_PERMISSIONS_REQUEST_LOCATION = 99
        private const val LOCATION_REFRESH_TIME : Long = 2000

        fun calcAvgPos() {
            var num : Int = 0
            var avgLat : Double = 0.0
            var avgLon : Double = 0.0
            for(point in points){
                avgLat += point.latitude
                avgLon += point.longitude
                num++
            }
            if (points.isEmpty()) {
                map.overlays.removeAll{(it !is MapEventsOverlay) and (it !is CopyrightOverlay)}
            } else {
                if(!map.overlays.contains(avg_marker))
                    map.overlays.add(1, avg_marker)
                avg_marker.position = GeoPoint(avgLat / num, avgLon / num)
            }
        }


    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));

        setContentView(R.layout.activity_main)

        var statusText : TextView = findViewById(R.id.textStatus)

        map = findViewById<MapView>(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setBuiltInZoomControls(false)
        map.setMultiTouchControls(true)

        val mapController = map.controller
        mapController.setZoom(3.0)
        map.invalidate()

        map.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                InfoWindow.closeAllInfoWindowsOn(map)
                return true
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                return false
            }
        }))

        val overlay = CopyrightOverlay(this)
        map.overlays.add(overlay);

        var buttonStartStop = findViewById<Button>(R.id.buttonStartStop)
        var buttonShare = findViewById<Button>(R.id.buttonShare)

        avg_marker = Marker(map)

        buttonStartStop.setOnClickListener {
            if (!running){
                if(this::mLocationManager.isInitialized) {
                    mLocationManager.removeUpdates(mLocationListener)
                    map.overlays.removeAll {(it !is MapEventsOverlay) and (it !is CopyrightOverlay)}
                    map.invalidate()
                }
                points = mutableListOf()
                buttonShare.isEnabled = false

                buttonStartStop.setText("STOP")
                statusText.setText("Loading...")

                var markerGrey = ContextCompat.getDrawable(this, R.drawable.marker_grey_small)

                var centerSet = false

                points = mutableListOf<GeoPoint>()

                avg_marker.icon = ContextCompat.getDrawable(this, R.drawable.marker_green)
                avg_marker.title = "Average Position"
                avg_marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                map.overlays.add(avg_marker)
                avg_marker.setVisible(false)

                map.setTileSource(TileSourceFactory.MAPNIK)
                (map.tileProvider as MapTileProviderBasic).tileCache.protectedTileComputers.clear()

                getInstance().userAgentValue = BuildConfig.APPLICATION_ID

                mLocationListener = object : LocationListener {
                    override fun onLocationChanged(it: Location) {
                        var pos = GeoPoint(it.latitude, it.longitude)
                        if (!centerSet) {
                            mapController.setZoom(20.0)
                            mapController.setCenter(pos)
                            buttonShare.isEnabled = true
                            centerSet = true
                            map.invalidate()
                            avg_marker.setVisible(true)
                        }


                        points.add(pos)

                        var marker = Marker(map)
                        marker.position = pos
                        marker.icon = markerGrey
                        var iw = CustomInfoWindow(R.layout.marker_info, map, pos, marker, points.size, it.accuracy)
                        marker.infoWindow = iw
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        map.overlays.add(marker)

                        calcAvgPos()

                        map.invalidate()

                        var coords = Coordinates(avg_marker.position.latitude, avg_marker.position.longitude)
                        statusText.setText("AVG gps position: " + coords.toDDMString() + " (of " + points.size + " points)")

                    }

                    override fun onProviderEnabled(provider: String) {}

                    override fun onProviderDisabled(provider: String) {}

                    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
                }



                mLocationManager = getSystemService(LOCATION_SERVICE) as LocationManager
                checkLocationPermission()
                running = true

            } else {
                buttonStartStop.setText("START")
                statusText.setText("Position ready. Click \"SHARE\" to export coordinates")
                mLocationManager.removeUpdates(mLocationListener)
                running = false
                buttonShare.isEnabled = true
            }
        }

        

        buttonShare.setOnClickListener {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                var coords = Coordinates(avg_marker.position.latitude, avg_marker.position.longitude)
                putExtra(Intent.EXTRA_TEXT, coords.toDDMString())
                type = "text/plain"
            }

            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)

        }

    }

    private fun checkPermissions() {

    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                AlertDialog.Builder(this)
                    .setTitle("Location Permission Needed")
                    .setMessage("This app needs the Location permission, please accept to use location functionality")
                    .setPositiveButton(
                        "OK"
                    ) { _, _ ->
                        requestLocationPermission()
                    }
                    .create()
                    .show()
            } else {
                requestLocationPermission()
            }
        } else {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                Companion.LOCATION_REFRESH_TIME, 0.01f, mLocationListener)
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            MY_PERMISSIONS_REQUEST_LOCATION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                            Companion.LOCATION_REFRESH_TIME, 0.01f, mLocationListener)
                    }

                } else {

                    Toast.makeText(this, "ERROR: LOCATION PERMISSION WAS NOT GRANTED", Toast.LENGTH_LONG).show()

                    if (!ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    ) {
                        startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", this.packageName, null),
                            ),
                        )
                    }
                }
                return
            }

        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

}
