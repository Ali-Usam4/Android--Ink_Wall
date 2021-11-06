package com.example.inkwall;

import android.Manifest;
import android.app.WallpaperManager;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.inkwall.adapter.SwiperAdapter;
import com.example.inkwall.model.WallpaperModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static com.example.inkwall.FavouriteActivity.BOOKMARK_LIST;
import static com.example.inkwall.FavouriteActivity.BOOKMARK_PREF;

public class SwiperActivity extends AppCompatActivity {

    private ViewPager2 viewPager;

    //Positions to store images on Favorite image
    int position, currentPosition, matchedPosition;

    FloatingActionButton actionButton;
    private SwiperAdapter swiperAdapter;
    private List<WallpaperModel> list;
    private List<WallpaperModel> bookmarkList;
    DatabaseReference reference;
    SharedPreferences preferences;
    SharedPreferences.Editor editor;
    private String imageUrl, id;
    Gson gson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swiper);

        init();
        getImages();

        list = new ArrayList<>();
        swiperAdapter = new SwiperAdapter(this, list);
        viewPager.setAdapter(swiperAdapter);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    WallpaperModel model = dataSnapshot.getValue(WallpaperModel.class);
                    list.add(model);
                }
                swiperAdapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                viewPager.setCurrentItem(position, true);
            }
        }, 200);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                imageUrl = list.get(position).getImage();
                id = list.get(position).getId();

                currentPosition = position;

                if (imageMatched()) {
                    actionButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_favorite));
                } else {
                    actionButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_favorite_border));
                }
            }
        });
        clickListener();
    }

    private void clickListener() {
        // Implementing interface method of Swiper adapter

        swiperAdapter.OnDataPass(new SwiperAdapter.OnDataPass() {
            @Override
            public void onImageSave(int position, Bitmap bitmap) {
                Dexter.withContext(SwiperActivity.this)
                        .withPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                        .withListener(new MultiplePermissionsListener() {

                            @Override
                            public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                                if (multiplePermissionsReport.areAllPermissionsGranted()) {
                                    saveImage(bitmap);
                                } else {
                                    Toast.makeText(SwiperActivity.this, "Please Allow Permission", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                                permissionToken.continuePermissionRequest();
                            }
                        }).check();
            }

            @Override
            public void onImageSet(int position, Bitmap bitmap) {
                WallpaperManager manager = WallpaperManager.getInstance(getApplicationContext());
                try {
                    manager.setBitmap(bitmap);
                    Toast.makeText(SwiperActivity.this, "Wallpaper set Successfully!", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(SwiperActivity.this, "Failed to set Wallpaper", Toast.LENGTH_SHORT).show();
                }
            }
        });
        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (imageMatched()) {
                    bookmarkList.remove(matchedPosition);
                    actionButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_favorite_border));
                } else {
                    bookmarkList.add(list.get(currentPosition));
                    actionButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_favorite));
                }
            }
        });
    }

    private void saveImage(Bitmap bitmap) {
        String time = "WallpaperImage" + System.currentTimeMillis();
        String imageName = time + ".png";

        File path = Environment.getExternalStorageDirectory();
        File dir = new File(path + "/DCIM/Ink Wall");
        boolean bool = dir.mkdir();
        Log.d("Result", String.valueOf(bool));

        File file = new File(dir, imageName);

        OutputStream stream;
        try {
            stream = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(stream);

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);

            bos.flush();
            bos.close();
            Toast.makeText(this, "Image Saved!", Toast.LENGTH_SHORT).show();
            scanFile(path);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save Image", Toast.LENGTH_LONG).show();
        }
    }

    private void init() {
        viewPager = findViewById(R.id.viewPager);
        actionButton = findViewById(R.id.actionBtn);

        position = getIntent().getIntExtra("position", 0);
        reference = FirebaseDatabase.getInstance().getReference().child("Wallpapers");
        gson = new Gson();
        preferences = getSharedPreferences(BOOKMARK_PREF,MODE_PRIVATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        storeImage();
    }

    // For favorite
    private void storeImage(){
        String json = gson.toJson(bookmarkList);
        editor = preferences.edit();
        editor.putString(BOOKMARK_LIST,json);
        editor.apply();
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

    public boolean imageMatched() {
        int i = 0;
        boolean matched = false;
        for (WallpaperModel model : bookmarkList) {
            if (model.getId().equals(list.get(currentPosition).getId()) && model.getImage().equals(list.get(currentPosition).getImage())) {
                matched = true;
                matchedPosition = i;
            }
            i++;
        }
        return matched;
    }

    // To display image into gallery...................!
// did't work...
    private void scanFile(File path) {

        MediaScannerConnection.scanFile(this,
                new String[]{String.valueOf(path)}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.d("Tag", "Scan finished. You can view the image in the gallery now.");
                }
        });
    }
}