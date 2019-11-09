package com.patidar.dinesh.androideatit;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.patidar.dinesh.androideatit.Common.Common;
import com.patidar.dinesh.androideatit.Interface.ItemClickListener;
import com.patidar.dinesh.androideatit.Model.Category;
import com.patidar.dinesh.androideatit.Model.Restaurant;
import com.patidar.dinesh.androideatit.ViewHolder.MenuViewHolder;
import com.patidar.dinesh.androideatit.ViewHolder.RestaurantViewHolder;
import com.squareup.picasso.Picasso;

public class RestaurantList extends AppCompatActivity {

    AlertDialog waitingDialog;
    RecyclerView recyclerView;
    SwipeRefreshLayout swipeRefreshLayout;



    FirebaseRecyclerOptions<Restaurant> options = new FirebaseRecyclerOptions.Builder<Restaurant>()
            .setQuery(FirebaseDatabase.getInstance()
                    .getReference()
                    .child("Restaurants")
                    ,Restaurant.class)
            .build();

   FirebaseRecyclerAdapter<Restaurant,RestaurantViewHolder> adapter = new FirebaseRecyclerAdapter<Restaurant, RestaurantViewHolder>(options) {

        @NonNull
        @Override
        public RestaurantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.restaurant_items,parent,false);
            return  new RestaurantViewHolder(itemView);
        }


        @Override
        protected void onBindViewHolder(@NonNull RestaurantViewHolder viewHolder, int position, @NonNull Restaurant model) {

            viewHolder.txt_restaurant_name.setText(model.getName());
            Picasso.with(getBaseContext()).load(model.getImage())
                    .into(viewHolder.img_restaurant);

            final Restaurant clickItem = model;
            viewHolder.setItemClickListener(new ItemClickListener() {
                @Override
                public void onClick(View view, int position, boolean isLongClick) {

                    // Get category id and send to new activity
                    Intent foodList = new Intent(RestaurantList.this,Home.class);
                    // when usee select restorent we will save id
                    Common.restautantSelected = adapter.getRef(position).getKey();
                    startActivity(foodList);

                    //  Toast.makeText(Home.this, ""+clickItem.getName(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_list);

        //view
        swipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.swipe_layout);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorPrimary),
                getResources().getColor(android.R.color.holo_green_dark),
                getResources().getColor(android.R.color.holo_orange_dark),
                getResources().getColor(android.R.color.holo_blue_dark));

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (Common.isConnectedToInternet(getBaseContext()))
                    loadRestaurant();

                else {
                    Toast.makeText(getBaseContext(), "Please check your Internet Connection!!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //default load for first time
        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {

                if (Common.isConnectedToInternet(getBaseContext()))
                    loadRestaurant();

                else {
                    Toast.makeText(getBaseContext(), "Please check your Internet Connection!!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // load menu
        recyclerView = (RecyclerView)findViewById(R.id.recycler_restaurant);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }
    private void loadRestaurant() {

        adapter.startListening();
        recyclerView.setAdapter(adapter);
        swipeRefreshLayout.setRefreshing(false);

        //animations
        recyclerView.getAdapter().notifyDataSetChanged();
        recyclerView.scheduleLayoutAnimation();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }

    @Override
    protected void onResume() {
        loadRestaurant();
        super.onResume();
    }
}
