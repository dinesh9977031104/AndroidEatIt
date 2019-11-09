package com.patidar.dinesh.androideatit;

import android.content.Context;
import android.graphics.Color;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import android.widget.RelativeLayout;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.patidar.dinesh.androideatit.Common.Common;
import com.patidar.dinesh.androideatit.Database.Database;
import com.patidar.dinesh.androideatit.Helper.RecyclerItemTouchHelper;
import com.patidar.dinesh.androideatit.Interface.RecyclerItemTouchHelperListener;
import com.patidar.dinesh.androideatit.Model.Favorits;
import com.patidar.dinesh.androideatit.Model.Order;
import com.patidar.dinesh.androideatit.ViewHolder.FavoritesAdapter;
import com.patidar.dinesh.androideatit.ViewHolder.FavoritesViewHolder;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class FavoritesActivity extends AppCompatActivity implements RecyclerItemTouchHelperListener {

    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;

   FavoritesAdapter adapter;
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
        setContentView(R.layout.activity_favorites);

        rootLayout = (RelativeLayout)findViewById(R.id.root_layout);

        recyclerView = (RecyclerView)findViewById(R.id.recycler_fav);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);



        //swipe  to delete
        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new RecyclerItemTouchHelper(0,ItemTouchHelper.LEFT,this);
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView);

        loadFavorites();
    }

    private void loadFavorites() {
        adapter = new FavoritesAdapter(this,new Database(this).getAllFavorites(Common.currentUser.getPhone()));
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if (viewHolder instanceof FavoritesViewHolder)
        {
            String name = ((FavoritesAdapter)recyclerView.getAdapter()).getItem(position).getFoodName();

            final Favorits deleteItem = ((FavoritesAdapter)recyclerView.getAdapter()).getItem(viewHolder.getAdapterPosition());

            final int deleteIndex = viewHolder.getAdapterPosition();

            adapter.removeItem(viewHolder.getAdapterPosition());
            new Database(getBaseContext()).removeFromFavorites(deleteItem.getFoodId(),Common.currentUser.getPhone());

            //make snakbar
            Snackbar snackbar = Snackbar.make(rootLayout,name + "removed form cart!",Snackbar.LENGTH_LONG);
            snackbar.setAction("UNDO", new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    adapter.restoreItem(deleteItem,deleteIndex);
                    new Database(getBaseContext()).addToFavorites(deleteItem);


                }
            });
            snackbar.setActionTextColor(Color.YELLOW);
            snackbar.show();
        }
    }
}
