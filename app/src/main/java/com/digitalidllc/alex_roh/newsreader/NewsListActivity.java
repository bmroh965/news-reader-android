package com.digitalidllc.alex_roh.newsreader;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.Toast;

import com.digitalidllc.alex_roh.newsreader.common.Constants;
import com.digitalidllc.alex_roh.newsreader.networking.HackerNewsApi;
import com.digitalidllc.alex_roh.newsreader.networking.news.NewsResponseSchema;
import com.digitalidllc.alex_roh.newsreader.networking.news.NewsSchema;
import com.digitalidllc.alex_roh.newsreader.screens.newslist.NewsListViewMvcImpl;

import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NewsListActivity extends AppCompatActivity implements NewsListViewMvcImpl.Listener {
    private HackerNewsApi mHackerNewsApi;

    private String[] mUpdatedNewsNum = new String[Constants.MAX_NEWS_NUM];

    private NewsListViewMvcImpl mViewMvc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewMvc = new NewsListViewMvcImpl(LayoutInflater.from(this), null);
        mViewMvc.registerListener(this);

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient ok=new OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .build();
            mHackerNewsApi = new Retrofit.Builder()
                    .baseUrl(Constants.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(ok.newBuilder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).build())
                    .build()
                    .create(HackerNewsApi.class);

        setContentView(mViewMvc.getRootView());
    }

    @Override
    protected void onStart() {
        super.onStart();
        fetchNews();
    }

    private void fetchNews(){
        //fetch the top stories
         mHackerNewsApi.getTopStories().enqueue(new retrofit2.Callback<List<Integer>>() {
             //succeded to fetch top news
             @Override
             public void onResponse(Call<List<Integer>> call, Response<List<Integer>> response) {
                 List<Integer> topStories = response.body();
                 //load each article and add it to mNewsList
                 for(int i=0; i<Constants.MAX_NEWS_NUM; i++){
                     mHackerNewsApi.getArticle(topStories.get(i)).enqueue(new retrofit2.Callback<NewsResponseSchema>() {
                         //succeded to fetch an article
                         @Override
                         public void onResponse(Call<NewsResponseSchema> call, Response<NewsResponseSchema> response) {
                             if(response.isSuccessful()) {
                                 //decouple server-side implementation from application impl
                                 NewsSchema newsSchema = new NewsSchema(response.body().getTitle(),response.body().getUrl());
                                 bindNews(newsSchema);
                             }
                         }

                         //failed to fetch an article
                         @Override
                         public void onFailure(Call<NewsResponseSchema> call, Throwable t) {
                                Toast.makeText(NewsListActivity.this, "Failed to load article", Toast.LENGTH_SHORT).show();
                         }
                     });
                 }
             }

             //failed to fetch top news
             @Override
             public void onFailure(Call<List<Integer>> call, Throwable t) {
                 Toast.makeText(NewsListActivity.this, "Failed to fetch articles", Toast.LENGTH_SHORT).show();
             }
         });{

        }
    }

    //decouple server-side implementation details
    private void bindNews(@NonNull NewsSchema newsSchema){
            String title= newsSchema.getTitle();
            String url = newsSchema.getUrl();
            mViewMvc.bindNews(title, url);
    }

    @Override
    public void onNewsClicked(News news){
        //open up article in Webview
        Intent intent = new Intent(getApplicationContext(), NewsViewerActivity.class);
        intent.putExtra("url",news.getURL());

        startActivity(intent);
    }
}
