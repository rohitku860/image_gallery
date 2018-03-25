package com.example.android.galleryassignment.api;

import com.example.android.galleryassignment.model.ItemResponse;

import retrofit2.Call;
import retrofit2.http.GET;


public interface Service {
    @GET("/tutorial/jsonparsetutorial.txt")
    Call<ItemResponse> getItems();
}
