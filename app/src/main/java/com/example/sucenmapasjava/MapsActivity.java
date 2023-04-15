package com.example.sucenmapasjava;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.Manifest;
import android.view.View;
import android.widget.Button;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.sucenmapasjava.databinding.ActivityMapsBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonPolygon;
import com.google.maps.android.ui.IconGenerator;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 99;
    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private List<Marker> markers = new ArrayList<>();

    private FusedLocationProviderClient mFusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        Button gpsButton = findViewById(R.id.gps_button);
        gpsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getLastLocation();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Tupa and move the camera
        LatLng tupa = new LatLng(-21.935, -50.513889);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(tupa, 16));
        // Adicione a camada GeoJSON ao mapa
        try {
            GeoJsonLayer layer = new GeoJsonLayer(mMap, R.raw.quarteirao_tupa_geojson, getApplicationContext());
            layer.addLayerToMap();

            // Inicialize o IconGenerator
            IconGenerator iconGenerator = new IconGenerator(getApplicationContext());

            // Defina o plano de fundo do marcador como transparente
            iconGenerator.setBackground(null);

            // Adicione o ID no centro de cada polígono
            for (GeoJsonFeature feature : layer.getFeatures()) {
                String id = feature.getProperty("ID");
                GeoJsonPolygon polygon = (GeoJsonPolygon) feature.getGeometry();
                LatLng centroid = calculateCentroid(polygon);

                // Crie um ícone personalizado com o ID como texto
                //iconGenerator.setTextAppearance(R.style.MarkerTextStyle);
                Bitmap iconBitmap = iconGenerator.makeIcon(id);

                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(centroid)
                        .icon(BitmapDescriptorFactory.fromBitmap(iconBitmap)));
                markers.add(marker);
            }

        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }

        // Adicione um ouvinte para atualizar a visibilidade dos marcadores com base no nível de zoom
        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                float zoomLevel = mMap.getCameraPosition().zoom;

                for (Marker marker : markers) {
                    if (zoomLevel > 15) {
                        marker.setVisible(true);
                    } else {
                        marker.setVisible(false);
                    }
                }
            }
        });
    }

    private LatLng calculateCentroid(GeoJsonPolygon polygon) {
        List<LatLng> points = polygon.getOuterBoundaryCoordinates();
        double centroidLat = 0, centroidLng = 0;
        int n = points.size();

        for (LatLng point : points) {
            centroidLat += point.latitude;
            centroidLng += point.longitude;
        }

        centroidLat /= n;
        centroidLng /= n;

        return new LatLng(centroidLat, centroidLng);
    }

    private void getLastLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Task<Location> locationTask = mFusedLocationClient.getLastLocation();
            locationTask.addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                    }
                }
            });
        }
    }

}
