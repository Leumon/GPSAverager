package me.muehl.gpsaverager

import android.widget.Button
import android.widget.TextView
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow

class CustomInfoWindow(layoutResId: Int, mapView: MapView?, point: GeoPoint, marker: Marker, numMarker: Int, accuracy: Float) : InfoWindow(layoutResId, mapView) {

    var point : GeoPoint
    var marker : Marker
    var numMarker : Int
    var accuracy : Float

    init {
        this.point = point
        this.marker = marker
        this.numMarker = numMarker
        this.accuracy = accuracy
    }

    override fun onOpen(item: Any?) {
        closeAllInfoWindowsOn(mapView)
        var textMarkerInfo = view.findViewById<TextView>(R.id.textMarkerInfo)
        var buttonRemove = view.findViewById<Button>(R.id.buttonRemove)

        textMarkerInfo.setText("Point " + numMarker + "\n"
                                + "Coordinates: " + Coordinates(point.latitude, point.longitude).toDDMString() + "\n"
                                + "Accuracy: " + accuracy)

        buttonRemove.setOnClickListener {
            MainActivity.points.remove(point)
            MainActivity.map.overlays.remove(marker)
            MainActivity.calcAvgPos()
            mapView.invalidate()
            close()
        }
    }



    override fun onClose() {

    }
}