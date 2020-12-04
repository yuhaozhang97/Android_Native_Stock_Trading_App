package com.example.stockapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    Toolbar mToolbar;
    SharedPreferences sharedPreferencesFavorite;
    SharedPreferences sharedPreferencesPortfolio;

    HashMap<String, Float> portfolioMap = new HashMap<String, Float>();
    ArrayList<Pair<String, Float>> portfolioList = new ArrayList<Pair<String, Float>>();
    Float[] portfolioCurPricesList;
    Float[] portfolioPriceChangesList;

    ArrayList<Pair<String, String>> favoriteList = new ArrayList<Pair<String, String>>();
    Float[] favoriteCurPricesList;
    Float[] favoritePriceChangesList;
    int progressCounter;
    int totalRequest;
    String[] optionsArray = new String[0];

    FavoriteAdapter favoriteAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Clear all local storage
//        sharedPreferencesPortfolio = getSharedPreferences("portfolio", Context.MODE_PRIVATE);
//        sharedPreferencesFavorite = getSharedPreferences("favorite", Context.MODE_PRIVATE);
//
//        sharedPreferencesPortfolio.edit().clear().apply();
//        sharedPreferencesFavorite.edit().clear().apply();

        // Set header toolbar
        mToolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(mToolbar);

        // Set today's date
        LocalDate date = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
        TextView dateView = (TextView) findViewById(R.id.main_date);
        dateView.setText(date.format(formatter));

        // Set footer link
        TextView footerView = (TextView) findViewById(R.id.main_footer);
        footerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(getString(R.string.tiingo_url)));
                startActivity(i);
            }
        });

        renderList();

        // Call backend every 15 seconds
        final Handler ha = new Handler();
        ha.postDelayed(new Runnable() {

            @Override
            public void run() {
                //call function
                Log.d("helper", "Fetching stock info and prices in home page.");
                ha.postDelayed(this, 15000);
            }
        }, 15000);

//        final Handler ha = new Handler();
//        ha.postDelayed(new Runnable() {
//
//            @Override
//            public void run() {
//                //call function
//                renderList();
//                ha.postDelayed(this, 15000);
//            }
//        }, 15000);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_menu, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.app_bar_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));


        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                Intent intent = new Intent(getApplicationContext(), OnSearchActivity.class);
                intent.putExtra("query", s);
                startActivity(intent);
//                finish();

                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                 return false;
            }
        });

        return true;
    }

    private void fetchAutocomplete(String query) {
        final RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        final String url = "http://nodeapp-env.eba-kgbwbpmw.us-east-1.elasticbeanstalk.com//api/options/" + query;

        JsonArrayRequest optionsRequest = new JsonArrayRequest(
                url,
                response -> {
                    try {
                        String[] tmpResults = new String[response.length()];
                        for (int i = 0; i < response.length(); i++) {
                            tmpResults[i] = response.getString(i);
                        }

                        optionsArray = tmpResults;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    Log.d("errorRequestInfo", error.toString());
                }
        );

        optionsRequest.setRetryPolicy(new DefaultRetryPolicy(
                1000*5,
                /*DefaultRetryPolicy.DEFAULT_MAX_RETRIES*/ 3,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        queue.add(optionsRequest);
    }

    private void renderList() {
        // Check portfolio
        sharedPreferencesPortfolio = getSharedPreferences("portfolio", Context.MODE_PRIVATE);
        if (sharedPreferencesPortfolio.getFloat("cash", -1f) < 0) {
            SharedPreferences.Editor portfolioEditor = sharedPreferencesPortfolio.edit();
            portfolioEditor.putFloat("cash", 20000f).apply();
        }

        Map<String, ?> portfolioAllEntries = sharedPreferencesPortfolio.getAll();
        for (Map.Entry<String, ?> entry : portfolioAllEntries.entrySet()) {
            if (!entry.getKey().equals("cash") && Float.parseFloat(entry.getValue().toString()) > 0f) {
                portfolioMap.put(entry.getKey(), Float.parseFloat(entry.getValue().toString()));
                portfolioList.add(new Pair(entry.getKey(), Float.parseFloat(entry.getValue().toString())));
            }
        }
        portfolioCurPricesList = new Float[portfolioList.size()];
        portfolioPriceChangesList = new Float[portfolioList.size()];

        // Check favorite
        sharedPreferencesFavorite = getSharedPreferences("favorite", Context.MODE_PRIVATE);
        Map<String, ?> favoriteAllEntries = sharedPreferencesFavorite.getAll();
        for (Map.Entry<String, ?> entry : favoriteAllEntries.entrySet()) {
            if (!entry.getValue().toString().equals("false")) {
                favoriteList.add(new Pair(entry.getKey(), entry.getValue().toString()));
            }
        }
        favoriteCurPricesList = new Float[favoriteList.size()];
        favoritePriceChangesList = new Float[favoriteList.size()];

        totalRequest = portfolioList.size() + favoriteList.size();

        TextView main_dummy_1 = (TextView) findViewById(R.id.main_dummy_text_1);
        TextView main_dummy_2 = (TextView) findViewById(R.id.main_dummy_text_2);
        TextView netWorth = (TextView) findViewById(R.id.main_net_worth_value);
        RecyclerView portRecView = (RecyclerView) findViewById(R.id.portfolio_recycler);

        if (portfolioList.size() == 0) {
            // Set invisible Portfolio
            main_dummy_1.setVisibility(View.GONE);
            main_dummy_2.setVisibility(View.GONE);
            netWorth.setVisibility(View.GONE);
            portRecView.setVisibility(View.GONE);
        } else {
            // Backend request on Portfolio
            main_dummy_1.setVisibility(View.VISIBLE);
            main_dummy_2.setVisibility(View.VISIBLE);
            netWorth.setVisibility(View.VISIBLE);
            portRecView.setVisibility(View.VISIBLE);

            retrievePortfolioPrices(portfolioList);
        }

        TextView main_dummy_3 = (TextView) findViewById(R.id.main_dummy_text_3);
        RecyclerView favRecView = (RecyclerView) findViewById((R.id.favorite_recycler));

        if (favoriteList.size() == 0) {
            // Set invisible Favorite
            main_dummy_3.setVisibility(View.GONE);
            favRecView.setVisibility(View.GONE);
        } else {
            // Backend request on Favorite
            main_dummy_3.setVisibility(View.VISIBLE);
            favRecView.setVisibility(View.VISIBLE);

            retrieveFavoritePrices(favoriteList);
        }

        // Remove progress bar
        if (totalRequest == 0) {
            ProgressBar bar = (ProgressBar) findViewById(R.id.main_pBar);
            bar.setVisibility(View.GONE);
            TextView barText = (TextView) findViewById(R.id.main_progress_text);
            barText.setVisibility(View.GONE);
            NestedScrollView scrollView = (NestedScrollView) findViewById(R.id.main_scroll_view);
            scrollView.setVisibility(View.VISIBLE);
        }

        Log.d("helper", "Fetching stock info and prices in home page.");
    }

    // Fetch latest prices from API for Portfolio
    public void retrievePortfolioPrices(ArrayList<Pair<String, Float>> tickerList) {
        final RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        final String urlPricesPrefix = "http://nodeapp-env.eba-kgbwbpmw.us-east-1.elasticbeanstalk.com/api/prices/";

        for (int i = 0; i < tickerList.size(); i++) {
            int finalI = i;
            Log.d("ticker", "portfolio: " + tickerList.get(finalI).getL());
            JsonArrayRequest pricesRequest = new JsonArrayRequest(
                    urlPricesPrefix + tickerList.get(finalI).getL(),
                    response -> {
                        try {
                            final JSONObject prices = response.getJSONObject(0);

                            // Set ticker cur price and change
                            portfolioCurPricesList[finalI] = Float.parseFloat(prices.getString("last"));

                            portfolioPriceChangesList[finalI] = Float.parseFloat(prices.getString("last")) - Float.parseFloat(prices.getString("prevClose"));

                            incrementRequest();
                        } catch (Exception e) {
                            Log.d("exception", "in portfolio retrieve");
                            e.printStackTrace();
                        }
                    },
                    error -> {
                        Log.d("errorRequestInfo", error.getMessage());
                    }
            );

            pricesRequest.setRetryPolicy(new DefaultRetryPolicy(
                    1000*5,
                    /*DefaultRetryPolicy.DEFAULT_MAX_RETRIES*/ 3,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            queue.add(pricesRequest);
        }
    }

    // Fetch latest prices from API for Favorite
    public void retrieveFavoritePrices(ArrayList<Pair<String, String>> tickerList) {
        final RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        final String urlPricesPrefix = "http://nodeapp-env.eba-kgbwbpmw.us-east-1.elasticbeanstalk.com/api/prices/";

        for (int i = 0; i < tickerList.size(); i++) {
            int finalI = i;
            Log.d("ticker", "favorite: " + tickerList.get(finalI).getL());
            JsonArrayRequest pricesRequest = new JsonArrayRequest(
                    urlPricesPrefix + tickerList.get(finalI).getL(),
                    response -> {
                        try {
                            final JSONObject prices = response.getJSONObject(0);

                            // Set ticker cur price and change
                            favoriteCurPricesList[finalI] = Float.parseFloat(prices.getString("last"));

                            favoritePriceChangesList[finalI] = Float.parseFloat(prices.getString("last")) - Float.parseFloat(prices.getString("prevClose"));

                            incrementRequest();
                        } catch (Exception e) {
                            Log.d("exception", "in favorite retrieve");
                            e.printStackTrace();
                        }
                    },
                    error -> {
                        Log.d("errorRequestInfo", error.getMessage());
                    }
            );

            pricesRequest.setRetryPolicy(new DefaultRetryPolicy(
                    1000*5,
                    /*DefaultRetryPolicy.DEFAULT_MAX_RETRIES*/ 3,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            queue.add(pricesRequest);
        }
    }

    public void incrementRequest() {
        progressCounter++;
        if (progressCounter == totalRequest) {
            final DecimalFormat df = new DecimalFormat("##0.00");

            // Set net worth
            sharedPreferencesPortfolio = getSharedPreferences("portfolio", Context.MODE_PRIVATE);
            final TextView netWorthValueView = (TextView) findViewById(R.id.main_net_worth_value);
            float netVal = sharedPreferencesPortfolio.getFloat("cash", 20000f);

            for (int i = 0; i < portfolioList.size(); i++) {
                float curStockVal = portfolioList.get(i).getR() * portfolioCurPricesList[i];
                netVal += curStockVal;
            }

            netWorthValueView.setText(df.format(netVal));


            // Set Portfolio recyclerView using Portfolio Adapter
            RecyclerView portfolioRecyclerView = findViewById(R.id.portfolio_recycler);
            PortfolioAdapter portfolioAdapter = new PortfolioAdapter(this, portfolioList, portfolioCurPricesList, portfolioPriceChangesList);
            portfolioRecyclerView.setAdapter(portfolioAdapter);
            portfolioRecyclerView.setLayoutManager(new LinearLayoutManager(this));

            // Attach drag and drop helper to Portfolio recycler view
            ItemTouchHelper itemTouchHelperPort = new ItemTouchHelper(simplePortfolioCallback);
            itemTouchHelperPort.attachToRecyclerView(portfolioRecyclerView);


            // Set Favorite recyclerView using Favorite Adapter
            RecyclerView favoriteRecyclerView = findViewById(R.id.favorite_recycler);
            favoriteAdapter = new FavoriteAdapter(this, favoriteList, favoriteCurPricesList, favoritePriceChangesList, portfolioMap);
            favoriteRecyclerView.setAdapter(favoriteAdapter);
            favoriteRecyclerView.setLayoutManager(new LinearLayoutManager(this));

            // Attach drag and drop helper to Favorite recycler view
            ItemTouchHelper itemTouchHelperFav = new ItemTouchHelper(simpleFavoriteCallback);
            itemTouchHelperFav.attachToRecyclerView(favoriteRecyclerView);

            // Remove progress bar
            ProgressBar bar = (ProgressBar) findViewById(R.id.main_pBar);
            bar.setVisibility(View.GONE);
            TextView barText = (TextView) findViewById(R.id.main_progress_text);
            barText.setVisibility(View.GONE);
            NestedScrollView scrollView = (NestedScrollView) findViewById(R.id.main_scroll_view);
            scrollView.setVisibility(View.VISIBLE);
        }
    }

    ItemTouchHelper.SimpleCallback simplePortfolioCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.START | ItemTouchHelper.END, 0) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            int fromPosition = viewHolder.getAdapterPosition();
            int toPosition = target.getAdapterPosition();

            Collections.swap(portfolioList, fromPosition, toPosition);
            swapArray(portfolioCurPricesList, fromPosition, toPosition);
            swapArray(portfolioPriceChangesList, fromPosition, toPosition);

            recyclerView.getAdapter().notifyItemMoved(fromPosition, toPosition);

            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}
    };

    ItemTouchHelper.SimpleCallback simpleFavoriteCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.START | ItemTouchHelper.END, ItemTouchHelper.LEFT) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            int fromPosition = viewHolder.getAdapterPosition();
            int toPosition = target.getAdapterPosition();

            Collections.swap(favoriteList, fromPosition, toPosition);
            swapArray(favoriteCurPricesList, fromPosition, toPosition);
            swapArray(favoritePriceChangesList, fromPosition, toPosition);

            recyclerView.getAdapter().notifyItemMoved(fromPosition, toPosition);

            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();

            String removedTicker = favoriteList.get(position).getL();
            // Remove from adapter
            favoriteList.remove(position);
            favoriteCurPricesList = removeElement(favoriteCurPricesList, position);
            favoritePriceChangesList = removeElement(favoritePriceChangesList, position);
            // Remove from local storage
            sharedPreferencesFavorite = getSharedPreferences("favorite", Context.MODE_PRIVATE);
            sharedPreferencesFavorite.edit().putString(removedTicker, "false").apply();

            favoriteAdapter.notifyItemRemoved(position);

            if (favoriteList.size() == 0) {
                TextView favorite_dummy = (TextView) findViewById(R.id.main_dummy_text_3);
                favorite_dummy.setVisibility(View.GONE);
            }
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    .addSwipeLeftBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.red))
                    .addSwipeLeftActionIcon(R.drawable.ic_baseline_delete_24)
                    .create()
                    .decorate();

            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    };

    public void swapArray(Float arr[], int from, int to) {
        Float tmp = arr[from];
        arr[from] = arr[to];
        arr[to] = tmp;
    }

    public Float[] removeElement(Float[] arr, int index) {
        if (arr == null || index < 0 || index >= arr.length) {
            return arr;
        }

        // Create another array of size one less
        Float[] anotherArray = new Float[arr.length - 1];

        // Copy the elements from starting till index
        // from original array to the other array
        System.arraycopy(arr, 0, anotherArray, 0, index);

        // Copy the elements from index + 1 till end
        // from original array to the other array
        System.arraycopy(arr, index + 1,
                anotherArray, index,
                arr.length - index - 1);

        // return the resultant array
        return anotherArray;
    }
}