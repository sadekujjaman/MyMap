package com.example.saju.mymap;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.saju.mymap.model.PlaceInfo;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "MapActivity";
    private static final String ACCESS_FINE = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String ACCESS_COARSE = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;
    private static final LatLngBounds myLAT_LNG_BOUNDS = new LatLngBounds(
            new LatLng(-40, -168), new LatLng(71, 136)
    );

    private static final int PLACE_PICKER_REQUEST = 1;

    private static Boolean myLocationPermissionGranted = false;
    GoogleMap myMap;
    FusedLocationProviderClient myFusedLocationProviderClient;
    GoogleApiClient myGoogleApiClient;
    GeoDataClient myGeoDataClient;
    AutoCompleteTextView searchInputText;
    ImageView gpsView, infoView, placeView;
    private Marker myMarker;
    private PlaceInfo myPlace;
    private PlaceAutoCompleteAdapter myPlaceAutoCompleteAdapter;
    private ResultCallback<PlaceBuffer> myUpdateDetailsCallback = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(@NonNull PlaceBuffer places) {

            if (!places.getStatus().isSuccess()) {
                Log.d(TAG, "onResult: place query did not complete Successfully: "
                        + places.getStatus().toString());
                places.release();
                return;
            }

            final Place place = places.get(0);

            try {

                myPlace = new PlaceInfo();

                myPlace.setName(place.getName().toString());
                Log.d(TAG, "onResult: name: " + place.getName());

                myPlace.setLatLng(place.getLatLng());
                Log.d(TAG, "onResult: LatLng: " + place.getLatLng());

                myPlace.setAddress(place.getAddress().toString());
                Log.d(TAG, "onResult: Address:  " + place.getAddress());

                myPlace.setId(place.getId());
                Log.d(TAG, "onResult: Id: " + place.getId());

                myPlace.setPhoneNumber(place.getPhoneNumber().toString());
                Log.d(TAG, "onResult: PhoneNumber: " + place.getPhoneNumber());

                myPlace.setWebsiteUri(place.getWebsiteUri());
                Log.d(TAG, "onResult: WebsiteUri: " + place.getWebsiteUri());

                myPlace.setRating(place.getRating());
                Log.d(TAG, "onResult: Rating: " + place.getRating());

                Log.d(TAG, "onResult: place: " + myPlace.toString());
            } catch (NullPointerException e) {
                Log.d(TAG, "onResult: NullPointerException: " + e.getMessage());
            }

            moveCameraToLocation(myPlace.getLatLng(), DEFAULT_ZOOM, myPlace);
//            moveCameraToLocation(new LatLng(place.getViewport().getCenter().latitude,
//                    place.getViewport().getCenter().longitude) , DEFAULT_ZOOM, myPlace.getName());
            places.release();
        }
    };
    private AdapterView.OnItemClickListener myAutoCompleteListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
            hideSoftKeyBoard();

            final AutocompletePrediction item = myPlaceAutoCompleteAdapter.getItem(position);
            final String placeId = item.getPlaceId();
            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(myGoogleApiClient, placeId);

            placeResult.setResultCallback(myUpdateDetailsCallback);

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        searchInputText = findViewById(R.id.search_input_id);
        searchInputText.setSingleLine();
        searchInputText.setMaxLines(1);
        searchInputText.setInputType(InputType.TYPE_CLASS_TEXT);

        gpsView = findViewById(R.id.ic_gps);
        infoView = findViewById(R.id.ic_info);
        placeView = findViewById(R.id.ic_places);

        getLocationPermission();
    }

    public void init() {
        Log.d(TAG, "init: initializing");

        myGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();

        myGeoDataClient = Places.getGeoDataClient(this, null);

        searchInputText.setOnItemClickListener(myAutoCompleteListener);

        myPlaceAutoCompleteAdapter = new PlaceAutoCompleteAdapter(
                this, myGeoDataClient, myLAT_LNG_BOUNDS, null
        );

        searchInputText.setAdapter(myPlaceAutoCompleteAdapter);

        searchInputText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {

                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || keyEvent.getAction() == KeyEvent.ACTION_DOWN
                        || keyEvent.getAction() == KeyEvent.KEYCODE_ENTER) {

                    // execute our method for searching
                    geoLocate();
                }

                return false;
            }
        });

        gpsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: clicking gps Icon");
                searchInputText.setText("");
                getDevicesLocation();
            }
        });

        infoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: clicked info icon");
                try {
                    if (myMarker.isInfoWindowShown()) {
                        myMarker.hideInfoWindow();
                    } else {
                        Log.d(TAG, "onClick: place info shown: " + myPlace.toString());
                        myMarker.showInfoWindow();
                    }
                } catch (NullPointerException e) {
                    Log.d(TAG, "onClick: NullPointerException: " + e.getMessage());
                }
            }
        });

        placeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: placePicker Clicked");

                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

                try {

                    startActivityForResult(builder.build(MapActivity.this), PLACE_PICKER_REQUEST);

                } catch (GooglePlayServicesRepairableException e) {
                    Log.d(TAG, "onClick: GooglePlayServicesRepairableException: " + e.getMessage());
                } catch (GooglePlayServicesNotAvailableException e) {
                    Log.d(TAG, "onClick: GooglePlayServicesNotAvailableException: " + e.getMessage());
                }
            }
        });

        hideSoftKeyBoard();

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(this, data);

                PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                        .getPlaceById(myGoogleApiClient, place.getId());

                placeResult.setResultCallback(myUpdateDetailsCallback);
            }
        }
    }

    private void hideSoftKeyBoard() {

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchInputText.getWindowToken(), 0);

    }

    private void geoLocate() {
        Log.d(TAG, "geoLocate: geoLocating...");

        String searchText = searchInputText.getText().toString();

        Geocoder geocoder = new Geocoder(MapActivity.this);
        List<Address> list = new ArrayList<>();

        try {

            list = geocoder.getFromLocationName(searchText, 1);

        } catch (IOException e) {
            Log.d(TAG, "geoLocate: IOException: " + e.getMessage());
        }

        if (list.size() > 0) {
            Address address = list.get(0);

            Log.d(TAG, "geoLocate: Address: " + address.toString());

            moveCameraToLocation(new LatLng(address.getLatitude(), address.getLongitude()),
                    DEFAULT_ZOOM, address.getAddressLine(0));

        }

    }

    private void getDevicesLocation() {
        Log.d(TAG, "getDevicesLocation: getting Devices current Location");

        myFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try {

            if (myLocationPermissionGranted) {

                final Task location = myFusedLocationProviderClient.getLastLocation();

                try {
                    location.addOnCompleteListener(new OnCompleteListener() {
                        @Override
                        public void onComplete(@NonNull Task task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "getDevicesLocation: Location Found " + task);

                                Location currentLocation = (Location) task.getResult();
                                Log.d(TAG, "Location: " + currentLocation);
                                if (currentLocation != null) {
                                    Log.d(TAG, "Location: " + currentLocation.getLatitude() + ", " + currentLocation.getLongitude());

                                    moveCameraToLocation(new LatLng(currentLocation.getLatitude(),
                                                    currentLocation.getLongitude()),
                                            DEFAULT_ZOOM, "My Location");
                                } else {
                                    Log.d(TAG, "Location Null");
                                }

                            } else {
                                Log.d(TAG, "getDevicesLocation: can not get current location ");
                                Toast.makeText(MapActivity.this, "Failed to getting Current Location", Toast.LENGTH_SHORT).show();

                            }
                        }
                    });
                } catch (Exception e) {
                    Log.d(TAG, "getDevicesLocation: Error on completion " + e.getMessage());

                }


            }
        } catch (SecurityException e) {
            Log.d(TAG, "getDevicesLocation: SecurityException: " + e.getMessage());

        }

    }

    private void moveCameraToLocation(LatLng latLng, float zoom, PlaceInfo placeInfo) {

        Log.d(TAG, "moveCameraToCurrentLocation: moving camera to lat: " + latLng.latitude + ", lng: " + latLng.longitude);
        myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        myMap.clear();

        myMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(MapActivity.this));

        if (placeInfo != null) {

            try {

                String snippted = "Address: " + placeInfo.getAddress() + "\n" +
                        "Latitude: " + placeInfo.getLatLng().latitude + "\n" +
                        "Longitude: " + placeInfo.getLatLng().longitude + "\n" +
                        "Phone No: " + placeInfo.getPhoneNumber() + "\n" +
                        "Price Rating: " + placeInfo.getRating() + "\n" +
                        "Websites: " + placeInfo.getWebsiteUri() + "\n";

                MarkerOptions options = new MarkerOptions()
                        .title(placeInfo.getName())
                        .position(latLng)
                        .snippet(snippted);

                myMarker = myMap.addMarker(options);

            } catch (NullPointerException e) {
                Log.d(TAG, "moveCamera: NullPointerException: " + e.getMessage());
            }

        } else {
            myMarker = myMap.addMarker(new MarkerOptions().position(latLng));
        }

        hideSoftKeyBoard();
    }


//    public boolean isGPSEnabled(Context mContext)
//    {
//        Log.d(TAG, "isGPSEnabled: getting Location Services" );
//        LocationManager lm = (LocationManager)
//                mContext.getSystemService(Context.LOCATION_SERVICE);
//        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
//    }

    private void moveCameraToLocation(LatLng latLng, float zoom, String title) {

        Log.d(TAG, "moveCameraToCurrentLocation: moving camera to lat: " + latLng.latitude + ", lng: " + latLng.longitude);
        myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));


        MarkerOptions options = new MarkerOptions()
                .position(latLng)
                .title(title);

        myMap.addMarker(options);

        hideSoftKeyBoard();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady: map is ready");
        Toast.makeText(MapActivity.this, "Map is Ready", Toast.LENGTH_LONG).show();
        myMap = googleMap;
        if (myLocationPermissionGranted) {
            getDevicesLocation();
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
            myMap.setMyLocationEnabled(true);
            myMap.getUiSettings().setMyLocationButtonEnabled(false);
            Log.d(TAG, "onMapReady: calling init");
            init();
        }


    }

    private void init_Map() {
        Log.d(TAG, "init_Map: Map Initializing");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(MapActivity.this);

//        mapFragment.getMapAsync(new OnMapReadyCallback() {
//            @Override
//            public void onMapReady(GoogleMap googleMap) {
//                myMap = googleMap;
//            }
//        });

    }

    private void getLocationPermission() {
        Log.d(TAG, "getLocationPermission: Checking Permission");

        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(MapActivity.this, ACCESS_FINE)
                == PackageManager.PERMISSION_GRANTED) {

            if (ContextCompat.checkSelfPermission(MapActivity.this, ACCESS_COARSE)
                    == PackageManager.PERMISSION_GRANTED) {
                myLocationPermissionGranted = true;
                init_Map();
            } else {
                ActivityCompat.requestPermissions(MapActivity.this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
            }

        } else {
            ActivityCompat.requestPermissions(MapActivity.this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }

    }



    /*
    ---------- Google places Autocomplete suggestions ---------------
     */

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult is called");

        myLocationPermissionGranted = false;

        switch (requestCode) {

            case LOCATION_PERMISSION_REQUEST_CODE:

                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            Log.d(TAG, "onRequestPermissionsResult: Permission Failed");

                            myLocationPermissionGranted = false;
                            return;
                        }
                    }

                    // map is ready, now we can init our map
                    Log.d(TAG, "onRequestPermissionsResult: Permission Success");
                    init_Map();
                }

//                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
//                    myLocationPermissionGranted = true;
//                    // map is ready, now we can init our map
//
//                }
                break;

        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


}
