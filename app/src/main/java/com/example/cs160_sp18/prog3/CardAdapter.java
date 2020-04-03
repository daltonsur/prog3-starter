package com.example.cs160_sp18.prog3;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class CardAdapter extends RecyclerView.Adapter {

    private Context context;
    private ArrayList<BearCard> cards;
    private String username;

    public CardAdapter(Context context, ArrayList<BearCard> cards, String username) {
        this.context = context;
        this.cards = cards;
        this.username = username;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.bear_cell_layout, parent, false);
        return new CardViewHolder(view, context, username);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        BearCard card = cards.get(position);
        ((CardViewHolder) holder).bind(card);
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }
}

class CardViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    public RelativeLayout layout;
    public TextView name;
    public ImageView image;
    public TextView distance;
    public Context context;
    public String username;

    public void onClick(View view) {
        TextView name = view.findViewById(R.id.bear_location);
        String location_name = name.getText().toString();
        Intent commentIntent = new Intent(context, CommentFeedActivity.class);
        commentIntent.putExtra("locationName", location_name);
        commentIntent.putExtra("Username", username);
        context.startActivity(commentIntent);
    }

    public CardViewHolder(View itemView, Context context, String username) {
        super(itemView);
        this.context = context;
        this.username = username;
        layout = (RelativeLayout) itemView.findViewById(R.id.bear_layout);
        layout.setOnClickListener(this);
        name = layout.findViewById(R.id.bear_location);
        image = layout.findViewById(R.id.bear_image);
        distance = layout.findViewById(R.id.bear_distance);
    }

    void bind(BearCard card) {
        name.setText(card.name);
        distance.setText(card.distanceString());
        image.setImageURI(null);
        image.setImageURI(card.image);
        if (card.distance > 10) {
            name.setTextColor(Color.RED);
            layout.setClickable(false);
        } else {
            name.setTextColor(Color.GREEN);
            layout.setClickable(true);
        }
    }
}