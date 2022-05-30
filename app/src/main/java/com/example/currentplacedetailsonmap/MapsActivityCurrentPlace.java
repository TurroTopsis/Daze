package com.example.currentplacedetailsonmap;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MapsActivityCurrentPlace extends AppCompatActivity
        implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener, GoogleMap.OnInfoWindowClickListener {

    private static final String TAG = "myTag";
    private static final String MAPS_API_KEY = "AIzaSyBUHupnB0Ks_eMK25yScFgmA9mQEWlXINU";
    private GoogleMap map;
    private CameraPosition cameraPosition;

    private PlacesClient placesClient; // The entry point to the Places API
    private FusedLocationProviderClient fusedLocationProviderClient; // The entry point to the Fused Location Provider

    // A default location (Bronxville, New York) and default zoom to use when location permission is not granted
    private final LatLng defaultLocation = new LatLng(40.93780651407292, -73.83080002136819);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean locationPermissionGranted;

    // The geographical location where the device is currently located. That is, the last-known location retrieved by the Fused Location Provider
    private Location lastKnownLocation;

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    // Used for selecting the current place.
    private static final int M_MAX_ENTRIES = 5;
    private String[] likelyPlaceNames;
    private String[] likelyPlaceAddresses;
    private List[] likelyPlaceAttributions;
    private LatLng[] likelyPlaceLatLngs;

    private PopupWindow popupWindow;
    private LatLng currentPoint;
    private static final HashMap<String, Object[]> CONCERNS_DICT = new HashMap<String, Object[]>() {{
        put("Pesticides", new Object[]{BitmapDescriptorFactory.HUE_GREEN, "http://npic.orst.edu/health/pets.html"});
        put("Salt", new Object[]{BitmapDescriptorFactory.HUE_AZURE,
                "https://nhdogwalkingclub.com/tips-to-protect-your-dogs-paws-and-the-environment-from-the-harmful-effects-of-road-salt/"});
        put("Ticks & Fleas", new Object[]{BitmapDescriptorFactory.HUE_MAGENTA, "https://pets.webmd.com/ss/slideshow-flea-and-tick-overview"});
        put("Dangerous Animals", new Object[]{BitmapDescriptorFactory.HUE_ROSE,
                "https://dogtime.com/dog-health/general/16043-top-10-animals-that-attack-pets"});
        put("No Dogs", new Object[]{BitmapDescriptorFactory.HUE_RED,
                "https://www.u-buy.jp/productimg/?image=aHR0cHM6Ly9tLm1lZGlhLWFtYXpvbi5jb20vaW1hZ2VzL0kvNjFpQytrbldBakwuX1NMMTUwMF8uanBn.jpg"});
        put("Dangerous Streets", new Object[]{BitmapDescriptorFactory.HUE_ORANGE,
                "https://www.finchleydogwalker.co.uk/the-dangers-of-broken-glass-on-the-footpath.html"});
        put("No Sidewalks", new Object[]{BitmapDescriptorFactory.HUE_BLUE, "https://www.forevermylittlemoon.com/2016/04/walkingsafetytip.html"});
        put("Street Lamp", new Object[]{BitmapDescriptorFactory.HUE_YELLOW, "https://www.k9ofmine.com/dog-walking-at-night/"});
        put("Trash Can", new Object[]{BitmapDescriptorFactory.HUE_VIOLET,
                "https://us.glasdon.com/images/products/400/Fido-25-pet-waste-station-01-Ginc.jpg"});
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) { // Retrieve location and camera position from saved instance state
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        setContentView(R.layout.activity_maps); // Retrieve the content view that renders the map

        // Construct a PlacesClient
        Places.initialize(getApplicationContext(), MAPS_API_KEY);
        placesClient = Places.createClient(this);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this); // Construct a FusedLocationProviderClient

        // Build the map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Saves the state of the map when the activity is paused
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (map != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, map.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, lastKnownLocation);
        }
        super.onSaveInstanceState(outState);
    }

    /**
     * Sets up the options menu
     * @param menu  the options menu
     * @return      boolean
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.current_place_menu, menu);
        return true;
    }

    /**
     * Handles a click on the menu option to get a place.
     * @param item  the menu item to handle
     * @return      boolean
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.option_get_place) {
            showCurrentPlace();
        }
        return true;
    }

    /**
     * Manipulates the map when it's available
     * This callback is triggered when the map is ready to be used
     */
    @Override
    public void onMapReady(GoogleMap map) {
        this.map = map;
        map.setOnMapLongClickListener(this);

        // Use a custom info window adapter to handle multiple lines of text in the info window contents
        this.map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            // Return null here, so that getInfoContents() is called next
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                // Inflate the layouts for the info window, title and snippet.
                View infoWindow = getLayoutInflater().inflate(R.layout.custom_info_contents,
                        (FrameLayout) findViewById(R.id.map), false);

                TextView title = infoWindow.findViewById(R.id.title);
                title.setText(marker.getTitle());
                TextView snippet = infoWindow.findViewById(R.id.snippet);
                snippet.setText(marker.getSnippet());
                return infoWindow;
            }
        });

        getLocationPermission(); // Prompt the user for permission
        updateLocationUI(); // Turn on the My Location layer and the related control on the map
        getDeviceLocation(); // Get the current location of the device and set the position of the map
    }

    /**
     * Gets the current location of the device and positions the map's camera
     */
    private void getDeviceLocation() {
        // Get the best and most recent location of the device, which may be null in rare cases when a location is not available.
        try {
            if (locationPermissionGranted) {
                @SuppressLint("MissingPermission") Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            lastKnownLocation = task.getResult();
                            if (lastKnownLocation != null) {
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(lastKnownLocation.getLatitude(),
                                                lastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                            }
                        } else {
                            Log.i(TAG, "Current location is null, using defaults");
                            map.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
                            map.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }

    /**
     * Prompts the user for permission to use the device location
     */
    private void getLocationPermission() {
        // Request location permission, so that we can get the location of the device
        // The result of the permission request is handled by a callback, onRequestPermissionsResult
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Handles the result of the request for location permissions
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        locationPermissionGranted = false;
        if (requestCode
            == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) { // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
        updateLocationUI();
    }

    /**
     * Prompts the user to select the current place from a list of likely places, and shows the
     * current place on the map - provided the user has granted location permission
     */
    private void showCurrentPlace() {
        if (map == null) {
            return;
        }

        if (locationPermissionGranted) {
            // Use fields to define the data types to return
            List<Place.Field> placeFields = Arrays.asList(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG);

            // Use the builder to create a FindCurrentPlaceRequest
            FindCurrentPlaceRequest request = FindCurrentPlaceRequest.newInstance(placeFields);

            // Get the likely places - that is, the businesses and other points of interest that are the best match for the device's current location
            @SuppressWarnings("MissingPermission") final
            Task<FindCurrentPlaceResponse> placeResult = placesClient.findCurrentPlace(request);
            placeResult.addOnCompleteListener (new OnCompleteListener<FindCurrentPlaceResponse>() {
                @Override
                public void onComplete(@NonNull Task<FindCurrentPlaceResponse> task) {
                    if (task.isSuccessful() && task.getResult() != null) {
                        FindCurrentPlaceResponse likelyPlaces = task.getResult();

                        int count; // Set the count, handling cases where less than 5 entries are returned
                        if (likelyPlaces.getPlaceLikelihoods().size() < M_MAX_ENTRIES) {
                            count = likelyPlaces.getPlaceLikelihoods().size();
                        } else {
                            count = M_MAX_ENTRIES;
                        }

                        int i = 0;
                        likelyPlaceNames = new String[count];
                        likelyPlaceAddresses = new String[count];
                        likelyPlaceAttributions = new List[count];
                        likelyPlaceLatLngs = new LatLng[count];

                        for (PlaceLikelihood placeLikelihood : likelyPlaces.getPlaceLikelihoods()) {
                            // Build a list of likely places to show the user
                            likelyPlaceNames[i] = placeLikelihood.getPlace().getName();
                            likelyPlaceAddresses[i] = placeLikelihood.getPlace().getAddress();
                            likelyPlaceAttributions[i] = placeLikelihood.getPlace()
                                    .getAttributions();
                            likelyPlaceLatLngs[i] = placeLikelihood.getPlace().getLatLng();

                            i++;
                            if (i > (count - 1)) {
                                break;
                            }
                        }

                        // Show a dialog offering the user the list of likely places, and add a marker at the selected place
                        MapsActivityCurrentPlace.this.openPlacesDialog();
                    }
                    else {
                        Log.e(TAG, "Exception: %s", task.getException());
                    }
                }
            });
        } else { // The user has not granted permission
            Log.i(TAG, "The user did not grant location permission");

            // Add a default marker, because the user hasn't selected a place
            myAddMarker(defaultLocation, getString(R.string.default_info_title), getString(R.string.default_info_snippet),
                    BitmapDescriptorFactory.HUE_CYAN);

            getLocationPermission(); // Prompt the user for permission
        }
    }

    /**
     * Displays a form allowing the user to select a place from a list of likely places
     */
    private void openPlacesDialog() {
        // Ask the user to choose the place where they are now
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // The "which" argument contains the position of the selected item
                LatLng markerLatLng = likelyPlaceLatLngs[which];
                String markerSnippet = likelyPlaceAddresses[which];
                if (likelyPlaceAttributions[which] != null) {
                    markerSnippet = markerSnippet + "\n" + likelyPlaceAttributions[which];
                }

                // Add a marker for the selected place, with an info window showing information about that place
                myAddMarker(markerLatLng, likelyPlaceNames[which], markerSnippet, BitmapDescriptorFactory.HUE_CYAN);
            }
        };

        // Display the dialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.pick_place)
                .setItems(likelyPlaceNames, listener)
                .show();
    }

    /**
     * Updates the map's UI settings based on whether the user has granted location permission
     */
    @SuppressLint("MissingPermission")
    private void updateLocationUI() {
        if (map == null) {
            return;
        }
        try {
            if (locationPermissionGranted) {
                map.setMyLocationEnabled(true);
                map.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                map.setMyLocationEnabled(false);
                map.getUiSettings().setMyLocationButtonEnabled(false);
                lastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    /**
     * When the user long-taps the map, add a popup for the user to select which concern they have at that point
     * @param point     the point where the user long-tapped the map and where the marker will be added
     */
    @Override
    public void onMapLongClick(LatLng point) {
        Log.i(TAG, "The user long-tapped the select a concern");
        currentPoint = point;
        addPopup(findViewById(android.R.id.content));
    }

    /**
     * Add popup to screen
     * @param view      the body the popup is added to
     */
    // https://stackoverflow.com/questions/5944987/how-to-create-a-popup-window-popupwindow-in-android
    private void addPopup(View view) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popup = inflater.inflate(R.layout.popup, null);
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        popupWindow = new PopupWindow(popup, width, height, true);
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
        popup.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                popupWindow.dismiss();
                Log.i(TAG, "No concern has been selected, so no marker was placed");
                return true;
            }
        });
        Log.i(TAG, "The popup for the user to select their concern has appeared");
    }

    /**
     * When the user clicks a button from the popup, place the appropriate marker
     * @param view      the button
     */
    // https://stackoverflow.com/questions/5706942/possibility-to-add-parameters-in-button-xml
    public void onButtonClick(View view) {
        String concern = view.getTag().toString();
        myAddMarker(currentPoint, concern, "", (Float) CONCERNS_DICT.get(concern)[0]);
        popupWindow.dismiss();
        Log.i(TAG, "A marker for " + concern + " has been placed");
    }

    /**
     * Add a specified marker to the map and move camera to it
     * @param point     the point where the marker will be added
     * @param title     the title of the marker
     * @param snippet   the description that will accompany the marker
     * @param color     the color of the marker
     */
    private void myAddMarker(LatLng point, String title, String snippet, float color) {
        map.setOnInfoWindowClickListener(this);
        map.addMarker(new MarkerOptions()
                .position(point)
                .title(title)
                .snippet(snippet)
                .icon(BitmapDescriptorFactory.defaultMarker(color)));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(point, DEFAULT_ZOOM));
    }

    /**
     * When the user clicks the info window of a marker, open the url associated with its concern
     * @param marker    the Marker object that was clicked
     */
    public void onInfoWindowClick(Marker marker) {
        if (CONCERNS_DICT.containsKey(marker.getTitle())) {
            String url = (String) CONCERNS_DICT.get(marker.getTitle())[1];
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
            Log.i(TAG, "The info window of a marker for " + marker.getTitle() + " was clicked, so " + url + " was opened");
        } else {
            Log.i(TAG, "The info window of a marker not for a concern was clicked, so nothing happened");
        }
    }
}