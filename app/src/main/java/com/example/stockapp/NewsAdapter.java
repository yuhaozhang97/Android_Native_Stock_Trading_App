package com.example.stockapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.RequestCreator;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.RecyclerView;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.MyViewHolder> {

    String[] sources;
    String[] titles;
    String[] urls;
    RequestCreator[] imagesLoader;
    int[] datesAgo;
    Context context;

    public NewsAdapter(Context ct, String[] sources_input, String[] titles_input, String[] urls_input, int[] datesAgo_input, RequestCreator[] imagesLoader_input) {
        context = ct;
        sources = sources_input;
        titles = titles_input;
        urls = urls_input;
        imagesLoader = imagesLoader_input;
        datesAgo = datesAgo_input;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.news_row, parent, false);

        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        if (position == 0) {
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(holder.constraintLayout);
            constraintSet.connect(R.id.cardView, ConstraintSet.LEFT, R.id.news_container, ConstraintSet.LEFT, 0);
            constraintSet.connect(R.id.news_source, ConstraintSet.TOP, R.id.cardView, ConstraintSet.BOTTOM, 10);
            constraintSet.connect(R.id.news_title, ConstraintSet.BOTTOM, R.id.news_container, ConstraintSet.BOTTOM, 10);
            constraintSet.connect(R.id.news_title, ConstraintSet.LEFT, R.id.news_container, ConstraintSet.LEFT, 10);
            constraintSet.connect(R.id.news_title, ConstraintSet.RIGHT, R.id.news_container, ConstraintSet.RIGHT, 10);
            constraintSet.applyTo(holder.constraintLayout);
            holder.cardView.getLayoutParams().height = 820;
            holder.cardView.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
            holder.cardView.requestLayout();

            holder.imageView.getLayoutParams().height = 820;
            holder.imageView.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
            holder.imageView.requestLayout();

            holder.titleView.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
            holder.titleView.requestLayout();
        }

        holder.sourceView.setText(sources[position]);
        holder.datesAgoView.setText(datesAgo[position] + " days ago");
        holder.titleView.setText(titles[position]);
        imagesLoader[position].into(holder.imageView);

        holder.constraintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = urls[position];

                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                context.startActivity(i);
            }
        });

        holder.constraintLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                openArticleDialog(position);
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return sources.length;
    }

    private void openArticleDialog(int pos) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View promptView = layoutInflater.inflate(R.layout.article_dialog, null);
        final AlertDialog alertD = new AlertDialog.Builder(context).create();

        ImageView articleImg = (ImageView) promptView.findViewById(R.id.article_image);
        imagesLoader[pos].into(articleImg);

        TextView title = (TextView) promptView.findViewById(R.id.article_title);
        title.setText(titles[pos]);

        ImageView twitter = (ImageView) promptView.findViewById(R.id.twitter_icon);
        twitter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = "https://twitter.com/intent/tweet?text=" + titles[pos] + "&url=" + urls[pos];

                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                context.startActivity(i);

                alertD.dismiss();
            }
        });

        ImageView chrome = (ImageView) promptView.findViewById(R.id.Google_icon);
        chrome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = urls[pos];

                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                context.startActivity(i);

                alertD.dismiss();
            }
        });

        alertD.setView(promptView);

        alertD.show();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView sourceView, datesAgoView, titleView;
        ImageView imageView;
        ConstraintLayout constraintLayout;
        CardView cardView;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            sourceView = itemView.findViewById(R.id.news_source);
            datesAgoView = itemView.findViewById(R.id.news_dates_ago);
            titleView = itemView.findViewById(R.id.news_title);
            imageView = itemView.findViewById(R.id.news_image);
            constraintLayout = itemView.findViewById(R.id.news_container);
            cardView = itemView.findViewById((R.id.cardView));
        }
    }
}
