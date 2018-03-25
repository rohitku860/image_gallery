package com.example.android.galleryassignment.controller;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.galleryassignment.ItemAdapter;
import com.example.android.galleryassignment.R;
import com.example.android.galleryassignment.api.Client;
import com.example.android.galleryassignment.api.Service;
import com.example.android.galleryassignment.model.Item;
import com.example.android.galleryassignment.model.ItemResponse;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    TextView Disconnected;
    ProgressDialog pd;
    private SwipeRefreshLayout swipeContainer;
    String FILENAME = "Contact.csv";
    String entry = "";
    CoordinatorLayout coordinatorLayout;
    FileOutputStream fileOutputStream;
    FileInputStream fileInputStream;
    File fileLoc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        coordinatorLayout = findViewById(R.id.main_layout);
        fileLoc = this.getFilesDir();
        Log.v("DONKEY", this.getFilesDir().getAbsolutePath());
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            startAsyncTask("Hello");
            Snackbar.make(coordinatorLayout,"Start creating Zip File ",Snackbar.LENGTH_SHORT).show();

        });

        swipeContainer = findViewById(R.id.swipeContainer);
        swipeContainer.setColorSchemeResources(android.R.color.holo_orange_dark);
        swipeContainer.setOnRefreshListener(() -> {
            loadJSON();
            Toast.makeText(MainActivity.this, "User Refreshed", Toast.LENGTH_SHORT).show();
        });
    }


    private void initViews() {
        pd = new ProgressDialog(this);
        pd.setMessage("Fetching images");
        pd.setCancelable(false);
        pd.show();
        recyclerView = findViewById(R.id.recyclerView);
        StaggeredGridLayoutManager staggeredGridLayoutManager = new StaggeredGridLayoutManager
                (3, LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(staggeredGridLayoutManager);
        recyclerView.smoothScrollToPosition(0);
        loadJSON();
    }

    private void loadJSON() {
        Disconnected = findViewById(R.id.disconnected);
        try {
         //   Client client = new Client();
            Service apiService = Client.getClient().create(Service.class);
            Call<ItemResponse> call = apiService.getItems();
            call.enqueue(new Callback<ItemResponse>() {
                @Override
                public void onResponse(Call<ItemResponse> call, Response<ItemResponse> response) {
                    List<Item> items = response.body().getItems();
                    recyclerView.setAdapter(new ItemAdapter(getApplicationContext(), items));
                    recyclerView.smoothScrollToPosition(0);
                    swipeContainer.setRefreshing(false);
                    pd.hide();
                }

                @Override
                public void onFailure(Call<ItemResponse> call, Throwable t) {
                    Log.d("Error", t.getMessage());
                    Toast.makeText(MainActivity.this, "Error Fetching Data", Toast.LENGTH_SHORT).show();
                    Disconnected.setVisibility(View.VISIBLE);
                    pd.hide();
                }
            });
        } catch (Exception e) {
            Log.d("Error", e.getMessage());
            Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private void getContactList() {
        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);
        try {
            fileOutputStream = openFileOutput(FILENAME, Context.MODE_APPEND);
            fileInputStream = openFileInput(FILENAME);

        } catch (FileNotFoundException e) {
            Log.v("DONKEY", e.getMessage());
        }

        if ((cur != null ? cur.getCount() : 0) > 0) {
            while (cur != null && cur.moveToNext()) {
                String id = cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(
                        ContactsContract.Contacts.DISPLAY_NAME));

                if (cur.getInt(cur.getColumnIndex(
                        ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    Cursor pCur = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id}, null);
                    if (pCur != null){
                    while (pCur.moveToNext()) {
                        String phoneNo = pCur.getString(pCur.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER));
                        try {
                            entry = id + "," + name + "," + phoneNo;
                            fileOutputStream.write(entry.getBytes());
                        } catch (Exception e) {
                            Log.v("DONKEY", e.getMessage());
                        }
                    }
                    pCur.close();
                    }
                }
            }

        }
        if (cur != null) cur.close();
        try {
            fileOutputStream.flush();
            fileOutputStream.close();
            zip(FILENAME, "Contacts.zip");
        } catch (IOException e) {
            Log.v("DONKEY", "exception in calling zip " + e.getMessage());
        }
    }

    public void zip(String files, String zipFile) throws IOException {
        BufferedInputStream origin;// = null;
        try (ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(getFilesDir() + zipFile)))) {
            byte data[] = new byte[1024];
            origin = new BufferedInputStream(fileInputStream, 1024);

            try {
                ZipEntry entry = new ZipEntry(files.substring(files.lastIndexOf("/") + 1));
                out.putNextEntry(entry);
                int count;
                int Counting = 0;
                while ((count = origin.read(data, 0, 1024)) != -1) {
                    out.write(data, 0, count);
                    Counting++;
                    Log.v("DONKEY", "write perform" + Counting + "  " + origin.read(data, 0, 1024));

                }
            } catch (Exception e) {
                Log.v("DONKEY", "exception in zip entry" + e.getMessage());

            } finally {
                origin.close();
            }

        }
    }

    private void startAsyncTask(String input) {
        Observable.just(input)
                .map(this::doInBackground)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onPostExecute);
    }


    //------------- full mapping to async task functions -----------------//
    private void onPostExecute(int result) {
        Snackbar.make(coordinatorLayout,"Zip File Is saved at"+getFilesDir()+"/Contacts.zip",Snackbar.LENGTH_LONG).show();

    }
    //do background things here
    private int doInBackground(String data) {
        getContactList();
        return data.length();
    }
}