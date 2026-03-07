package com.lumos.ui.maps

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.lumos.domain.service.ConnectivityGate
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.plugins.annotation.Symbol
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions
import kotlin.random.Random

data class MapPoint(
    val latitude: Double,
    val longitude: Double
)

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(
    context: Context,
    connectivityGate: ConnectivityGate,
    referenceLocation: LatLng,
    numberOfPoints: Int,
    onConfirm: (List<MapPoint>) -> Unit
) {

    val mapView = remember { MapView(context) }

    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var symbolManager by remember { mutableStateOf<SymbolManager?>(null) }

    val symbols = remember { mutableStateListOf<Symbol>() }

    var isOnline by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isOnline = runCatching { connectivityGate.canReachServer() }.getOrDefault(false)
    }

    DisposableEffect(mapView) {
        mapView.onStart()
        onDispose {
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        AndroidView(
            factory = {

                mapView.apply {

                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    getMapAsync { mapLibreMap ->

                        map = mapLibreMap

                        mapLibreMap.setStyle(
                            Style.Builder().fromUri("https://demotiles.maplibre.org/style.json")
                        ) { style ->

                            mapLibreMap.cameraPosition = CameraPosition.Builder()
                                .target(referenceLocation)
                                .zoom(18.0)
                                .build()

                            enableLocation(context, mapLibreMap, style)

                            val manager = SymbolManager(mapView, mapLibreMap, style)
                            manager.iconAllowOverlap = true
                            manager.iconIgnorePlacement = true

                            symbolManager = manager

                            symbols.clear()
                            symbols.addAll(
                                createInitialSymbols(
                                    manager,
                                    referenceLocation,
                                    numberOfPoints
                                )
                            )
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {

            Button(
                onClick = {

                    val points = symbols.map {
                        MapPoint(
                            latitude = it.latLng.latitude,
                            longitude = it.latLng.longitude
                        )
                    }

                    onConfirm(points)

                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Confirmar pontos")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {

                    val loc = map?.locationComponent?.lastKnownLocation ?: return@Button

                    map?.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(loc.latitude, loc.longitude))
                        .zoom(18.0)
                        .build()

                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Centralizar no GPS")
            }
        }
    }
}

private fun createInitialSymbols(
    manager: SymbolManager,
    reference: LatLng,
    numberOfPoints: Int
): List<Symbol> {

    val created = mutableListOf<Symbol>()

    repeat(numberOfPoints) {

        val offsetLat = reference.latitude + Random.nextDouble(-0.00005, 0.00005)
        val offsetLon = reference.longitude + Random.nextDouble(-0.00005, 0.00005)

        val symbol = manager.create(
            SymbolOptions()
                .withLatLng(LatLng(offsetLat, offsetLon))
                .withDraggable(true)
        )

        created.add(symbol)
    }

    return created
}

@SuppressLint("MissingPermission")
private fun enableLocation(
    context: Context,
    map: MapLibreMap,
    style: Style
) {

    val locationComponent = map.locationComponent

    locationComponent.activateLocationComponent(
        LocationComponentActivationOptions.builder(context, style).build()
    )

    locationComponent.isLocationComponentEnabled = true
    locationComponent.cameraMode = CameraMode.NONE
    locationComponent.renderMode = RenderMode.COMPASS
}