package com.patidar.dinesh.androideatit.ViewHolder;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.patidar.dinesh.androideatit.Interface.ItemClickListener;
import com.patidar.dinesh.androideatit.R;

public class RestaurantViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    public TextView txt_restaurant_name;
    public ImageView img_restaurant;

    private ItemClickListener itemClickListener;

    public RestaurantViewHolder(@NonNull View itemView) {
        super(itemView);

        txt_restaurant_name = (TextView)itemView.findViewById(R.id.restaurant_name);
        img_restaurant = (ImageView)itemView.findViewById(R.id.restaurant_image);

        itemView.setOnClickListener(this);
    }

    public void setItemClickListener(ItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    @Override
    public void onClick(View view) {

        itemClickListener.onClick(view,getAdapterPosition(),false);
    }
}

