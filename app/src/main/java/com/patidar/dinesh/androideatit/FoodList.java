package com.patidar.dinesh.androideatit;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.ShareDialog;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.patidar.dinesh.androideatit.Common.Common;
import com.patidar.dinesh.androideatit.Database.Database;
import com.patidar.dinesh.androideatit.Interface.ItemClickListener;
import com.patidar.dinesh.androideatit.Model.Favorits;
import com.patidar.dinesh.androideatit.Model.Food;
import com.patidar.dinesh.androideatit.Model.Order;
import com.patidar.dinesh.androideatit.ViewHolder.FoodViewHolder;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.List;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class FoodList extends AppCompatActivity {

    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;

    FirebaseDatabase database;
    DatabaseReference foodList;

    String categoryId = "";
    FirebaseRecyclerAdapter<Food,FoodViewHolder> adapter;

    // Search Functionallity
    FirebaseRecyclerAdapter<Food,FoodViewHolder> searchAdapter;
    List<String> suggestList = new ArrayList<>();
    MaterialSearchBar materialSearchBar;

    //favorites
    Database localDB;

    CallbackManager callbackManager;
    ShareDialog shareDialog;

    SwipeRefreshLayout swipeRefreshLayout;

    //create target for picasso
    Target target  = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            //create photo from bitmap
            SharePhoto photo = new SharePhoto.Builder()
                    .setBitmap(bitmap)
                    .build();

            if (ShareDialog.canShow(SharePhotoContent.class))
            {
                SharePhotoContent content = new SharePhotoContent.Builder()
                        .addPhoto(photo)
                        .build();
                shareDialog.show(content);
            }
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {

        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }
    };

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
        setContentView(R.layout.activity_food_list);

        //init facebook
        callbackManager = CallbackManager.Factory.create();
        shareDialog = new ShareDialog(this);

        // Firebase
        database = FirebaseDatabase.getInstance();
       // foodList = database.getReference("Foods");
        foodList = database.getReference("Restaurants").child(Common.restautantSelected).child("detail").child("Foods");


        //local db
        localDB = new Database(this);

        swipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.swipe_layout);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorPrimary),
                getResources().getColor(android.R.color.holo_green_dark),
                getResources().getColor(android.R.color.holo_orange_dark),
                getResources().getColor(android.R.color.holo_blue_dark));

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Get intent here
                if (getIntent() != null)
                    categoryId = getIntent().getStringExtra("CategoryId");
                if (!categoryId.isEmpty() && categoryId != null)
                {
                    if (Common.isConnectedToInternet(getBaseContext()))
                        loadListFood(categoryId);
                    else {
                        Toast.makeText(FoodList.this, "Please check your Internet Connection!!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

            }
        });
        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                // Get intent here
                if (getIntent() != null)
                    categoryId = getIntent().getStringExtra("CategoryId");
                if (!categoryId.isEmpty() && categoryId != null)
                {
                    if (Common.isConnectedToInternet(getBaseContext()))
                        loadListFood(categoryId);
                    else {
                        Toast.makeText(FoodList.this, "Please check your Internet Connection!!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                // Search
                materialSearchBar = (MaterialSearchBar)findViewById(R.id.searchBar);
                materialSearchBar.setHint("Enter Your Food");
                // materialSearchBar.setSpeechMode(false); // no need, we have define in xml already
                loadSuggest(); // write function to load suggest list from firebase

                materialSearchBar.setCardViewElevation(10);
                materialSearchBar.addTextChangeListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                        // when user type we change suggest list
                        List<String> suggest = new ArrayList<String>();
                        for (String search:suggestList) // loop in suggest list
                        {
                            if (search.toLowerCase().contains(materialSearchBar.getText().toLowerCase()))
                                suggest.add(search);

                        }
                        materialSearchBar.setLastSuggestions(suggest);
                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                });
                materialSearchBar.setOnSearchActionListener(new MaterialSearchBar.OnSearchActionListener() {
                    @Override
                    public void onSearchStateChanged(boolean enabled) {
                        // when search bar is close
                        // restore original suggest adapter
                        if (!enabled)
                            recyclerView.setAdapter(adapter);
                    }

                    @Override
                    public void onSearchConfirmed(CharSequence text) {
                        // when search finish
                        // show result of search adapter
                        startSearch(text);

                    }

                    @Override
                    public void onButtonClicked(int buttonCode) {

                    }
                });
            }
        });

        recyclerView = (RecyclerView)findViewById(R.id.recycler_food);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null)
            adapter.startListening();
    }

    private void startSearch(CharSequence text) {

        //create query by name
        Query searchByName = foodList.orderByChild("name").equalTo(text.toString());
        //create options with query
        FirebaseRecyclerOptions<Food> foodOptions = new FirebaseRecyclerOptions.Builder<Food>()
                .setQuery(searchByName,Food.class)
                .build();

        searchAdapter = new FirebaseRecyclerAdapter<Food, FoodViewHolder>(foodOptions) {
            @Override
            protected void onBindViewHolder(@NonNull FoodViewHolder viewHolder, int position, @NonNull Food model) {

                viewHolder.food_name.setText(model.getName());
                Picasso.with(getBaseContext()).load(model.getImage())
                        .into(viewHolder.food_image);

                final Food local = model;
                viewHolder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {

                        // start new activity
                        Intent foodDetail = new Intent(FoodList.this,FoodDetail.class);
                        foodDetail.putExtra("FoodId",searchAdapter.getRef(position).getKey()); // send food id to new activity
                        startActivity(foodDetail);

                        // Toast.makeText(FoodList.this, ""+local.getName(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @NonNull
            @Override
            public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.food_item,parent,false);
                return  new FoodViewHolder(itemView);
            }
        };
        searchAdapter.startListening();
        recyclerView.setAdapter(searchAdapter); // set adapter for recyclerview search result
    }

    private void loadSuggest() {
        foodList.orderByChild("menuId").equalTo(categoryId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        for (DataSnapshot postSnapshot:dataSnapshot.getChildren())
                        {
                            Food item = postSnapshot.getValue(Food.class);
                            suggestList.add(item.getName()); // Add Name of food to suggest list
                        }
                        materialSearchBar.setLastSuggestions(suggestList);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    private void loadListFood(String categoryId) {

        //create query by category id
        Query searchByName = foodList.orderByChild("menuId").equalTo(categoryId);
        //create options with query
        FirebaseRecyclerOptions<Food> foodOptions = new FirebaseRecyclerOptions.Builder<Food>()
                .setQuery(searchByName,Food.class)
                .build();

        adapter = new FirebaseRecyclerAdapter<Food, FoodViewHolder>(foodOptions) {
            @Override
            protected void onBindViewHolder(@NonNull final FoodViewHolder viewHolder, final int position, @NonNull final Food model) {

                viewHolder.food_name.setText(model.getName());
                viewHolder.food_price.setText(String.format("$ %s",model.getPrice().toString()));
                Picasso.with(getBaseContext()).load(model.getImage())
                        .into(viewHolder.food_image);

                //quick cart

                    viewHolder.quick_cart.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            boolean isExists = new Database(getBaseContext()).checkFoodExists(adapter.getRef(position).getKey(),Common.currentUser.getPhone());
                            if (!isExists) {
                                new Database(getBaseContext()).addToCart(new Order(
                                        Common.currentUser.getPhone(),
                                        adapter.getRef(position).getKey(),
                                        model.getName(),
                                        "1",
                                        model.getPrice(),
                                        model.getDiscount(),
                                        model.getImage()
                                ));


                            } else {
                                new Database(getBaseContext()).increaseCart(Common.currentUser.getPhone(), adapter.getRef(position).getKey());
                            }
                            Toast.makeText(FoodList.this, "Added to Cart", Toast.LENGTH_SHORT).show();
                        }
                    });


                //add favorites
                if (localDB.isFavorites(adapter.getRef(position).getKey(),Common.currentUser.getPhone()))
                    viewHolder.fav_image.setImageResource(R.drawable.ic_favorite_black_24dp);

                //click to share
                viewHolder.share_image.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Picasso.with(getApplicationContext())
                                .load(model.getImage())
                                .into(target);
                    }
                });


                // click to change state of favorites
                viewHolder.fav_image.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        Favorits favorits = new Favorits();
                        favorits.setFoodId(adapter.getRef(position).getKey());
                        favorits.setFoodName(model.getName());
                        favorits.setFoodDescription(model.getDescription());
                        favorits.setFoodDiscount(model.getDiscount());
                        favorits.setFoodImage(model.getImage());
                        favorits.setFoodMenuId(model.getMenuId());
                        favorits.setUserPhone(Common.currentUser.getPhone());
                        favorits.setFoodPrice(model.getPrice());

                        if (!localDB.isFavorites(adapter.getRef(position).getKey(),Common.currentUser.getPhone()))
                        {
                            localDB.addToFavorites(favorits);
                            viewHolder.fav_image.setImageResource(R.drawable.ic_favorite_black_24dp);
                            Toast.makeText(FoodList.this, ""+model.getName()+"was added to favorites", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            localDB.removeFromFavorites(adapter.getRef(position).getKey(),Common.currentUser.getPhone());
                            viewHolder.fav_image.setImageResource(R.drawable.ic_favorite_border_black_24dp);
                            Toast.makeText(FoodList.this, ""+model.getName()+"was removed from favorites", Toast.LENGTH_SHORT).show();
                        }
                    }
                });


                final Food local = model;
                viewHolder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {

                        // start new activity
                        Intent foodDetail = new Intent(FoodList.this,FoodDetail.class);
                        foodDetail.putExtra("FoodId",adapter.getRef(position).getKey()); // send food id to new activity
                        startActivity(foodDetail);
                    }
                });
            }

            @NonNull
            @Override
            public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
               View itemView = LayoutInflater.from(parent.getContext())
                       .inflate(R.layout.food_item,parent,false);
               return  new FoodViewHolder(itemView);
            }
        };

        // set adapter
        adapter.startListening();
        recyclerView.setAdapter(adapter);
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null){
            adapter.stopListening();
        }
        if (searchAdapter != null) {
            searchAdapter.stopListening();
        }
    }
}
