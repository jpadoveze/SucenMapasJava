package com.example.sucenmapasjava;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
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
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonPolygon;
import com.google.maps.android.data.geojson.GeoJsonPolygonStyle;
import com.google.maps.android.ui.IconGenerator;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 99;
    private GoogleMap mMap;
    private final List<Marker> markers = new ArrayList<>();

    private FusedLocationProviderClient mFusedLocationClient;

    private boolean poisVisible = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        com.example.sucenmapasjava.databinding.ActivityMapsBinding binding = ActivityMapsBinding.inflate(getLayoutInflater());
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

        FloatingActionButton fab = findViewById(R.id.gps_button);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getLastLocation();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Move a câmera para Tupã - SP
        LatLng tupa = new LatLng(-21.935, -50.513889);

        // Ajusta o zoom inicial
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(tupa, 16));

        // Carregue o estilo personalizado do arquivo JSON
        MapStyleOptions mapStyle = MapStyleOptions.loadRawResourceStyle(this, R.raw.hide_pois_style);

        // Aplique o estilo ao mapa
        mMap.setMapStyle(mapStyle);

        // Adicione a camada GeoJSON ao mapa
        try {
            // Criando a camadada para as quadras (Tupã)
            GeoJsonLayer layerQuarteirao = new GeoJsonLayer(mMap, R.raw.quarteirao_tupa_geojson, getApplicationContext());
            layerQuarteirao.addLayerToMap();

            // Criando a camadada para os Censitários (Tupã)
            GeoJsonLayer layerCensitario = new GeoJsonLayer(mMap, R.raw.censitario_tupa_geojson, getApplicationContext());
            layerCensitario.addLayerToMap();

            // Inicialize o IconGenerator
            IconGenerator iconQuarteirao = new IconGenerator(getApplicationContext());
            IconGenerator iconCensitario = new IconGenerator(getApplicationContext());

            // Defina o plano de fundo do marcador como transparente
            iconQuarteirao.setBackground(null);
            iconCensitario.setBackground(null);

            // Personalize o estilo do texto do IconGenerator
            iconQuarteirao.setTextAppearance(R.style.QuarteiraoTextStyle);
            iconCensitario.setTextAppearance(R.style.CensitarioTextStyle);

            // Alterando a cor da linha dos polígonos da camada layerQuarteirao
            GeoJsonPolygonStyle quarteiraoStyle = new GeoJsonPolygonStyle();
            quarteiraoStyle.setStrokeColor(Color.argb(80, 45, 156, 216)); // Defina a cor da linha para vermelho

            // Adicione o ID no centro de cada polígono quadras
            for (GeoJsonFeature feature : layerQuarteirao.getFeatures()) {
                String id = feature.getProperty("ID");
                GeoJsonPolygon polygon = (GeoJsonPolygon) feature.getGeometry();
                LatLng centroid = calculateCentroid(polygon);
                feature.setPolygonStyle(quarteiraoStyle);

                // Crie um ícone personalizado com o ID como texto
                //iconGenerator.setTextAppearance(R.style.MarkerTextStyle);
                Bitmap iconBitmap = iconQuarteirao.makeIcon(id);

                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(centroid)
                        .icon(BitmapDescriptorFactory.fromBitmap(iconBitmap)));
                markers.add(marker);
            }

            // Alterando a cor da linha dos polígonos da camada layerQuarteirao
            GeoJsonPolygonStyle censitarioStyle  = new GeoJsonPolygonStyle();
            censitarioStyle.setStrokeColor(Color.argb(80, 245, 124, 0)); // Defina a cor da linha para vermelho

            // Adicione o ID no centro de cada polígono censitarios
            for (GeoJsonFeature feature : layerCensitario.getFeatures()) {
                String geocodi = feature.getProperty("CD_GEOCODI");
                GeoJsonPolygon polygon = (GeoJsonPolygon) feature.getGeometry();
                LatLng centroid = calculateCentroid(polygon);
                feature.setPolygonStyle(censitarioStyle);

                // Crie um ícone personalizado com o ID como texto
                //iconGenerator.setTextAppearance(R.style.MarkerTextStyle);
                Bitmap iconBitmap = iconCensitario.makeIcon(geocodi);

                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(centroid)
                        .icon(BitmapDescriptorFactory.fromBitmap(iconBitmap)));
                markers.add(marker);
            }

            Button togglePoisButton = findViewById(R.id.toggle_pois_button);
            togglePoisButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    togglePoisVisibility();
                }
            });


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

    // Controla a visibilidade dos pontos de interesse
    private void togglePoisVisibility() {
        poisVisible = !poisVisible;
        int styleResId = poisVisible ? R.raw.show_pois_style : R.raw.hide_pois_style;
        MapStyleOptions mapStyle = MapStyleOptions.loadRawResourceStyle(this, styleResId);
        mMap.setMapStyle(mapStyle);
    }

}
