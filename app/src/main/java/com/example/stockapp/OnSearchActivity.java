package com.example.stockapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import org.json.JSONObject;

import java.sql.Date;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.isNull;

public class OnSearchActivity extends AppCompatActivity {

    SharedPreferences sharedPreferencesFavorite;
    SharedPreferences sharedPreferencesPortfolio;
    String ticker;
    String stockName;
    boolean favorite;
    int progressCounter;
    double curPrice;
    float curShare;
    float inputShare = 0f;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ticker = getIntent().getStringExtra(SearchManager.QUERY).toUpperCase();

        setContentView(R.layout.activity_on_search);

        // Set header toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);

        // add back arrow to toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Fetch from backend
        handleIntent(getIntent());

        // Fetch chart from HTML
        WebView webView = (WebView) findViewById(R.id.web_view);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("file:///android_asset/get_chart.html?ticker=" + ticker);

        // Set trade button
        Button tradeButton = (Button) findViewById(R.id.detail_portfolio_button);
        tradeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openDialog();
;            }
        });
    }

    // Open trade dialog
    public void openDialog() {
        DecimalFormat df = new DecimalFormat("##0.00");

        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View promptView = layoutInflater.inflate(R.layout.trade_dialog, null);
        final AlertDialog alertD = new AlertDialog.Builder(this).create();

        float cash = sharedPreferencesPortfolio.getFloat("cash", -1);
        sharedPreferencesPortfolio = getSharedPreferences("portfolio", Context.MODE_PRIVATE);
        SharedPreferences.Editor portfolioEditor = sharedPreferencesPortfolio.edit();

        // Prepare to set views
        EditText userInput = (EditText) promptView.findViewById(R.id.trade_dialog_input);
        Button buyButton = (Button) promptView.findViewById(R.id.trade_dialog_buy_button);
        Button sellButton = (Button) promptView.findViewById(R.id.trade_dialog_sell_button);
        TextView available = (TextView) promptView.findViewById(R.id.trade_dialog_available);
        TextView title = (TextView) promptView.findViewById(R.id.trade_dialog_title);
        TextView calculate = (TextView) promptView.findViewById(R.id.trade_dialog_calculate);

        title.setText("Trade " + stockName + " shares");
        available.setText("$" + df.format(cash) + " available to buy " + ticker);
        calculate.setText("0 x $" + curPrice + "/share = $" + df.format(inputShare * curPrice));

        // set EditText listener
        userInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.toString().matches("")) {
                    inputShare = 0f;
                    calculate.setText("0 x $" + curPrice + "/share = $" + df.format(inputShare * curPrice));
                } else if (!editable.toString().matches("[0-9]+")) {
                    inputShare = 0f;
                    calculate.setText("0 x $" + curPrice + "/share = $" + df.format(inputShare * curPrice));
                    Toast.makeText(getApplicationContext(), "Please enter valid amount.", Toast.LENGTH_SHORT).show();
                } else {
                    inputShare = Float.parseFloat(editable.toString());
                    calculate.setText(inputShare + " x $" + curPrice + "/share = $" + df.format(inputShare * curPrice));
                }
            }
        });

        // Set buttons listener
        buyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (curPrice * inputShare > cash) {
                    Toast.makeText(getApplicationContext(), "Not enough money to buy.", Toast.LENGTH_SHORT).show();
                } else if (inputShare <= 0f) {
                    Toast.makeText(getApplicationContext(), "Cannot buy less than 0 shares.", Toast.LENGTH_SHORT).show();
                } else {
                    // Set new cash and share values
                    portfolioEditor.putFloat("cash", cash - (float) curPrice * inputShare);
                    portfolioEditor.putFloat(ticker, inputShare + curShare);
                    portfolioEditor.apply();

                    openCongratsDialog("bought");
                    alertD.dismiss();

                    setPortfolio();
                }
            }
        });

        sellButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (inputShare > curShare) {
                    Toast.makeText(getApplicationContext(), "Not enough shares to sell.", Toast.LENGTH_SHORT).show();
                } else if (inputShare <= 0f) {
                    Toast.makeText(getApplicationContext(), "Cannot sell less than 0 shares.", Toast.LENGTH_SHORT).show();
                } else {
                    // Set new cash and share values
                    portfolioEditor.putFloat("cash", cash + (float) curPrice * inputShare);
                    portfolioEditor.putFloat(ticker, curShare - inputShare);
                    portfolioEditor.apply();

                    openCongratsDialog("sold");
                    alertD.dismiss();

                    setPortfolio();
                }
            }
        });

        alertD.setView(promptView);

        alertD.show();
    }

    private void openCongratsDialog(String action) {
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View promptView = layoutInflater.inflate(R.layout.congrats_dialog, null);
        final AlertDialog alertD = new AlertDialog.Builder(this).create();

        // Set subtitle
        TextView subText = (TextView) promptView.findViewById(R.id.congrats_sub_text);
        subText.setText("You have successfully " + action + " " + inputShare + "\nshares of " + ticker);

        // Set dismiss button
        Button button = (Button) promptView.findViewById(R.id.congrats_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertD.dismiss();
            }
        });

        alertD.setView(promptView);

        alertD.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.search_menu, menu);

        // Check if favorite
        sharedPreferencesFavorite = getSharedPreferences("favorite", Context.MODE_PRIVATE);
        if (!sharedPreferencesFavorite.getString(ticker, "false").equals("false")) {
            favorite = true;
            menu.getItem(0).setIcon(R.drawable.ic_baseline_star_24);
        } else {
            favorite = false;
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
//            onBackPressed();
            finish();
        } else if (item.getItemId() == R.id.star) {
            // Toggle star
            sharedPreferencesFavorite = getSharedPreferences("favorite", Context.MODE_PRIVATE);
            SharedPreferences.Editor favoriteEditor = sharedPreferencesFavorite.edit();

            Log.d("fav", "In toggle, ticker is " + ticker);
            if (favorite) {
                favoriteEditor.putString(ticker, "false").apply();
                item.setIcon(R.drawable.ic_baseline_star_border_24);
                Toast.makeText(getApplicationContext(), "\"" + ticker + "\"" + " was removed from favorites", Toast.LENGTH_SHORT).show();
            } else {
                favoriteEditor.putString(ticker, stockName).apply();
                item.setIcon(R.drawable.ic_baseline_star_24);
                Toast.makeText(getApplicationContext(), "\"" + ticker + "\"" + " was added to favorites", Toast.LENGTH_SHORT).show();
            }

            favorite = !favorite;

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    public static void makeTextViewResizable(final TextView tv, final int maxLine, final String expandText, final boolean viewMore) {

        if (tv.getTag() == null) {
            tv.setTag(tv.getText());
        }
        ViewTreeObserver vto = tv.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @SuppressWarnings("deprecation")
            @Override
            public void onGlobalLayout() {
                String text;
                int lineEndIndex;
                ViewTreeObserver obs = tv.getViewTreeObserver();
                obs.removeGlobalOnLayoutListener(this);
                if (maxLine == 0) {
                    lineEndIndex = tv.getLayout().getLineEnd(0);
                    text = tv.getText().subSequence(0, lineEndIndex - expandText.length() + 1) + " " + expandText;
                } else if (maxLine > 0 && tv.getLineCount() >= maxLine) {
                    lineEndIndex = tv.getLayout().getLineEnd(maxLine - 1);
                    text = tv.getText().subSequence(0, lineEndIndex - expandText.length() + 1) + " " + expandText;
                } else {
                    lineEndIndex = tv.getLayout().getLineEnd(tv.getLayout().getLineCount() - 1);
                    text = tv.getText().subSequence(0, lineEndIndex) + " " + expandText;
                }
                tv.setText(text);
                tv.setMovementMethod(LinkMovementMethod.getInstance());
                tv.setText(
                        addClickablePartTextViewResizable(Html.fromHtml(tv.getText().toString()), tv, lineEndIndex, expandText,
                                viewMore), TextView.BufferType.SPANNABLE);
            }
        });
    }

    private static SpannableStringBuilder addClickablePartTextViewResizable(final Spanned strSpanned, final TextView tv,
                                                                            final int maxLine, final String spannableText, final boolean viewMore) {
        String str = strSpanned.toString();
        SpannableStringBuilder ssb = new SpannableStringBuilder(strSpanned);

        if (str.contains(spannableText)) {
            ssb.setSpan(new MySpannable(false){
                @Override
                public void onClick(View widget) {
                    tv.setHighlightColor(Color.TRANSPARENT);
                    tv.setLayoutParams(tv.getLayoutParams());
                    tv.setText(tv.getTag().toString(), TextView.BufferType.SPANNABLE);
                    tv.invalidate();
                    if (viewMore) {
                        makeTextViewResizable(tv, -1, "Show Less", false);
                    } else {
                        makeTextViewResizable(tv, 2, "Show More...", true);
                    }
                }
            }, str.indexOf(spannableText), str.indexOf(spannableText) + spannableText.length(), 0);

        }
        return ssb;
    }

    private void setPortfolio() {
        // Set Portfolio TODO: fetch from localStorage first
        DecimalFormat df = new DecimalFormat("##0.00");
        final TextView portfolio_info = (TextView) findViewById(R.id.detail_portfolio_info);
        sharedPreferencesPortfolio = getSharedPreferences("portfolio", Context.MODE_PRIVATE);
        float prevShare = sharedPreferencesPortfolio.getFloat(ticker, 0f);
        curShare = prevShare;

        if (prevShare > 0) {
            portfolio_info.setText("Shares owned: " + prevShare + "\nMarket Value: $" + df.format(prevShare * curPrice));
        } else {
            portfolio_info.setText("You have 0 share of " + ticker + ".\nStart trading!");
        }
    }

    private void incrementRequest() {
        progressCounter++;
        if (progressCounter == 3) {
            ProgressBar bar = (ProgressBar) findViewById(R.id.detail_pBar);
            bar.setVisibility(View.GONE);
            TextView barText = (TextView) findViewById(R.id.detail_progress_text);
            barText.setVisibility(View.GONE);
            NestedScrollView scrollView = (NestedScrollView) findViewById(R.id.detail_scroll_view);
            scrollView.setVisibility(View.VISIBLE);
        }
    }

    private void handleIntent(Intent intent) {
        final String query = intent.getStringExtra(SearchManager.QUERY);

        final RequestQueue queue = Volley.newRequestQueue(OnSearchActivity.this);

        final String urlInfo = "http://nodeapp-env.eba-kgbwbpmw.us-east-1.elasticbeanstalk.com/api/info/" + query;
        final String urlPrices = "http://nodeapp-env.eba-kgbwbpmw.us-east-1.elasticbeanstalk.com/api/prices/" + query;
        final String urlNews = "http://nodeapp-env.eba-kgbwbpmw.us-east-1.elasticbeanstalk.com/api/news/" + query;
//        final String urlChart = "http://nodeapp-env.eba-kgbwbpmw.us-east-1.elasticbeanstalk.com/api/twoyearscharts/" + query + ;


        // description, ticker, name
        JsonObjectRequest infoRequest = new JsonObjectRequest(
                urlInfo,
                null,
                response -> {
                    try {
                        final TextView tickerView = (TextView) findViewById(R.id.detail_ticker);
                        tickerView.setText(response.getString("ticker"));

                        final TextView name = (TextView) findViewById(R.id.detail_name);
                        name.setText(response.getString("name"));
                        stockName = response.getString("name");


                        // Set About (About chunk)
                        final TextView about_desc = (TextView) findViewById(R.id.detail_about_desc);
                        about_desc.setText(response.getString("description"));
                        makeTextViewResizable(about_desc, 2, "Show More...", true);

                        // Count loading progress and change visibility
                        incrementRequest();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    Log.d("errorRequestInfo", error.getMessage());
                }
        );

        // prices
        // last, cur, low, mid, bid, high, open, prevClose, volume
        JsonArrayRequest pricesRequest = new JsonArrayRequest(
                urlPrices,
                response -> {
                    try {
                        final JSONObject prices = response.getJSONObject(0);

                        // Set ticker cur price and change (top chunk)
                        final TextView price = (TextView) findViewById(R.id.detail_price);
                        price.setText("$" + prices.getString("last"));
                        curPrice = Double.parseDouble(prices.getString("last"));

                        DecimalFormat df = new DecimalFormat("##0.00");
                        final TextView change = (TextView) findViewById(R.id.detail_change);
                        final double price_dif = Double.parseDouble(prices.getString("last")) - Double.parseDouble(prices.getString("prevClose"));

                        if (price_dif == 0) {
                            change.setText("$" + df.format(price_dif));
                        } else if (price_dif < 0) {
                            change.setText("-$" + df.format(Math.abs(price_dif)));
                            change.setTextColor(getResources().getColor(R.color.red));
                        } else {
                            change.setText("+$" + df.format(price_dif));
                            change.setTextColor(getResources().getColor(R.color.green));
                        }

                        // Set Stats (Stats chunk)
                        final TextView cur_price = (TextView) findViewById(R.id.stats_cur_price);
                        cur_price.setText("Current Price:\n" + prices.getString("last"));

                        final TextView low_price = (TextView) findViewById(R.id.stats_low);
                        low_price.setText("Low: " + prices.getString("low"));

                        final TextView bid_price = (TextView) findViewById(R.id.stats_bid_price);
                        if (isNull(prices.getString("bidPrice"))) {
                            bid_price.setText("Bid: " + prices.getString("bidPrice"));
                        } else {
                            bid_price.setText("Bid: 0.0");
                        }

                        final TextView open_price = (TextView) findViewById(R.id.stats_open_price);
                        open_price.setText("OpenPrice:\n" + prices.getString("open"));

                        final TextView mid_price = (TextView) findViewById(R.id.stats_mid);
                        if (isNull(prices.getString("mid"))) {
                            mid_price.setText("Mid: " + prices.getString("mid"));
                        } else {
                            mid_price.setText("Mid: 0.0");
                        }

                        final TextView high_price = (TextView) findViewById(R.id.stats_high);
                        high_price.setText("High: " + prices.getString("high"));

                        final TextView volume = (TextView) findViewById(R.id.stats_volume);
                        volume.setText("Volume:\n" + prices.getString("volume") + ".00");

                        // Set Portfolio
                        setPortfolio();

                        // Count loading progress and change visibility
                        incrementRequest();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    Log.d("errorRequestInfo", error.toString());
                }
        );

        // News
        // title, source.name, urlToImage, url, publishedAt
        JsonArrayRequest newsRequest = new JsonArrayRequest(
                urlNews,
                response -> {
                    try {
                        String sources[] = new String[response.length()];
                        String titles[] = new String[response.length()];
                        String urls[] = new String[response.length()];
                        RequestCreator imagesLoader[] = new RequestCreator[response.length()];
                        int datesAgo[] = new int[response.length()];

                        for (int i = 0; i < response.length(); i++) {
                          JSONObject news = response.getJSONObject(i);
                          sources[i] = news.getJSONObject("source").getString("name");
                          titles[i] = news.getString("title");
                          urls[i] = news.getString("url");

                          // Convert image
                          imagesLoader[i] = Picasso.with(this).load(news.getString("urlToImage"));

                          // Convert Date
                          Date published = Date.valueOf(news.getString("publishedAt").substring(0, 10));
                          Date now = new Date(Calendar.getInstance().getTime().getTime());
                          final long diffInMillies = Math.abs(now.getTime() - published.getTime());
                          final int dayDiff = (int) TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
                          datesAgo[i] = dayDiff;

                          // Set recyclerView using customed adapter
                          RecyclerView newsRecyclerView = findViewById(R.id.news_recycler_view);
                          NewsAdapter newsAdapter = new NewsAdapter(this, sources, titles, urls, datesAgo, imagesLoader);
                          newsRecyclerView.setAdapter(newsAdapter);
                          newsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                        }

                        // Count loading progress and change visibility
                        incrementRequest();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    Log.d("errorRequestInfo", error.toString());
                }
        );

        Log.d("helper", "Fetching stock info and prices in detail page.");

        // Requests retry policy
        infoRequest.setRetryPolicy(new DefaultRetryPolicy(
                1000*5,
                /*DefaultRetryPolicy.DEFAULT_MAX_RETRIES*/ 3,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        pricesRequest.setRetryPolicy(new DefaultRetryPolicy(
                1000*5,
                /*DefaultRetryPolicy.DEFAULT_MAX_RETRIES*/ 3,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        newsRequest.setRetryPolicy(new DefaultRetryPolicy(
                1000*5,
                /*DefaultRetryPolicy.DEFAULT_MAX_RETRIES*/ 3,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        queue.add(infoRequest);
        queue.add(pricesRequest);
        queue.add(newsRequest);

        // TODO: Two years chart
    }
}
