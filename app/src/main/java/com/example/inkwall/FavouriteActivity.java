package com.example.inkwall;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inkwall.adapter.FavouriteAdapter;
import com.example.inkwall.model.WallpaperModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class FavouriteActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    List<WallpaperModel> bookmarkList;
    SharedPreferences preferences;
    SharedPreferences.Editor editor;
    Gson gson;
    FavouriteAdapter adapter;

    public static final String BOOKMARK_PREF = "bookmarkPref";
    public static final String BOOKMARK_LIST = "bookmarkList";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);
        init();
        getImages();

        GridLayoutManager layoutManager = new GridLayoutManager(this,3);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new FavouriteAdapter(bookmarkList,this);
        recyclerView.setAdapter(adapter);

        onDataHandle();
    }

    private void onDataHandle(){
        adapter.OnImageRemoved(new FavouriteAdapter.OnImageRemoved() {
            @Override
            public void onImageRemoved(int position) {
                bookmarkList.remove(position);

                adapter.notifyDataSetChanged();
            }
        });
    }

    private void getImages() {
        String json = preferences.getString(BOOKMARK_LIST,"");
        Type type = new TypeToken<List<WallpaperModel>>(){
        }.getType();

            bookmarkList = gson.fromJson(json,type);

            if (bookmarkList == null){
                bookmarkList = new ArrayList<>();
            }
    }

    private void storeImage(){
        String json = gson.toJson(bookmarkList);
        editor = preferences.edit();
        editor.putString(BOOKMARK_LIST,json);
        editor.apply();
    }

    @Override
    protected void onPause() {
        super.onPause();
        storeImage();
    }

    private void init() {
        recyclerView = findViewById(R.id.recyclerView);
        preferences = getSharedPreferences(BOOKMARK_PREF,MODE_PRIVATE);
                gson = new Gson();
    }
}