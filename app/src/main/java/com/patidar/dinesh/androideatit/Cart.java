package com.patidar.dinesh.androideatit;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.patidar.dinesh.androideatit.Common.Common;
import com.patidar.dinesh.androideatit.Database.Database;
import com.patidar.dinesh.androideatit.Helper.RecyclerItemTouchHelper;
import com.patidar.dinesh.androideatit.Interface.RecyclerItemTouchHelperListener;
import com.patidar.dinesh.androideatit.Model.DataMessage;
import com.patidar.dinesh.androideatit.Model.MyResponse;
import com.patidar.dinesh.androideatit.Model.Order;
import com.patidar.dinesh.androideatit.Model.Request;
import com.patidar.dinesh.androideatit.Model.Token;
import com.patidar.dinesh.androideatit.Remote.APIService;
import com.patidar.dinesh.androideatit.Remote.IGoogleService;
import com.patidar.dinesh.androideatit.ViewHolder.CartAdapter;
import com.patidar.dinesh.androideatit.ViewHolder.CartViewHolder;
import com.rengwuxian.materialedittext.MaterialEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class Cart extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, RecyclerItemTouchHelperListener {

    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;

    FirebaseDatabase database;
    DatabaseReference requests;

    public TextView txtTotalPrice;
    Button btnPlace;

    List<Order> cart = new ArrayList<>();
    CartAdapter adapter;

    APIService mService;

    Place shippingAddress;

    // location
    private LocationRequest mLocationRequests;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;


    private static final int UPDATE_INTERVAL = 5000;
    private static final int FATEST_INTERVAL = 3000;
    private static final int DISPLASMENT = 10;

    private static final int LOCATION_REQUEST_CODE = 9999;
    private static final int PLAY_SERVICES_REQUEST = 9997;

    //declear Google maps
    IGoogleService mGoogleMapService;

    String address;

    RelativeLayout rootLayout;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //note: add this code before setcontent view
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/NABILA.TTF")
                .setFontAttrId(R.attr.fontPath)
                .build());

        setContentView(R.layout.activity_cart);

        //init
        mGoogleMapService = Common.getGoogleMapAPI();

        rootLayout = (RelativeLayout) findViewById(R.id.rootLayout);

        //runtime permisions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, LOCATION_REQUEST_CODE);
        } else {
            if (checkPlayServices()) // if have play services in device
            {
                buildGoogleApiClient();
                createLocationRequest();
            }
        }

        mService = Common.getFCMService();

        // Firebase
        database = FirebaseDatabase.getInstance();
       // requests = database.getReference("Requests");
        requests = database.getReference("Restaurants").child(Common.restautantSelected).child("Requests");

        //init
        recyclerView = (RecyclerView) findViewById(R.id.listCart);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        //swipe  to delete
        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new RecyclerItemTouchHelper(0, ItemTouchHelper.LEFT, this);
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView);

        txtTotalPrice = (TextView) findViewById(R.id.total);
        btnPlace = (Button) findViewById(R.id.btnPlaceOrder);

        btnPlace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Create new request
                if (cart.size() > 0)
                    showAlertDialog();
                else {
                    Toast.makeText(Cart.this, "Your cart is empty!!!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        loadListFood();
    }

    private void createLocationRequest() {
        mLocationRequests = new LocationRequest();
        mLocationRequests.setInterval(UPDATE_INTERVAL);
        mLocationRequests.setFastestInterval(FATEST_INTERVAL);
        mLocationRequests.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequests.setSmallestDisplacement(DISPLASMENT);
    }

    private synchronized void buildGoogleApiClient() {

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();

        mGoogleApiClient.connect();
    }

    private boolean checkPlayServices() {
        int requestCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (requestCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(requestCode))
                GooglePlayServicesUtil.getErrorDialog(requestCode, this, PLAY_SERVICES_REQUEST).show();
            else {
                Toast.makeText(this, "This device is not supported", Toast.LENGTH_SHORT).show();
                finish();
            }
            return false;
        }
        return true;
    }

    private void showAlertDialog() {

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(Cart.this);
        alertDialog.setTitle("One more step!");
        alertDialog.setMessage("Enter your address: ");

        LayoutInflater inflater = this.getLayoutInflater();
        View order_address_comment = inflater.inflate(R.layout.order_address_comment, null);

         final MaterialEditText edtAddress = (MaterialEditText)order_address_comment.findViewById(R.id.edtAddress);
        /*final PlaceAutocompleteFragment edtAddress = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        //hide search icon before fragment
        edtAddress.getView().findViewById(R.id.place_autocomplete_search_button).setVisibility(View.GONE);
        // set hint for autocomplate edttext
        ((EditText) edtAddress.getView().findViewById(R.id.place_autocomplete_search_input))
                .setHint("Enter Your Address");
        //set text size
        ((EditText) edtAddress.getView().findViewById(R.id.place_autocomplete_search_input))
                .setTextSize(14);
        //get address from place autocomplage
        edtAddress.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                shippingAddress = place;
            }

            @Override
            public void onError(Status status) {

                Log.e("ERROR", status.getStatusMessage());
            }
        });*/


        final MaterialEditText edtComment = (MaterialEditText) order_address_comment.findViewById(R.id.edtComment);

        //radio buttons
        /*final RadioButton rdiShipToAddress = (RadioButton) order_address_comment.findViewById(R.id.rdiShioToAddress);
        final RadioButton rdiHomeAddress = (RadioButton) order_address_comment.findViewById(R.id.rdiHomeAddress);

        final RadioButton rdiCOD = (RadioButton) order_address_comment.findViewById(R.id.rdiCOD);
        final RadioButton rdiPaypal = (RadioButton) order_address_comment.findViewById(R.id.rdiPaypal);*/

        //event raido
       /* rdiHomeAddress.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    if (Common.currentUser.getHomeAddress() != null ||
                            !TextUtils.isEmpty(Common.currentUser.getHomeAddress())) {
                        address = Common.currentUser.getHomeAddress();
                        ((EditText) edtAddress.getView().findViewById(R.id.place_autocomplete_search_input))
                                .setText(address);
                    } else {
                        Toast.makeText(Cart.this, "Please Update your home address", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        rdiShipToAddress.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                //ship to this address featers
                if (b)//b=true
                {
                    mGoogleMapService.getAddressName(String.format("https://maps.googleapis.com/maps/api/geocode/json?latlng=%f,%f&sensor=false",
                            mLastLocation.getLatitude(),
                            mLastLocation.getLongitude()))
                            .enqueue(new Callback<String>() {
                                @Override
                                public void onResponse(Call<String> call, Response<String> response) {
                                    //if fatch api ok
                                    try {
                                        JSONObject jsonObject = new JSONObject(response.body().toString());

                                        JSONArray resultArray = jsonObject.getJSONArray("results");

                                        JSONObject firstObject = resultArray.getJSONObject(0);

                                        address = firstObject.getString("formatted_address");
                                        ((EditText) edtAddress.getView().findViewById(R.id.place_autocomplete_search_input))
                                                .setText(address);

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void onFailure(Call<String> call, Throwable t) {

                                    Toast.makeText(Cart.this, "" + t.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                }
            }
        });*/

        alertDialog.setView(order_address_comment);

        alertDialog.setIcon(R.drawable.ic_shopping_cart_black_24dp);

        alertDialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

               // if (!rdiShipToAddress.isChecked() && !rdiHomeAddress.isChecked()) {
                    /*if (shippingAddress != null)
                        address = shippingAddress.getAddress().toString();*/
                if (edtAddress != null)
                    address = edtAddress.getText().toString();
                    else {
                        Toast.makeText(Cart.this, "Please Select Address or select option address ", Toast.LENGTH_SHORT).show();
                        //remove fragment
                       /* getFragmentManager().beginTransaction()
                                .remove(getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment))
                                .commit();*/

                        return;
                    }
                //}

                if (TextUtils.isEmpty(address)) {
                    Toast.makeText(Cart.this, "Please Select Address/select option address ", Toast.LENGTH_SHORT).show();
                    //remove fragment
                   /* getFragmentManager().beginTransaction()
                            .remove(getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment))
                            .commit();*/

                    return;
                }

               /* if (!rdiCOD.isChecked() && !rdiCOD.isChecked()) {
                    Toast.makeText(Cart.this, "Please Select Payment options", Toast.LENGTH_SHORT).show();
                    //remove fragment
                    getFragmentManager().beginTransaction()
                            .remove(getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment))
                            .commit();

                    return;
                } else if (rdiCOD.isChecked()) {
                    // create new request
                    Request request = new Request(
                            Common.currentUser.getPhone(),
                            Common.currentUser.getName(),
                            // edtAddress.getText().toString(),
                            shippingAddress.getAddress().toString(),
                            txtTotalPrice.getText().toString(),
                            "0", //status
                            edtComment.getText().toString(),
                           // "COD",
                           // String.format("%s,%s", mLastLocation.getLongitude(), mLastLocation.getLongitude()),
                            cart
                    );
                    // Submit to firebase
                    // we will using system.currentMilli to key

                    String order_number = String.valueOf(System.currentTimeMillis());

                    requests.child(order_number)
                            .setValue(request);

                    // delete cart
                    new Database(getBaseContext()).cleanCart(Common.currentUser.getPhone());

                    sendNotificationOrder(order_number);

                    //  Toast.makeText(Cart.this, "Thank you, Order Place", Toast.LENGTH_SHORT).show();
                    // finish();
                }*/

                // create new request
                Request request = new Request(
                        Common.currentUser.getPhone(),
                        Common.currentUser.getName(),
                         edtAddress.getText().toString(),
                       // shippingAddress.getAddress().toString(),
                        txtTotalPrice.getText().toString(),
                        "0", //status
                        edtComment.getText().toString(),
                        //"Paypal",
                       // String.format("%s,%s", shippingAddress.getLatLng().latitude, shippingAddress.getLatLng().longitude),
                        Common.restautantSelected,
                        cart
                );
                // Submit to firebase
                // we will using system.currentMilli to key

                String order_number = String.valueOf(System.currentTimeMillis());

                requests.child(order_number)
                        .setValue(request);

                // delete cart
                new Database(getBaseContext()).cleanCart(Common.currentUser.getPhone());

                sendNotificationOrder(order_number);

                //  Toast.makeText(Cart.this, "Thank you, Order Place", Toast.LENGTH_SHORT).show();
                // finish();
                //remove fragment
               /* getFragmentManager().beginTransaction()
                        .remove(getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment))
                        .commit();*/
            }

        });

        alertDialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                dialogInterface.dismiss();
                //remove fragment
               /* getFragmentManager().beginTransaction()
                        .remove(getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment))
                        .commit();*/
            }
        });

        alertDialog.show();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case LOCATION_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (checkPlayServices()) // if have play services in device
                    {
                        buildGoogleApiClient();
                        createLocationRequest();
                    }
                }
            }
            break;
        }
    }

    private void sendNotificationOrder(final String order_number) {
        DatabaseReference tokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query data = tokens.orderByChild("serverToken").equalTo(true); // get all node with isserver token is true
        data.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    Token serverToken = postSnapshot.getValue(Token.class);

                    // create raw payload to send
                   /*Notification notification = new Notification("Dinesh Patidar","You have new order"+order_number);
                   Sender content = new Sender(serverToken.getToken(),notification);*/

                    Map<String, String> dataSend = new HashMap<>();
                    dataSend.put("title", "Dinesh Patidar");
                    dataSend.put("message", "You have new order" + order_number);
                    DataMessage dataMessage = new DataMessage(serverToken.getToken(), dataSend);

                    mService.sendNotification(dataMessage)
                            .enqueue(new Callback<MyResponse>() {
                                @Override
                                public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {

                                    //only run when get result
                                    if (response.code() == 200) {

                                        if (response.body().success == 1) {
                                            Toast.makeText(Cart.this, "Thank you, Order Place", Toast.LENGTH_SHORT).show();
                                            finish();
                                        } else {
                                            Toast.makeText(Cart.this, "Failed !!!", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(Call<MyResponse> call, Throwable t) {

                                    Log.e("ERROR ", t.getMessage());
                                }
                            });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void loadListFood() {

        cart = new Database(this).getCarts(Common.currentUser.getPhone());
        adapter = new CartAdapter(cart, this);
        adapter.notifyDataSetChanged();
        recyclerView.setAdapter(adapter);

        // Calculate total price
        int total = 0;
        for (Order order : cart)
            total += (Integer.parseInt(order.getPrice())) * (Integer.parseInt(order.getQuantity()));

        Locale locale = new Locale("en", "US");
        NumberFormat fmt = NumberFormat.getCurrencyInstance(locale);
        txtTotalPrice.setText(fmt.format(total));

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getTitle().equals(Common.DELETE))
            deleteCart(item.getOrder());
        return true;
    }

    private void deleteCart(int position) {
        //we will remov item at List<Order> by position
        cart.remove(position);
        //after that we will delete all old data from sqlite
        new Database(this).cleanCart(Common.currentUser.getPhone());
        //and final we will update new data from list<order> to sqlite
        for (Order item : cart)
            new Database(this).addToCart(item);
        //refresh
        loadListFood();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        displayLocation();
        startLocationUpdate();
    }

    private void startLocationUpdate() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequests, this);

    }

    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            Log.d("Location", "your last location :" + mLastLocation.getLatitude() + "," + mLastLocation.getLongitude());
        } else {
            Log.d("Location", "could not get your locations");
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        displayLocation();
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if (viewHolder instanceof CartViewHolder) {
            String name = ((CartAdapter) recyclerView.getAdapter()).getItem(viewHolder.getAdapterPosition()).getProductName();

            final Order deleteItem = ((CartAdapter) recyclerView.getAdapter()).getItem(viewHolder.getAdapterPosition());

            final int deleteIndex = viewHolder.getAdapterPosition();

            adapter.removeItem(deleteIndex);
            new Database(getBaseContext()).removeFromCart(deleteItem.getProductId(), Common.currentUser.getPhone());


            //update total amount
            // Calculate total price
            List<Order> orders = new Database(getBaseContext()).getCarts(Common.currentUser.getPhone());
            int total = 0;
            for (Order item : orders)
                // total += (Integer.parseInt(order.getPrice())) * (Integer.parseInt(item.getQuantity()));
                // total += (Integer.parseInt(item.getPrice())) * (Integer.parseInt(item.getQuantity()));
                total += (Integer.parseInt(item.getPrice())) * (Integer.parseInt(item.getQuantity()));

            Locale locale = new Locale("en", "US");
            NumberFormat fmt = NumberFormat.getCurrencyInstance(locale);
            txtTotalPrice.setText(fmt.format(total));

            //make snakbar
            Snackbar snackbar = Snackbar.make(rootLayout, name + "removed form cart!", Snackbar.LENGTH_LONG);
            snackbar.setAction("UNDO", new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    adapter.restoreItem(deleteItem, deleteIndex);
                    new Database(getBaseContext()).addToCart(deleteItem);

                    //update total amount
                    // Calculate total price
                    List<Order> orders = new Database(getBaseContext()).getCarts(Common.currentUser.getPhone());
                    int total = 0;
                    for (Order item : orders)
                        // total += (Integer.parseInt(order.getPrice())) * (Integer.parseInt(item.getQuantity()));
                        // total += (Integer.parseInt(item.getPrice())) * (Integer.parseInt(item.getQuantity()));
                        total += (Integer.parseInt(item.getPrice())) * (Integer.parseInt(item.getQuantity()));

                    Locale locale = new Locale("en", "US");
                    NumberFormat fmt = NumberFormat.getCurrencyInstance(locale);
                    txtTotalPrice.setText(fmt.format(total));
                }
            });
            snackbar.setActionTextColor(Color.YELLOW);
            snackbar.show();
        }
    }
}
