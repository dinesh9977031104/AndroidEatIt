package com.patidar.dinesh.androideatit;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.JsonArray;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.patidar.dinesh.androideatit.Common.Common;
import com.patidar.dinesh.androideatit.Helper.DirectionJSONParser;
import com.patidar.dinesh.androideatit.Model.Request;
import com.patidar.dinesh.androideatit.Model.ShippingInformation;
import com.patidar.dinesh.androideatit.Remote.IGoogleService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import dmax.dialog.SpotsDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TrakingOrder extends FragmentActivity implements OnMapReadyCallback,ValueEventListener {

    private GoogleMap mMap;

    FirebaseDatabase database;
    DatabaseReference requests, shippingOrder;

    Request currentOrder;

    IGoogleService mService;

    Marker shippingMarker;

    Polyline polyline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_traking_order);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        database = FirebaseDatabase.getInstance();
        requests = database.getReference("Requests");
        shippingOrder = database.getReference("ShippingOrders");

        shippingOrder.addValueEventListener(this);

        mService = Common.getGoogleMapAPI();
    }

    @Override
    protected void onStop() {
        shippingOrder.removeEventListener(this);
        super.onStop();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        trakingLocation();
    }

    private void trakingLocation() {

        requests.child(Common.currentKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        currentOrder = dataSnapshot.getValue(Request.class);
                        if (currentOrder.getAddress() != null && !currentOrder.getAddress().isEmpty())
                        {
                            mService.getLocationFromAddress(new StringBuilder("https://maps.googleapis.com/maps/api/geocode/json?address=")
                            .append(currentOrder.getAddress()).toString())
                                    .enqueue(new Callback<String>() {
                                        @Override
                                        public void onResponse(Call<String> call, Response<String> response) {
                                            try {
                                                JSONObject jsonObject = new JSONObject(response.body());

                                                String lat = ((JSONArray)jsonObject.get("results"))
                                                        .getJSONObject(0)
                                                        .getJSONObject("geometry")
                                                        .getJSONObject("location")
                                                        .get("lat").toString();

                                                String lng = ((JSONArray)jsonObject.get("results"))
                                                        .getJSONObject(0)
                                                        .getJSONObject("geometry")
                                                        .getJSONObject("location")
                                                        .get("lng").toString();

                                                final LatLng location = new LatLng(Double.parseDouble(lat),
                                                        Double.parseDouble(lng));

                                                mMap.addMarker(new MarkerOptions().position(location)
                                                .title("Order Destination")
                                                .icon(BitmapDescriptorFactory.defaultMarker()));

                                                shippingOrder.child(Common.currentKey)
                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(DataSnapshot dataSnapshot) {

                                                        ShippingInformation shippingInformation = dataSnapshot.getValue(ShippingInformation.class);

                                                        LatLng shipperLocation = new LatLng(shippingInformation.getLat(),shippingInformation.getLng());

                                                        if (shippingMarker == null)
                                                        {
                                                            shippingMarker = mMap.addMarker(
                                                                    new MarkerOptions()
                                                                    .position(shipperLocation)
                                                                    .title("shipper #"+shippingInformation.getOrderId())
                                                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                                                            );
                                                        }
                                                        else
                                                        {
                                                            shippingMarker.setPosition(shipperLocation);
                                                        }

                                                        CameraPosition cameraPosition = new CameraPosition.Builder()
                                                                .target(shipperLocation)
                                                                .zoom(16)
                                                                .bearing(0)
                                                                .tilt(45)
                                                                .build();

                                                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                                                        //draw route
                                                        if (polyline != null)
                                                            polyline.remove();
                                                        mService.getDirections(shipperLocation.latitude+","+shipperLocation.longitude,
                                                                currentOrder.getAddress())
                                                                .enqueue(new Callback<String>() {
                                                                    @Override
                                                                    public void onResponse(Call<String> call, Response<String> response) {
                                                                        new ParseTask().execute(response.body().toString());
                                                                    }

                                                                    @Override
                                                                    public void onFailure(Call<String> call, Throwable t) {

                                                                    }
                                                                });
                                                    }

                                                    @Override
                                                    public void onCancelled(DatabaseError databaseError) {

                                                    }
                                                });

                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<String> call, Throwable t) {

                                        }
                                    });
                        }
                        /*else if (currentOrder.getLatLng() != null && !currentOrder.getLatLng().isEmpty())
                        {
                            mService.getLocationFromAddress(new StringBuilder("https://maps.googleapis.com/maps/api/geocode/json?latlng=")
                                    .append(currentOrder.getLatLng()).toString())
                                    .enqueue(new Callback<String>() {
                                        @Override
                                        public void onResponse(Call<String> call, Response<String> response) {
                                            try {
                                                JSONObject jsonObject = new JSONObject(response.body());

                                                String lat = ((JSONArray)jsonObject.get("results"))
                                                        .getJSONObject(0)
                                                        .getJSONObject("geometry")
                                                        .getJSONObject("location")
                                                        .get("lat").toString();

                                                String lng = ((JSONArray)jsonObject.get("results"))
                                                        .getJSONObject(0)
                                                        .getJSONObject("geometry")
                                                        .getJSONObject("location")
                                                        .get("lng").toString();

                                                final LatLng location = new LatLng(Double.parseDouble(lat),
                                                        Double.parseDouble(lng));

                                                mMap.addMarker(new MarkerOptions().position(location)
                                                        .title("Order Destination")
                                                        .icon(BitmapDescriptorFactory.defaultMarker()));

                                                shippingOrder.child(Common.currentKey)
                                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(DataSnapshot dataSnapshot) {

                                                                ShippingInformation shippingInformation = dataSnapshot.getValue(ShippingInformation.class);

                                                                LatLng shipperLocation = new LatLng(shippingInformation.getLat(),shippingInformation.getLng());

                                                                if (shippingMarker == null)
                                                                {
                                                                    shippingMarker = mMap.addMarker(
                                                                            new MarkerOptions()
                                                                                    .position(shipperLocation)
                                                                                    .title("shipper #"+shippingInformation.getOrderId())
                                                                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                                                                    );
                                                                }
                                                                else
                                                                {
                                                                    shippingMarker.setPosition(shipperLocation);
                                                                }

                                                                CameraPosition cameraPosition = new CameraPosition.Builder()
                                                                        .target(shipperLocation)
                                                                        .zoom(16)
                                                                        .bearing(0)
                                                                        .tilt(45)
                                                                        .build();

                                                                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                                                                //draw route
                                                                if (polyline != null)
                                                                    polyline.remove();
                                                                mService.getDirections(shipperLocation.latitude+","+shipperLocation.longitude,
                                                                        currentOrder.getLatLng())
                                                                        .enqueue(new Callback<String>() {
                                                                            @Override
                                                                            public void onResponse(Call<String> call, Response<String> response) {
                                                                                new ParseTask().execute(response.body().toString());
                                                                            }

                                                                            @Override
                                                                            public void onFailure(Call<String> call, Throwable t) {

                                                                            }
                                                                        });
                                                            }

                                                            @Override
                                                            public void onCancelled(DatabaseError databaseError) {

                                                            }
                                                        });

                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<String> call, Throwable t) {

                                        }
                                    });
                        }*/
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        trakingLocation();
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {

    }

    private class ParseTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        AlertDialog mDialog = new SpotsDialog(TrakingOrder.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mDialog.show();
            mDialog.setMessage("Please waiting...");
        }

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... strings) {
            JSONObject jsonObject;
            List<List<HashMap<String,String>>> routes = null;

            try {
                jsonObject = new JSONObject(strings[0]);

                DirectionJSONParser parser = new DirectionJSONParser();

                routes = parser.parse(jsonObject);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> lists) {
            mDialog.dismiss();

            ArrayList points = null;
            PolylineOptions lineOptions = null;

            for (int i = 0; i < lists.size(); i++)
            {
                points = new ArrayList();
                lineOptions = new PolylineOptions();

                List<HashMap<String,String>> path = lists.get(i);
                for ( int j=0; j<path.size();j++){
                    HashMap<String,String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));

                    LatLng position = new LatLng(lat,lng);
                    points.add(position);
                }
                lineOptions.addAll(points);
                lineOptions.width(12);
                lineOptions.color(Color.BLUE);
                lineOptions.geodesic(true);
            }
          polyline = mMap.addPolyline(lineOptions);
        }
    }
}
