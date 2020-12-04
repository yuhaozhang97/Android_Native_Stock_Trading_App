package com.example.stockapp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

public class PortfolioAdapter extends RecyclerView.Adapter<PortfolioAdapter.MyViewHolder> {

    Context context;
    ArrayList<Pair<String, Float>> tickerShareList;
    Float[] curPricesList;
    Float[] priceChangesList;

    public PortfolioAdapter(Context ct, ArrayList<Pair<String, Float>> ts, Float[] cp, Float[] pc) {
        context = ct;
        tickerShareList = ts;
        curPricesList = cp;
        priceChangesList = pc;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.main_row, parent, false);

        return new PortfolioAdapter.MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        DecimalFormat df = new DecimalFormat("##0.00");

        holder.tickerView.setText(tickerShareList.get(position).getL());
        holder.sharesView.setText(String.valueOf(tickerShareList.get(position).getR()) + " shares");
        holder.priceView.setText(String.valueOf(curPricesList[position]));

        holder.changeView.setText(df.format(Math.abs(priceChangesList[position])));
        if (priceChangesList[position] < 0) {
            holder.changeView.setTextColor(context.getResources().getColor(R.color.main_red));
            holder.imageView.setImageDrawable(context.getDrawable(R.drawable.ic_baseline_trending_down_24));
        } else if (priceChangesList[position] > 0) {
            holder.changeView.setTextColor(context.getResources().getColor(R.color.main_green));
        } else {
            holder.imageView.setImageDrawable(null);
        }

        holder.constraintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, OnSearchActivity.class);
                intent.putExtra("query", tickerShareList.get(position).getL());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tickerShareList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView tickerView, sharesView, priceView, changeView;
        ImageView imageView;
        ConstraintLayout constraintLayout;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            tickerView = itemView.findViewById(R.id.main_list_ticker);
            sharesView = itemView.findViewById(R.id.main_list_shares);
            priceView = itemView.findViewById(R.id.main_list_price);
            changeView = itemView.findViewById(R.id.main_list_change_text);
            imageView = itemView.findViewById(R.id.main_list_trend_arrow);
            constraintLayout = itemView.findViewById(R.id.main_row_container);
        }
    }
}
