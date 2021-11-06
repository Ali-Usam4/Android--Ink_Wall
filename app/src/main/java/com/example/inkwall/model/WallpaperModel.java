package com.example.inkwall.model;

public class WallpaperModel{
    private String id, image;

    public WallpaperModel() {
    }

    public WallpaperModel(String id, String image) {
        this.id = id;
        this.image = image;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
