package com.example.shakil.kotlinmapboxexample

import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.widget.Toast
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity(), OnMapReadyCallback, PermissionsListener, MapboxMap.OnMapClickListener {

    private var mapboxMap: MapboxMap? = null
    private var permissionsManager: PermissionsManager? = null
    private var locationComponent: LocationComponent? = null
    private var currentRoute: DirectionsRoute? = null
    private var navigationMapRoute: NavigationMapRoute? = null
    private val TAG = "DirectionsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.access_token))
        setContentView(R.layout.activity_main)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(getString(R.string.navigation_guidance_day)) {
            style: Style? ->
            enableLocationComponent(style)
            addDestinationIconSymbolLayer(style)
            mapboxMap.addOnMapClickListener(this)
        }
    }

    private fun addDestinationIconSymbolLayer(loadedMapStyle: Style?) {
        loadedMapStyle!!.addImage("destination-icon-id", BitmapFactory.decodeResource(this.resources, R.drawable.mapbox_marker_icon_default))

        val geoJsonSource = GeoJsonSource("destination-source-id")
        loadedMapStyle.addSource(geoJsonSource)
        val destinationSymbolLayer = SymbolLayer("destination-symbol-layer-id", "destination-source-id")
        destinationSymbolLayer.withProperties(PropertyFactory.iconImage("destination-icon-id"),
        PropertyFactory.iconAllowOverlap(true),
        PropertyFactory.iconIgnorePlacement(true))

        loadedMapStyle.addLayer(destinationSymbolLayer)
    }

    override fun onMapClick(point: LatLng): Boolean {
        val destinationPoint = Point.fromLngLat(point.longitude, point.latitude)
        val originPoint = Point.fromLngLat(locationComponent!!.lastKnownLocation!!.longitude, locationComponent!!.lastKnownLocation!!.latitude)

        val source = mapboxMap!!.style!!.getSourceAs<GeoJsonSource>("destination-source-id")
        source?.setGeoJson(Feature.fromGeometry(destinationPoint))

        getRoute(originPoint, destinationPoint)
        return true
    }

    private fun getRoute(origin: Point, destination: Point) {
        NavigationRoute.builder(this).accessToken(Mapbox.getAccessToken()!!).origin(origin)
                .destination(destination)
                .build()
                .getRoute(object : Callback<DirectionsResponse> {
                    override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                        // You can get the generic HTTP info about the response
                        Log.d(TAG, "Response code: " + response.body())
                        if (response.body() == null) {
                            Log.d(TAG, "No routes found, make sure you set the right user and access token")
                            return
                        }
                        else if (response.body()!!.routes().size < 1){
                            Log.e(TAG, "No routes found")
                            return
                        }
                        currentRoute = response.body()!!.routes()[0]

                        //Draw the route on the map
                        if (navigationMapRoute != null) {
                            navigationMapRoute!!.removeRoute()
                        }
                        else {
                            navigationMapRoute = NavigationMapRoute(null, mapView, mapboxMap!!, R.style.NavigationMapRoute)
                        }
                        navigationMapRoute!!.addRoute(currentRoute)
                    }

                    override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                        Log.e(TAG, "Error: " + t.message)
                    }

                })
    }

    private fun enableLocationComponent(loadedMapStyle: Style?) {
        //Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            // Activity the MapboxMap LocationComponent to show user location
            // Adding in LocationComponentOptions is also an optional parameter
            locationComponent = mapboxMap!!.locationComponent
            locationComponent!!.activateLocationComponent(this, loadedMapStyle!!)
            locationComponent!!.setLocationComponentEnabled(true)

            //Set the component's camera mode
            locationComponent!!.setCameraMode(CameraMode.TRACKING)
        }

        else {
            permissionsManager = PermissionsManager(this)
            permissionsManager!!.requestLocationPermissions(this)
        }
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_SHORT).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            enableLocationComponent(mapboxMap!!.style)
        }
        else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        permissionsManager!!.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}
