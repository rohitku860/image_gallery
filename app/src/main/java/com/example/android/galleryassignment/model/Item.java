package com.example.android.galleryassignment.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by RohitKrUp on 18/03/2018.
 */

public class Item {
    @SerializedName("flag")
    @Expose
    private String flag;

    public Item(String flag) {
        this.flag = flag;
    }

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }
}
