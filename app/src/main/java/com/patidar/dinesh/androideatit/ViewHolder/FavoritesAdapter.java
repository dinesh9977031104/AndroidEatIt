package com.patidar.dinesh.androideatit.ViewHolder;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.patidar.dinesh.androideatit.Common.Common;
import com.patidar.dinesh.androideatit.Database.Database;
import com.patidar.dinesh.androideatit.FoodDetail;
import com.patidar.dinesh.androideatit.FoodList;
import com.patidar.dinesh.androideatit.Interface.ItemClickListener;
import com.patidar.dinesh.androideatit.Model.Favorits;
import com.patidar.dinesh.androideatit.Model.Food;
import com.patidar.dinesh.androideatit.Model.Order;
import com.patidar.dinesh.androideatit.R;
import com.squareup.picasso.Picasso;

import java.util.List;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesViewHolder> {

    private Context context;
    private List<Favorits> favoritsList;

    public FavoritesAdapter(Context context, List<Favorits> favoritsList) {
        this.context = context;
        this.favoritsList = favoritsList;
    }

    @NonNull
    @Override
    public FavoritesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context)
                .inflate(R.layout.favorites_item,parent,false);
        return new FavoritesViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoritesViewHolder viewHolder, final int position) {

        viewHolder.food_name.setText(favoritsList.get(position).getFoodName());
        viewHolder.food_price.setText(String.format("$ %s",favoritsList.get(position).getFoodPrice().toString()));
        Picasso.with(context).load(favoritsList.get(position).getFoodImage())
                .into(viewHolder.food_image);

        //quick cart
        viewHolder.quick_cart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isExists = new Database(context).checkFoodExists(favoritsList.get(position).getFoodId(),Common.currentUser.getPhone());
                if (!isExists) {
                    new Database(context).addToCart(new Order(
                            Common.currentUser.getPhone(),
                            favoritsList.get(position).getFoodId(),
                            favoritsList.get(position).getFoodName(),
                            "1",
                            favoritsList.get(position).getFoodPrice(),
                            favoritsList.get(position).getFoodDiscount(),
                            favoritsList.get(position).getFoodImage()
                    ));


                } else {
                    new Database(context).increaseCart(Common.currentUser.getPhone(), favoritsList.get(position).getFoodId());
                }
                Toast.makeText(context, "Added to Cart", Toast.LENGTH_SHORT).show();
            }
        });


        final Favorits local = favoritsList.get(position);
        viewHolder.setItemClickListener(new ItemClickListener() {
            @Override
            public void onClick(View view, int position, boolean isLongClick) {

                // start new activity
                Intent foodDetail = new Intent(context,FoodDetail.class);
                foodDetail.putExtra("FoodId",favoritsList.get(position).getFoodId()); // send food id to new activity
                context.startActivity(foodDetail);
            }
        });

    }

    @Override
    public int getItemCount() {
        return favoritsList.size();
    }

    public void removeItem(int position)
    {
        favoritsList.remove(position);
        notifyItemRemoved(position);
    }

    public void restoreItem(Favorits item, int position)
    {
        favoritsList.add(position,item);
        notifyItemInserted(position);
    }

    public Favorits getItem(int position)
    {
        return favoritsList.get(position);
    }
}
